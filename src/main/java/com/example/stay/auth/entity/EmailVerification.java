package com.example.stay.auth.entity;

import com.example.stay.common.BaseCreatedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "email_verifications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVerification extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 발송 시점의 이메일 */
    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "token_hash", nullable = false, length = 64, columnDefinition = "CHAR(64)")
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    public static EmailVerification create(User user, String email, String tokenHash, LocalDateTime expiresAt) {
        EmailVerification e = new EmailVerification();
        e.user = user;
        e.email = email;
        e.tokenHash = tokenHash;
        e.expiresAt = expiresAt;
        return e;
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
