package com.example.stay.auth.dto;

import jakarta.validation.constraints.*;

import java.util.List;

public record SignupRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Pattern(regexp = Patterns.PASSWORD, message = Patterns.PASSWORD_MESSAGE) String password,
        @NotBlank @Size(max = 50) String name,
        @Pattern(regexp = Patterns.PHONE, message = Patterns.PHONE_MESSAGE) String phone,
        /** 동의한 약관 ID 목록 (필수 약관은 모두 포함되어야 함) */
        @NotNull List<Long> agreedTermIds
) {
}
