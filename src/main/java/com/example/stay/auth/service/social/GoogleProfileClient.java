package com.example.stay.auth.service.social;

import com.example.stay.auth.config.SocialProperties;
import com.example.stay.auth.enums.SocialProvider;
import com.example.stay.common.BusinessException;
import com.example.stay.auth.exception.AuthErrorCode;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 주의: userinfo 는 "이 토큰이 우리 앱용으로 발급됐는지(audience)"를 검증하지 않는다.
 * 운영에서는 id_token 검증(aud == 우리 client_id) 방식으로 바꾸는 것을 권장.
 */
@Component
class GoogleProfileClient extends AbstractRestProfileClient {

    private final SocialProperties.Provider cred;

    GoogleProfileClient(SocialProperties props) {
        this.cred = props.google();
    }

    @Override
    public String exchangeCode(String code, String redirectUri, String state) {
        return exchange("https://oauth2.googleapis.com/token", authorizationCodeForm(cred, code, redirectUri));
    }

    @Override
    public SocialProvider provider() {
        return SocialProvider.GOOGLE;
    }

    @Override
    public SocialProfile fetch(String accessToken) {
        Map<String, Object> body = getJson("https://www.googleapis.com/oauth2/v3/userinfo", accessToken);
        String id = str(body.get("sub"));
        if (id == null) {
            throw new BusinessException(AuthErrorCode.SOCIAL_AUTH_FAILED);
        }
        return new SocialProfile(id, str(body.get("email")), bool(body.get("email_verified")), str(body.get("name")));
    }
}
