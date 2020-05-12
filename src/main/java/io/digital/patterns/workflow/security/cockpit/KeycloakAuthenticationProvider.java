package io.digital.patterns.workflow.security.cockpit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.rest.security.auth.AuthenticationResult;
import org.camunda.bpm.engine.rest.security.auth.impl.ContainerBasedAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.util.StringUtils;

public class KeycloakAuthenticationProvider extends ContainerBasedAuthenticationProvider {

    @Override
    public AuthenticationResult extractAuthenticatedUser(HttpServletRequest request, ProcessEngine engine) {

        OAuth2Authentication authentication = (OAuth2Authentication) SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return AuthenticationResult.unsuccessful();
        }
        Authentication userAuthentication = authentication.getUserAuthentication();
        if (userAuthentication == null || userAuthentication.getDetails() == null) {
            return AuthenticationResult.unsuccessful();
        }

        @SuppressWarnings("unchecked")

        String userId = ((HashMap<String, String>) userAuthentication.getDetails()).get("email");
        if (StringUtils.isEmpty(userId)) {
            return AuthenticationResult.unsuccessful();
        }

        AuthenticationResult authenticationResult = new AuthenticationResult(userId, true);
        authenticationResult.setGroups(getUserGroups(userId, engine));

        return authenticationResult;
    }

    private List<String> getUserGroups(String userId, ProcessEngine engine) {
        List<String> groupIds = new ArrayList<>();
        engine.getIdentityService().createGroupQuery().groupMember(userId).list()
                .forEach(g -> groupIds.add(g.getId()));
        return groupIds;
    }

}