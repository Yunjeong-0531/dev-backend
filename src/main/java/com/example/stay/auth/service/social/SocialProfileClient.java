package com.example.stay.auth.service.social;

import com.example.stay.auth.enums.SocialProvider;

public interface SocialProfileClient {

    SocialProvider provider();

    /** provider access token 으로 프로필 조회. 실패 시 BusinessException(SOCIAL_AUTH_FAILED). */
    SocialProfile fetch(String accessToken);


    /**
     * 인가 코드를 제공자 액세스 토큰으로 교환한다 (서버가 client-secret 으로 수행).
     * 설정(client-id)이 없으면 UNSUPPORTED_PROVIDER, 교환 실패 시 SOCIAL_AUTH_FAILED.
     */
    String exchangeCode(String code, String redirectUri, String state);
}
