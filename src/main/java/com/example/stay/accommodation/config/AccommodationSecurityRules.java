package com.example.stay.accommodation.config;

import com.example.stay.common.security.SecurityRuleContributor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * [로그인 없이 볼 수 있는 API]를 feature 가 직접 선언한다. (SecurityConfig 를 수정하지 않는다)
 *  - GET /api/accommodations/**  → 누구나 조회 가능
 *  - 그 외(POST/PATCH/DELETE 등)는 선언하지 않았으므로 기본값인 "로그인 필요"
 * 역할 제한(HOST/ADMIN)은 여기가 아니라 컨트롤러의 @HostOrAdmin / @AdminOnly 로 건다.
 */
@Component
public class AccommodationSecurityRules implements SecurityRuleContributor {

    @Override
    public List<PublicEndpoint> publicEndpoints() {
        return List.of(PublicEndpoint.get("/api/accommodations/**"));
    }
}
