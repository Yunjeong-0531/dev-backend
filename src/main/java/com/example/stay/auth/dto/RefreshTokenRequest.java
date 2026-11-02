package com.example.stay.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** /refresh, /logout 공용 */
public record RefreshTokenRequest(@NotBlank String refreshToken) {
}
