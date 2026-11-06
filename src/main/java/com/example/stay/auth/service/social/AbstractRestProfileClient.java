package com.example.stay.auth.service.social;

import com.example.stay.auth.config.SocialProperties;
import com.example.stay.common.BusinessException;
import com.example.stay.auth.exception.AuthErrorCode;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

abstract class AbstractRestProfileClient implements SocialProfileClient {

    private final RestClient restClient = RestClient.create();

    protected Map<String, Object> getJson(String url, String accessToken) {
        try {
            Map<String, Object> body = restClient.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {
                    });
            if (body == null) {
                throw new BusinessException(AuthErrorCode.SOCIAL_AUTH_FAILED);
            }
            return body;
        } catch (RestClientException e) {
            throw new BusinessException(AuthErrorCode.SOCIAL_AUTH_FAILED);
        }
    }

    /** 표준 authorization_code 교환 폼. client-id 가 설정되지 않은 제공자면 UNSUPPORTED_PROVIDER. */
    protected static MultiValueMap<String, String> authorizationCodeForm(
            SocialProperties.Provider cred, String code, String redirectUri) {
        if (cred == null || !cred.configured()) {
            throw new BusinessException(AuthErrorCode.UNSUPPORTED_PROVIDER);
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", cred.clientId());
        form.add("code", code);
        form.add("redirect_uri", redirectUri);
        if (cred.clientSecret() != null && !cred.clientSecret().isBlank()) {
            form.add("client_secret", cred.clientSecret());
        }
        return form;
    }

    /** 토큰 엔드포인트로 form 을 POST 하고 access_token 을 꺼낸다. */
    protected String exchange(String tokenUrl, MultiValueMap<String, String> form) {
        try {
            Map<String, Object> body = restClient.post()
                    .uri(tokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {
                    });
            String token = body == null ? null : str(body.get("access_token"));
            if (token == null) {
                throw new BusinessException(AuthErrorCode.SOCIAL_AUTH_FAILED);
            }
            return token;
        } catch (RestClientException e) {
            throw new BusinessException(AuthErrorCode.SOCIAL_AUTH_FAILED);
        }
    }

    @SuppressWarnings("unchecked")
    protected static Map<String, Object> map(Object o) {
        return o instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    protected static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    protected static boolean bool(Object o) {
        return Boolean.TRUE.equals(o);
    }
}
