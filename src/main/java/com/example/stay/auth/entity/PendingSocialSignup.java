package com.example.stay.auth.entity;

import com.example.stay.auth.enums.SocialProvider;
import com.example.stay.common.BaseCreatedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "pending_social_signups")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PendingSocialSignup extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "signup_token_hash", nullable = false, length = 64, columnDefinition = "CHAR(64)")
    private String signupTokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SocialProvider provider;

    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    @Column(name = "provider_email", length = 255)
    private String providerEmail;

    @Column(name = "provider_name", length = 100)
    private String providerName;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    public static PendingSocialSignup create(String signupTokenHash, SocialProvider provider,
                                             String providerUserId, String providerEmail,
                                             String providerName, LocalDateTime expiresAt) {
        PendingSocialSignup p = new PendingSocialSignup();
        p.signupTokenHash = signupTokenHash;
        p.provider = provider;
        p.providerUserId = providerUserId;
        p.providerEmail = providerEmail;
        p.providerName = providerName;
        p.expiresAt = expiresAt;
        return p;
    }

    public boolean isExpired() {
        return expiresAt.isBefore(LocalDateTime.now());
    }
}
