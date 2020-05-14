package io.digital.patterns.workflow.security.rest;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;

@Component
@ConfigurationProperties(prefix = "rest.security")
@Validated
public class RestApiSecurityConfigurationProperties {

    @SuppressWarnings("unused")
    private Boolean enabled = true;

    @NotEmpty
    private String jwkSetUrl;

    @NotEmpty
    private String requiredAudience;

    public String getJwkSetUrl() {
        return jwkSetUrl;
    }

    public void setJwkSetUrl(String jwkSetUrl) {
        this.jwkSetUrl = jwkSetUrl;
    }

    public String getRequiredAudience() {
        return requiredAudience;
    }

    public void setRequiredAudience(String requiredAudience) {
        this.requiredAudience = requiredAudience;
    }

}