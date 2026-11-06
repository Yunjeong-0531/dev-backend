package com.example.stay.accommodation.controller;

import com.example.stay.accommodation.dto.AccommodationResponse;
import com.example.stay.accommodation.service.AccommodationService;
import com.example.stay.common.security.AdminOnly;
import com.example.stay.common.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 관리자 전용 API 예시.
 *  1) 경로를 /api/admin/ 아래에 둔다  → SecurityConfig 가 경로 규칙으로 ADMIN 만 허용
 *  2) 클래스에 @AdminOnly 를 붙인다    → 혹시 경로 규칙이 바뀌어도 메서드 보안이 한 번 더 막는다
 */
@RestController
@RequestMapping("/api/admin/accommodations")
@AdminOnly
@RequiredArgsConstructor
public class AdminAccommodationController {

    private final AccommodationService service;

    @GetMapping
    public List<AccommodationResponse> listAll() {
        return service.list();
    }

    /** 관리자는 소유권과 무관하게 삭제 가능 (서비스의 assertCanModify 가 ADMIN 을 통과시킨다) */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> forceDelete(@AuthenticationPrincipal AuthUser admin, @PathVariable Long id) {
        service.delete(admin, id);
        return ResponseEntity.noContent().build();
    }
}
