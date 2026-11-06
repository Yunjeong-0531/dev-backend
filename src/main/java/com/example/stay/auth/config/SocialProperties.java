package com.example.stay.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml 의 app.auth.social.*
 * client-secret 은 서버에만 둔다 (프론트에는 client-id 만 둔다).
 */
@ConfigurationProperties(prefix = "app.auth.social")
public record SocialProperties(
        /**
         * true 면 프론트가 보낸 "제공자 액세스 토큰"을 그대로 받는다 (개발용 토큰 직접 입력 화면).
         * 이 방식은 토큰이 우리 앱용으로 발급됐는지 확인하기 어려우므로 운영에서는 false(기본값)로 둔다.
         */
        boolean allowAccessToken,
        Provider kakao,
        Provider naver,
        Provider google
) {
    public record Provider(String clientId, String clientSecret) {
        public boolean configured() {
            return clientId != null && !clientId.isBlank();
        }
    }
}
