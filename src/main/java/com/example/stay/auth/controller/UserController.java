package com.example.stay.auth.controller;

import com.example.stay.auth.dto.ChangePasswordRequest;
import com.example.stay.auth.dto.TokenResponse;
import com.example.stay.auth.dto.UserSummary;
import com.example.stay.common.security.AuthUser;
import com.example.stay.auth.service.PasswordService;
import com.example.stay.auth.service.UserService;
import com.example.stay.common.ClientInfo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** 로그인 상태(액세스 토큰 필요)에서 호출하는 내 계정 API */
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PasswordService passwordService;

    @GetMapping
    public UserSummary me(@AuthenticationPrincipal AuthUser user) {
        return userService.getMe(user.id());
    }

    /** 비밀번호 변경: 모든 기기 로그아웃 후, 현재 기기용 새 토큰을 응답으로 반환 */
    @PatchMapping("/password")
    public TokenResponse changePassword(@AuthenticationPrincipal AuthUser user,
                                        @Valid @RequestBody ChangePasswordRequest req,
                                        HttpServletRequest http) {
        return passwordService.change(user.id(), req.currentPassword(), req.newPassword(),
                ClientInfo.from(http));
    }

    /** 회원 탈퇴 (soft delete + 개인정보 익명화) */
    @DeleteMapping
    public ResponseEntity<Void> withdraw(@AuthenticationPrincipal AuthUser user) {
        userService.withdraw(user.id());
        return ResponseEntity.noContent().build();
    }
}
