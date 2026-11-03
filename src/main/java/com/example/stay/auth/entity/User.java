package com.example.stay.auth.entity;

import com.example.stay.common.security.UserRole;
import com.example.stay.auth.enums.UserStatus;
import com.example.stay.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 소문자 정규화된 이메일 */
    @Column(nullable = false, length = 255)
    private String email;

    /** 소셜 전용 가입자는 null */
    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 20)
    private String phone;

    @Column(name = "phone_verified_at")
    private LocalDateTime phoneVerifiedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    @Column(name = "failed_login_count", nullable = false)
    private int failedLoginCount;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // ------------------------------------------------------------------
    // 생성
    // ------------------------------------------------------------------

    /** 이메일/비밀번호 가입. 이메일 인증 전까지 PENDING. */
    public static User createLocal(String email, String passwordHash, String name, String phone) {
        User u = new User();
        u.email = email;
        u.passwordHash = passwordHash;
        u.name = name;
        u.phone = phone;
        u.role = UserRole.GUEST;
        u.status = UserStatus.PENDING;
        u.passwordChangedAt = LocalDateTime.now();
        return u;
    }

    /** 소셜 가입. 제공자가 이메일을 검증해 준 경우 바로 ACTIVE. */
    public static User createSocial(String email, String name, String phone, boolean emailVerifiedByProvider) {
        User u = new User();
        u.email = email;
        u.name = name;
        u.phone = phone;
        u.role = UserRole.GUEST;
        if (emailVerifiedByProvider) {
            u.status = UserStatus.ACTIVE;
            u.emailVerifiedAt = LocalDateTime.now();
        } else {
            u.status = UserStatus.PENDING;
        }
        return u;
    }

    // ------------------------------------------------------------------
    // 도메인 로직
    // ------------------------------------------------------------------

    public void verifyEmail() {
        this.emailVerifiedAt = LocalDateTime.now();
        if (this.status == UserStatus.PENDING) {
            this.status = UserStatus.ACTIVE;
        }
    }

    public boolean isEmailVerified() {
        return emailVerifiedAt != null;
    }

    public boolean hasPassword() {
        return passwordHash != null;
    }

    public boolean isLocked() {
        return lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now());
    }

    public boolean isWithdrawn() {
        return status == UserStatus.WITHDRAWN;
    }

    /** 로그인 실패 누적. maxAttempts 도달 시 lockDuration 만큼 잠금. */
    public void recordLoginFailure(int maxAttempts, Duration lockDuration) {
        this.failedLoginCount++;
        if (this.failedLoginCount >= maxAttempts) {
            this.lockedUntil = LocalDateTime.now().plus(lockDuration);
            this.failedLoginCount = 0;
        }
    }

    public void recordLoginSuccess() {
        this.failedLoginCount = 0;
        this.lockedUntil = null;
        this.lastLoginAt = LocalDateTime.now();
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
        this.passwordChangedAt = LocalDateTime.now();
        this.failedLoginCount = 0;
        this.lockedUntil = null;
    }

    public void verifyPhone(String phone) {
        this.phone = phone;
        this.phoneVerifiedAt = LocalDateTime.now();
    }

    public void changeRole(UserRole role) {
        this.role = role;
    }

    public void suspend() {
        this.status = UserStatus.SUSPENDED;
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
    }

    /** 탈퇴: soft delete + 개인정보 익명화 (email UNIQUE 충돌 방지). */
    public void withdraw() {
        this.status = UserStatus.WITHDRAWN;
        this.deletedAt = LocalDateTime.now();
        this.email = "deleted_" + this.id + "@withdrawn.local";
        this.passwordHash = null;
        this.name = "탈퇴회원";
        this.phone = null;
        this.phoneVerifiedAt = null;
    }
}
