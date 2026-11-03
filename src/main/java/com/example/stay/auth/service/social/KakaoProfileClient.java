package com.example.stay.auth.service.social;

import com.example.stay.auth.enums.SocialProvider;
import com.example.stay.common.BusinessException;
import com.example.stay.auth.exception.AuthErrorCode;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
class KakaoProfileClient extends AbstractRestProfileClient {

    @Override
    public SocialProvider provider() {
        return SocialProvider.KAKAO;
    }

    @Override
    public SocialProfile fetch(String accessToken) {
        Map<String, Object> body = getJson("https://kapi.kakao.com/v2/user/me", accessToken);
        String id = str(body.get("id"));
        if (id == null) {
            throw new BusinessException(AuthErrorCode.SOCIAL_AUTH_FAILED);
        }
        Map<String, Object> account = map(body.get("kakao_account"));
        Map<String, Object> profile = map(account.get("profile"));
        boolean verified = bool(account.get("is_email_valid")) && bool(account.get("is_email_verified"));
        return new SocialProfile(id, str(account.get("email")), verified, str(profile.get("nickname")));
    }
}
