package com.example.stay.auth.dto;

import com.example.stay.common.security.UserRole;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(@NotNull UserRole role) {
}
