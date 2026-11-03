package com.example.stay.common.security;

import org.springframework.http.HttpMethod;

import java.util.List;

/**
 * 각 feature 가 "자기 API 의 공개/인증 규칙"을 스스로 등록하는 확장점.
 * SecurityConfig 를 직접 수정하지 않고, feature 안에 @Component 로 구현체를 두면 자동 반영된다.
 *
 * 예) 숙소 조회는 비로그인 허용, 등록/수정은 로그인 필요:
 *   @Component
 *   class AccommodationSecurityRules implements SecurityRuleContributor {
 *       public List<PublicEndpoint> publicEndpoints() {
 *           return List.of(PublicEndpoint.get("/api/accommodations/**"));   // GET 만 공개
 *       }
 *   }
 * 아무 규칙에도 해당하지 않는 경로는 기본적으로 "로그인 필요"다.
 */
public interface SecurityRuleContributor {

    /** 로그인 없이 접근 가능한 경로 */
    default List<PublicEndpoint> publicEndpoints() {
        return List.of();
    }

    /** publicEndpoints 패턴 안에 있지만 예외적으로 로그인이 필요한 경로 (예: /api/auth/logout-all). 공개 규칙보다 먼저 적용된다. */
    default List<String> authenticatedPaths() {
        return List.of();
    }

    /** method 가 null 이면 모든 HTTP 메서드 */
    record PublicEndpoint(HttpMethod method, String pattern) {
        public static PublicEndpoint any(String pattern) {
            return new PublicEndpoint(null, pattern);
        }

        public static PublicEndpoint get(String pattern) {
            return new PublicEndpoint(HttpMethod.GET, pattern);
        }
    }
}
