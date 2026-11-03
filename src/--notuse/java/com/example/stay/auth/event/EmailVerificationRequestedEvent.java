package com.example.stay.auth.event;

public record EmailVerificationRequestedEvent(String email, String name, String rawToken) {
}
