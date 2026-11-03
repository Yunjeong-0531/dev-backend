package com.example.stay.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record EmailVerifyRequest(@NotBlank String token) {
}
