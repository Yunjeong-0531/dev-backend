package com.example.stay.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** 프론트가 소셜 SDK/OAuth 로 받아온 provider access token */
public record SocialLoginRequest(@NotBlank String accessToken) {
}
