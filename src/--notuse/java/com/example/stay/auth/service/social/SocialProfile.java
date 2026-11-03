package com.example.stay.auth.service.social;

/** 소셜 제공자에서 가져온 표준화된 프로필 */
public record SocialProfile(String providerUserId, String email, boolean emailVerified, String name) {
}
