package com.app.Genderize.service;

import com.app.Genderize.config.SystemProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2Service {
    private final SystemProperties systemProperties;

    private final RestTemplate restTemplate = new RestTemplate();

    public String generateGithubAccessToken(String code, String codeVerifier) {
        return generateGithubAccessToken(code, codeVerifier, null);
    }

    public String generateGithubAccessToken(String code, String codeVerifier, String redirectUri) {
        // call GitHub token endpoint
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new HashMap<>();
        body.put("client_id", systemProperties.getGithubClientId());
        body.put("client_secret", systemProperties.getGithubClientSecret());
        body.put("code", code);
        body.put("code_verifier", codeVerifier);
        if (redirectUri != null && !redirectUri.isBlank()) {
            body.put("redirect_uri", redirectUri);
        }
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body,headers);
        String url = systemProperties.getGithubTokenUrl();
        log.info("Sending request {} to Github API {}", entity.getBody(), url);

        var response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
        log.info("github response: {}", response.getBody());
        Map<String, Object> responseBody = response.getBody();

        if (responseBody == null || responseBody.get("access_token") == null) {
            throw new RuntimeException("Failed to retrieve GitHub access token");
        }

        return (String) responseBody.get("access_token");
    }

    public Map<String, Object> getUser(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<?> entity = new HttpEntity<>(headers);
        String url = systemProperties.getGithubUserUrl();
        log.info("Sending request {} to Github API {}", entity.getBody(), url);
        var response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Map.class
        );
        log.info("github response: {}", response.getBody());
        if(response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        }else{
            return null;
        }

    }

    public String getPrimaryEmail(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<?> entity = new HttpEntity<>(headers);
        var response = restTemplate.exchange(
                "https://api.github.com/user/emails",
                HttpMethod.GET,
                entity,
                List.class
        );

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            return null;
        }

        for (Object item : response.getBody()) {
            if (item instanceof Map<?, ?> email) {
                boolean primary = Boolean.TRUE.equals(email.get("primary"));
                boolean verified = Boolean.TRUE.equals(email.get("verified"));
                Object value = email.get("email");
                if (primary && verified && value != null) {
                    return String.valueOf(value);
                }
            }
        }

        return null;
    }
}
