package com.example.stay.auth.dto;

import com.example.stay.auth.entity.User;
import com.example.stay.auth.enums.UserStatus;

public record SignupResponse(Long userId, String email, UserStatus status) {

    public static SignupResponse from(User user) {
        return new SignupResponse(user.getId(), user.getEmail(), user.getStatus());
    }
}
