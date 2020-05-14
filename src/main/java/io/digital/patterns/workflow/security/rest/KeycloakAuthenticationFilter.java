package io.digital.patterns.workflow.security.rest;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.identity.Group;
import org.camunda.spin.Spin;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;

@Slf4j
public class KeycloakAuthenticationFilter implements Filter {
    private IdentityService identityService;

    public KeycloakAuthenticationFilter(IdentityService identityService) {
        this.identityService = identityService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        OAuth2AuthenticationDetails details = (OAuth2AuthenticationDetails) authentication.getDetails();
        String accessToken = details.getTokenValue();
        String claims = JwtHelper.decode(accessToken).getClaims();

        String userId = Spin.JSON(claims).prop("email").stringValue();
        log.debug("Extracted userId from bearer token: {}", userId);

        if (userId != null) {
            MDC.put("userId", userId);
        }
        try {
            identityService.setAuthentication(userId, getUserGroups(userId));
            chain.doFilter(request, response);
        } finally {
            identityService.clearAuthentication();
            MDC.remove("userId");
        }
    }

    private List<String> getUserGroups(String userId) {
        return identityService.createGroupQuery().groupMember(userId).list()
                .stream()
                .map(Group::getId).collect(Collectors.toList());
    }
}
