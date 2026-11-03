package com.example.stay.auth.config;

import com.example.stay.common.security.SecurityRuleContributor;
import org.springframework.stereotype.Component;

import java.util.List;

/** auth feature 의 API 중 토큰 없이 접근 가능한 경로를 선언한다. */
@Component
public class AuthSecurityRules implements SecurityRuleContributor {

    @Override
    public List<PublicEndpoint> publicEndpoints() {
        return List.of(
                PublicEndpoint.any("/api/auth/**"),   // 가입, 로그인, refresh, 이메일 인증, 비밀번호 재설정, 소셜
                PublicEndpoint.any("/api/terms"));    // 가입 화면 약관 목록
    }

    @Override
    public List<String> authenticatedPaths() {
        return List.of("/api/auth/logout-all");        // /api/auth/** 중 예외: 로그인 필요
    }
}
