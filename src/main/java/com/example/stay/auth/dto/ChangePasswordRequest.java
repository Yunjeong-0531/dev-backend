package com.example.stay.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangePasswordRequest(
        /** 소셜 전용 가입자(비밀번호 없음)가 처음 설정하는 경우에는 생략 가능 */
        String currentPassword,
        @NotBlank @Pattern(regexp = Patterns.PASSWORD, message = Patterns.PASSWORD_MESSAGE) String newPassword
) {
}
