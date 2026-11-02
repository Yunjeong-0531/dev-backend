package com.example.stay.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResetPasswordRequest(
        @NotBlank String token,
        @NotBlank @Pattern(regexp = Patterns.PASSWORD, message = Patterns.PASSWORD_MESSAGE) String newPassword
) {
}
