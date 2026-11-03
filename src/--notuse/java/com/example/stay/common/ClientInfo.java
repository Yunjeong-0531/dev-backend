package com.example.stay.common;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 요청 클라이언트 정보.
 * IP 는 request.getRemoteAddr() 만 사용한다. 프록시/로드밸런서 뒤라면
 * server.forward-headers-strategy=framework 설정으로 실제 IP 가 채워지게 할 것
 * (X-Forwarded-For 를 직접 파싱하면 헤더 위조로 rate limit 을 우회당할 수 있다).
 */
public record ClientInfo(String ip, String userAgent, String deviceName) {

    public static ClientInfo from(HttpServletRequest request) {
        return new ClientInfo(
                request.getRemoteAddr(),
                truncate(request.getHeader("User-Agent"), 500),
                truncate(request.getHeader("X-Device-Name"), 100));
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
