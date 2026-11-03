package com.example.stay.auth.dto;

import com.example.stay.auth.entity.User;
import com.example.stay.common.security.UserRole;

public record UserSummary(Long id, String email, String name, UserRole role, boolean emailVerified) {

    public static UserSummary from(User user) {
        return new UserSummary(user.getId(), user.getEmail(), user.getName(),
                user.getRole(), user.isEmailVerified());
    }
}
