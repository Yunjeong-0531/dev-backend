package com.example.stay.auth.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/** 동의/철회 이력. 상태 변경 시 UPDATE 하지 않고 행을 추가한다. */
@Getter
@Entity
@Table(name = "term_agreements")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TermAgreement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "term_id", nullable = false)
    private Term term;

    @Column(nullable = false)
    private boolean agreed;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @CreatedDate
    @Column(name = "agreed_at", nullable = false, updatable = false)
    private LocalDateTime agreedAt;

    public static TermAgreement of(User user, Term term, boolean agreed, String ipAddress) {
        TermAgreement a = new TermAgreement();
        a.user = user;
        a.term = term;
        a.agreed = agreed;
        a.ipAddress = ipAddress;
        return a;
    }
}
