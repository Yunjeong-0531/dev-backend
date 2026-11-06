package com.example.stay.auth.service.social;

import com.example.stay.auth.config.SocialProperties;
import com.example.stay.auth.enums.SocialProvider;
import com.example.stay.common.BusinessException;
import com.example.stay.auth.exception.AuthErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;

import java.util.Map;

@Component
class NaverProfileClient extends AbstractRestProfileClient {

    private final SocialProperties.Provider cred;

    NaverProfileClient(SocialProperties props) {
        this.cred = props.naver();
    }


    @Override
    public SocialProvider provider() {
        return SocialProvider.NAVER;
    }


    @Override
    public String exchangeCode(String code, String redirectUri, String state) {
        MultiValueMap<String, String> form = authorizationCodeForm(cred, code, redirectUri);
        form.remove("redirect_uri");          // 네이버는 redirect_uri 대신 state 를 요구한다
        form.add("state", state == null ? "" : state);
        return exchange("https://nid.naver.com/oauth2.0/token", form);
    }


    @Override
    public SocialProfile fetch(String accessToken) {
        Map<String, Object> body = getJson("https://openapi.naver.com/v1/nid/me", accessToken);
        Map<String, Object> res = map(body.get("response"));
        String id = str(res.get("id"));
        if (id == null) {
            throw new BusinessException(AuthErrorCode.SOCIAL_AUTH_FAILED);
        }
        // 네이버 계정 이메일은 네이버가 소유 검증한 값으로 취급
        String email = str(res.get("email"));
        return new SocialProfile(id, email, email != null, str(res.get("name")));
    }
}
