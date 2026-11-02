package com.example.stay.auth.dto;

public record TokenResponse(
        String tokenType,
        String accessToken,
        long accessTokenExpiresIn,     // 초
        String refreshToken,
        long refreshTokenExpiresIn,    // 초
        UserSummary user
) {
    public static TokenResponse of(String accessToken, long accessTtlSec,
                                   String refreshToken, long refreshTtlSec, UserSummary user) {
        return new TokenResponse("Bearer", accessToken, accessTtlSec, refreshToken, refreshTtlSec, user);
    }
}
