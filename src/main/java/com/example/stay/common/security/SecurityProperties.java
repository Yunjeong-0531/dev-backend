package com.example.stay.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/** application.yml 의 app.security.* */
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
        Jwt jwt,
        /** CORS 허용 Origin 목록 */
        List<String> allowedOrigins
) {
    public record Jwt(String secret, Duration accessTokenTtl) {
    }
}
