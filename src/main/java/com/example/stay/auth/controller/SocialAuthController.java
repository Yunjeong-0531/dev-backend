package com.example.stay.auth.controller;

import com.example.stay.auth.dto.SocialCompleteRequest;
import com.example.stay.auth.dto.SocialLoginRequest;
import com.example.stay.auth.dto.SocialLoginResponse;
import com.example.stay.auth.dto.TokenResponse;
import com.example.stay.auth.service.SocialAuthService;
import com.example.stay.common.ClientInfo;
import com.example.stay.common.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth/social")
@RequiredArgsConstructor
public class SocialAuthController {

    private final SocialAuthService socialAuthService;
    private final RateLimiter rateLimiter;

    /**
     * 소셜 로그인/가입 1단계.
     * 응답의 newUser=false 면 tokens 로 로그인 완료, true 면 signupToken 으로 /complete 호출.
     * (/complete 가 {provider} 와 겹치지 않도록 리터럴 경로가 우선 매칭된다)
     */
    @PostMapping("/{provider}")
    public SocialLoginResponse login(@PathVariable String provider,
                                     @Valid @RequestBody SocialLoginRequest req,
                                     HttpServletRequest http) {
        ClientInfo client = ClientInfo.from(http);
        rateLimiter.check("social-login:ip:" + client.ip(), 30, Duration.ofMinutes(1));
        return socialAuthService.login(provider, req, client);
    }

    /** 소셜 가입 2단계: 약관 동의 + 추가 정보 입력 → 가입 완료 및 로그인 */
    @PostMapping("/complete")
    public TokenResponse complete(@Valid @RequestBody SocialCompleteRequest req, HttpServletRequest http) {
        ClientInfo client = ClientInfo.from(http);
        rateLimiter.check("social-complete:ip:" + client.ip(), 10, Duration.ofMinutes(10));
        return socialAuthService.complete(req, client);
    }
}
