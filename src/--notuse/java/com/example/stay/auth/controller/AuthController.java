package com.example.stay.auth.controller;

import com.example.stay.auth.dto.*;
import com.example.stay.auth.security.AuthUser;
import com.example.stay.auth.service.AuthService;
import com.example.stay.auth.service.EmailVerificationService;
import com.example.stay.auth.service.PasswordService;
import com.example.stay.common.ClientInfo;
import com.example.stay.common.EmailUtil;
import com.example.stay.common.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@Validated
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordService passwordService;
    private final RateLimiter rateLimiter;

    /** 회원가입 */
    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest req,
                                                 HttpServletRequest http) {
        ClientInfo client = ClientInfo.from(http);
        rateLimiter.check("signup:ip:" + client.ip(), 10, Duration.ofHours(1));
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(req, client));
    }

    /** 이메일 중복 확인 (계정 존재 여부가 드러나므로 rate limit 필수) */
    @GetMapping("/check-email")
    public CheckEmailResponse checkEmail(@RequestParam @NotBlank @Email String email,
                                         HttpServletRequest http) {
        rateLimiter.check("check-email:ip:" + ClientInfo.from(http).ip(), 30, Duration.ofMinutes(1));
        return authService.checkEmail(email);
    }

    /** 로그인 (액세스 + 리프레시 토큰 발급) */
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        ClientInfo client = ClientInfo.from(http);
        rateLimiter.check("login:ip:" + client.ip(), 30, Duration.ofMinutes(1));
        rateLimiter.check("login:email:" + EmailUtil.normalize(req.email()), 10, Duration.ofMinutes(5));
        return authService.login(req, client);
    }

    /** 액세스 토큰 재발급 (리프레시 토큰 rotation) */
    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest req, HttpServletRequest http) {
        ClientInfo client = ClientInfo.from(http);
        rateLimiter.check("refresh:ip:" + client.ip(), 60, Duration.ofMinutes(1));
        return authService.refresh(req.refreshToken(), client);
    }

    /** 로그아웃 (해당 리프레시 토큰 폐기) */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest req) {
        authService.logout(req.refreshToken());
        return ResponseEntity.noContent().build();
    }

    /** 전체 기기 로그아웃 (액세스 토큰 필요) */
    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(@AuthenticationPrincipal AuthUser user) {
        authService.logoutAll(user.id());
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------
    // 이메일 인증
    // ------------------------------------------------------------------

    /** 인증 메일 링크 → 프론트 페이지 → 이 API 를 POST 로 호출 */
    @PostMapping("/email/verify")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody EmailVerifyRequest req,
                                            HttpServletRequest http) {
        rateLimiter.check("email-verify:ip:" + ClientInfo.from(http).ip(), 20, Duration.ofMinutes(10));
        emailVerificationService.verify(req.token());
        return ResponseEntity.noContent().build();
    }

    /** 인증 메일 재발송 (가입 여부와 무관하게 항상 같은 응답) */
    @PostMapping("/email/resend")
    public MessageResponse resendEmail(@Valid @RequestBody EmailResendRequest req, HttpServletRequest http) {
        rateLimiter.check("email-resend:ip:" + ClientInfo.from(http).ip(), 5, Duration.ofMinutes(10));
        rateLimiter.check("email-resend:email:" + EmailUtil.normalize(req.email()), 3, Duration.ofMinutes(10));
        emailVerificationService.resend(req.email());
        return new MessageResponse("가입된 이메일이라면 인증 메일을 발송했습니다.");
    }

    // ------------------------------------------------------------------
    // 비밀번호 재설정
    // ------------------------------------------------------------------

    /** 재설정 메일 요청 (가입 여부와 무관하게 항상 같은 응답) */
    @PostMapping("/password/forgot")
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest req,
                                          HttpServletRequest http) {
        ClientInfo client = ClientInfo.from(http);
        rateLimiter.check("pw-forgot:ip:" + client.ip(), 5, Duration.ofMinutes(10));
        rateLimiter.check("pw-forgot:email:" + EmailUtil.normalize(req.email()), 3, Duration.ofMinutes(10));
        passwordService.forgot(req.email(), client);
        return new MessageResponse("가입된 이메일이라면 비밀번호 재설정 메일을 발송했습니다.");
    }

    /** 새 비밀번호 설정 */
    @PostMapping("/password/reset")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest req,
                                              HttpServletRequest http) {
        rateLimiter.check("pw-reset:ip:" + ClientInfo.from(http).ip(), 10, Duration.ofMinutes(10));
        passwordService.reset(req.token(), req.newPassword());
        return ResponseEntity.noContent().build();
    }
}
