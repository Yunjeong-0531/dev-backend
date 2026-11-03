package com.example.stay.auth.dto;

import jakarta.validation.constraints.*;

import java.util.List;

public record SocialCompleteRequest(
        @NotBlank String signupToken,
        /** 소셜에서 이메일을 못 받은 경우에만 필수 */
        @Email @Size(max = 255) String email,
        @Size(max = 50) String name,
        @Pattern(regexp = Patterns.PHONE, message = Patterns.PHONE_MESSAGE) String phone,
        @NotNull List<Long> agreedTermIds
) {
}
