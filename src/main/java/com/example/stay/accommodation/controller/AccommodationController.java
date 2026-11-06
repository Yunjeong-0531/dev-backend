package com.example.stay.accommodation.controller;

import com.example.stay.accommodation.dto.AccommodationRequest;
import com.example.stay.accommodation.dto.AccommodationResponse;
import com.example.stay.accommodation.service.AccommodationService;
import com.example.stay.common.security.AuthUser;
import com.example.stay.common.security.HostOrAdmin;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 권한 규칙 예시 (한 컨트롤러에서 세 가지 수준을 모두 보여준다)
 *
 *  | API                         | 누가 호출 가능?              | 어떻게 막나                                   |
 *  |-----------------------------|------------------------------|-----------------------------------------------|
 *  | GET    /api/accommodations  | 누구나(비로그인 포함)        | AccommodationSecurityRules (GET 공개)         |
 *  | POST   /api/accommodations  | HOST, ADMIN                  | @HostOrAdmin                                  |
 *  | PATCH  /api/accommodations/{id} | HOST(본인 숙소만), ADMIN | @HostOrAdmin + 서비스의 소유권 검사           |
 *  | DELETE /api/accommodations/{id} | HOST(본인 숙소만), ADMIN | @HostOrAdmin + 서비스의 소유권 검사           |
 */
@RestController
@RequestMapping("/api/accommodations")
@RequiredArgsConstructor
public class AccommodationController {

    private final AccommodationService service;

    @GetMapping
    public List<AccommodationResponse> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public AccommodationResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @HostOrAdmin
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccommodationResponse create(@AuthenticationPrincipal AuthUser user,
                                        @Valid @RequestBody AccommodationRequest req) {
        return service.create(user, req);
    }

    @HostOrAdmin
    @PatchMapping("/{id}")
    public AccommodationResponse update(@AuthenticationPrincipal AuthUser user,
                                        @PathVariable Long id,
                                        @Valid @RequestBody AccommodationRequest req) {
        return service.update(user, id, req);
    }

    @HostOrAdmin
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        service.delete(user, id);
        return ResponseEntity.noContent().build();
    }
}
