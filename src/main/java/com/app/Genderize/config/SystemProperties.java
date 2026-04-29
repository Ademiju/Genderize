package com.app.Genderize.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties
@Configuration
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SystemProperties {
    private String genderizeBaseUrl;
    private String agifyBaseUrl;
    private String nationalizeBaseUrl;
    private String jwtSecret;
    private String githubClientId;
    private String githubClientSecret;
    private String githubTokenUrl;
    private String githubUserUrl;
    private String githubAuthorizeUrl;
}
