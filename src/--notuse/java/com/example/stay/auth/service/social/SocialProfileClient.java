package com.example.stay.auth.service.social;

import com.example.stay.auth.enums.SocialProvider;

public interface SocialProfileClient {

    SocialProvider provider();

    /** provider access token 으로 프로필 조회. 실패 시 BusinessException(SOCIAL_AUTH_FAILED). */
    SocialProfile fetch(String accessToken);
}
