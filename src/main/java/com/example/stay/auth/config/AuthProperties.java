package com.example.stay.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** application.yml 의 app.auth.* (JWT/CORS 는 공용이라 app.security.* 로 분리됨) */
@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(
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
        /** 비어 있으면 사용 안 함. 이 이메일로 "가입 + 이메일 인증"을 마친 계정을 서버 시작 시 ADMIN 으로 승격 (최초 관리자 생성용) */
        String initialAdminEmail
) {
}
