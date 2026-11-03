package com.example.stay.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 기존 회원이면 newUser=false + tokens,
 * 신규 회원이면 newUser=true + signupToken (이후 /social/complete 로 가입 마무리).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SocialLoginResponse(
        boolean newUser,
        TokenResponse tokens,
        String signupToken,
        String email,      // 소셜에서 받은 (검증된) 이메일. 없으면 null → complete 에서 직접 입력
        String name
) {
    public static SocialLoginResponse loggedIn(TokenResponse tokens) {
        return new SocialLoginResponse(false, tokens, null, null, null);
    }

    public static SocialLoginResponse signupRequired(String signupToken, String email, String name) {
        return new SocialLoginResponse(true, null, signupToken, email, name);
    }
}
