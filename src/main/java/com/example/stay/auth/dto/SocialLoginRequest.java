package com.example.stay.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 프론트가 소셜 SDK/OAuth 로 받아온 provider access token */
public record SocialLoginRequest(
        @Size(max = 2048) String accessToken,
        @Size(max = 2048) String code,
        @Size(max = 512) String redirectUri,
        @Size(max = 256) String state
) {
}
