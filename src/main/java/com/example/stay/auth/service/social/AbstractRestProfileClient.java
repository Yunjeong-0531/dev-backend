package com.example.stay.auth.service.social;

import com.example.stay.common.BusinessException;
import com.example.stay.auth.exception.AuthErrorCode;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
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
