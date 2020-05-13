package io.digital.patterns.workflow;

import io.digital.patterns.workflow.security.KeycloakBearerTokenInterceptor;
import io.digital.patterns.workflow.security.KeycloakClient;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ApplicationConfiguration {
    @Bean
    public RestTemplate restTemplate(KeycloakClient keycloakClient,
                                     RestTemplateBuilder builder) {
        KeycloakBearerTokenInterceptor keycloakBearerTokenInterceptor =
                new KeycloakBearerTokenInterceptor(keycloakClient);
        RestTemplate restTemplate = builder.build();
        restTemplate.getInterceptors().add(keycloakBearerTokenInterceptor);
        return restTemplate;
    }
}
