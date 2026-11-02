package com.example.stay.auth.entity;

import com.example.stay.common.BaseCreatedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "password_reset_tokens")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PasswordResetToken extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, length = 64, columnDefinition = "CHAR(64)")
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "requested_ip", length = 45)
    private String requestedIp;

    public static PasswordResetToken create(User user, String tokenHash,
                                            LocalDateTime expiresAt, String requestedIp) {
        PasswordResetToken t = new PasswordResetToken();
        t.user = user;
        t.tokenHash = tokenHash;
        t.expiresAt = expiresAt;
        t.requestedIp = requestedIp;
        return t;
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public boolean isExpired() {
        return expiresAt.isBefore(LocalDateTime.now());
    }

    public void markUsed() {
        this.usedAt = LocalDateTime.now();
    }
}
