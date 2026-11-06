package com.example.stay.common.security;

/**
 * SecurityContext 에 담기는 로그인 사용자.
 * 모든 feature 의 컨트롤러에서 @AuthenticationPrincipal AuthUser 로 주입받아 user.id() 를 쓴다.
 * (다른 feature 는 auth 의 User 엔티티가 아니라 이 id 만 참조할 것)
 */
public record AuthUser(Long id, UserRole role) {

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    public boolean isHost() {
        return role == UserRole.HOST;
    }
}
