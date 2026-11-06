package com.example.stay.auth.controller;

import com.example.stay.auth.dto.ChangeRoleRequest;
import com.example.stay.auth.service.UserService;
import com.example.stay.auth.dto.UserSummary;
import com.example.stay.common.security.AdminOnly;
import com.example.stay.common.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 전용 회원 관리.
 * 경로가 /api/admin/ 아래라 SecurityConfig 의 경로 규칙으로도 막히고, 클래스에 @AdminOnly 도 붙어 있다(이중 안전장치).
 */
@RestController
@RequestMapping("/api/admin/users")
@AdminOnly
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    /** 회원의 역할 변경 (GUEST / HOST / ADMIN) */
    @PatchMapping("/{userId}/role")
    public UserSummary changeRole(@AuthenticationPrincipal AuthUser admin,
                                  @PathVariable Long userId,
                                  @Valid @RequestBody ChangeRoleRequest req) {
        return userService.changeRole(admin.id(), userId, req.role());
    }
}
