package com.example.stay.auth.event;

public record PasswordResetRequestedEvent(String email, String name, String rawToken) {
}
