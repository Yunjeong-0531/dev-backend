package com.example.stay.auth.entity;

import com.example.stay.auth.enums.RevokedReason;
import com.example.stay.common.BaseCreatedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "refresh_tokens")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, length = 64, columnDefinition = "CHAR(64)")
    private String tokenHash;

    /** 로그인 1회 = 1 family. rotation 되어도 유지된다. */
    @Column(name = "family_id", nullable = false, length = 36, columnDefinition = "CHAR(36)")
    private String familyId;

    @Column(name = "replaced_by_id")
    private Long replacedById;

    @Column(name = "device_name", length = 100)
    private String deviceName;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "revoked_reason")
    private RevokedReason revokedReason;

    /** 신규 로그인 시 발급 (새 family) / rotation 시 기존 familyId 를 넘겨 발급. */
    public static RefreshToken issue(User user, String tokenHash, String familyId,
                                     LocalDateTime expiresAt, String deviceName,
                                     String userAgent, String ipAddress) {
        RefreshToken t = new RefreshToken();
        t.user = user;
        t.tokenHash = tokenHash;
        t.familyId = familyId;
        t.expiresAt = expiresAt;
        t.deviceName = deviceName;
        t.userAgent = userAgent;
        t.ipAddress = ipAddress;
        return t;
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired() {
        return expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isUsable() {
        return !isRevoked() && !isExpired();
    }

    public void revoke(RevokedReason reason) {
        if (this.revokedAt == null) {
            this.revokedAt = LocalDateTime.now();
            this.revokedReason = reason;
        }
    }

    /** rotation: 현재 토큰을 폐기하고 새 토큰 id 를 연결. */
    public void markRotated(Long newTokenId) {
        revoke(RevokedReason.ROTATED);
        this.replacedById = newTokenId;
    }
}
