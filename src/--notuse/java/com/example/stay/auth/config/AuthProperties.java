package com.example.stay.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(
        Jwt jwt,
        Duration refreshTokenTtl,
        Duration emailVerificationTtl,
        Duration emailResendCooldown,
        Duration passwordResetTtl,
        Duration signupTokenTtl,
        int maxLoginAttempts,
        Duration lockDuration,
        /** true 면 이메일 인증 전 로그인 불가, false 면 로그인은 허용하고 emailVerified=false 로 내려줌 */
        boolean requireEmailVerifiedForLogin,
        String frontendBaseUrl,
        String mailFrom,
        List<String> allowedOrigins
) {
    public record Jwt(String secret, Duration accessTokenTtl) {
    }
}
