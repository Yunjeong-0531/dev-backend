package com.example.stay.auth.service.social;

import com.example.stay.auth.enums.SocialProvider;
import com.example.stay.common.BusinessException;
import com.example.stay.common.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
class NaverProfileClient extends AbstractRestProfileClient {

    @Override
    public SocialProvider provider() {
        return SocialProvider.NAVER;
    }

    @Override
    public SocialProfile fetch(String accessToken) {
        Map<String, Object> body = getJson("https://openapi.naver.com/v1/nid/me", accessToken);
        Map<String, Object> res = map(body.get("response"));
        String id = str(res.get("id"));
        if (id == null) {
            throw new BusinessException(ErrorCode.SOCIAL_AUTH_FAILED);
        }
        // 네이버 계정 이메일은 네이버가 소유 검증한 값으로 취급
        String email = str(res.get("email"));
        return new SocialProfile(id, email, email != null, str(res.get("name")));
    }
}
