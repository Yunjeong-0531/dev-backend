package com.example.stay.auth.entity;

import com.example.stay.auth.enums.SocialProvider;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "social_accounts")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SocialAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SocialProvider provider;

    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    @Column(name = "provider_email", length = 255)
    private String providerEmail;

    @CreatedDate
    @Column(name = "linked_at", nullable = false, updatable = false)
    private LocalDateTime linkedAt;

    public static SocialAccount link(User user, SocialProvider provider,
                                     String providerUserId, String providerEmail) {
        SocialAccount sa = new SocialAccount();
        sa.user = user;
        sa.provider = provider;
        sa.providerUserId = providerUserId;
        sa.providerEmail = providerEmail;
        return sa;
    }
}
