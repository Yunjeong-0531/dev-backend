package com.example.stay.auth.repository;

import com.example.stay.auth.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    @Query("select ev from EmailVerification ev join fetch ev.user where ev.tokenHash = :tokenHash")
    Optional<EmailVerification> findByTokenHash(@Param("tokenHash") String tokenHash);

    /** 재발송 쿨다운 체크용 (가장 최근 발송 건) */
    Optional<EmailVerification> findTopByUserIdOrderByCreatedAtDesc(Long userId);

    /** 재발송 시 기존 미사용 토큰 제거 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from EmailVerification ev where ev.user.id = :userId and ev.usedAt is null")
    int deleteUnusedByUserId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from EmailVerification ev where ev.expiresAt < :threshold")
    int deleteExpired(@Param("threshold") LocalDateTime threshold);
}
