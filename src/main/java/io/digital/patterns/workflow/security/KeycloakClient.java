package io.digital.patterns.workflow.security;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.Collections;

import static java.util.Optional.ofNullable;

@Service
public class KeycloakClient {
    private final URI authUrl;
    private final String clientId;
    private final String clientSecret;
    private RestTemplate restTemplate;

    public KeycloakClient(@Value("${keycloak.auth.url}") final String authUrl,
                          @Value("${keycloak.realm}") final String authRealm,
                          @Value("${keycloak.client.id}") final String clientId,
                          @Value("${keycloak.client.secret}") String clientSecret) throws URISyntaxException {
        this.authUrl = new URI(authUrl + "/auth/realms/" + authRealm + "/protocol/openid-connect/token");
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.restTemplate = new RestTemplate();
    }

    public String bearerToken() {
        final MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");

        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("Authorization", authHeader());

        final ResponseEntity<KeycloakResult> result = restTemplate.postForEntity(authUrl,
                new HttpEntity<>(body, headers),
                KeycloakResult.class);

        return ofNullable(result.getBody()).map(KeycloakResult::getAccessToken)
                .orElseThrow(() -> new RuntimeException("Failed to get token from Keyclaok"));
    }


    private String authHeader() {
        String auth = clientId + ":" + clientSecret;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes());
        return "Basic " + new String(encodedAuth);
    }

    @Data
    private static class KeycloakResult {
        @JsonProperty("access_token")
        private String accessToken;
    }

}
