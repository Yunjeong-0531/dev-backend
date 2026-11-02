package com.example.stay.auth.repository;

import com.example.stay.auth.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    @Query("select t from PasswordResetToken t join fetch t.user where t.tokenHash = :tokenHash")
    Optional<PasswordResetToken> findByTokenHash(@Param("tokenHash") String tokenHash);

    /** 요청 빈도 제한 보조용 (최근 N분 내 요청 수) */
    long countByUserIdAndCreatedAtAfter(Long userId, LocalDateTime after);

    /** 새 재설정 메일 발송 시 기존 미사용 토큰 제거 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from PasswordResetToken t where t.user.id = :userId and t.usedAt is null")
    int deleteUnusedByUserId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from PasswordResetToken t where t.expiresAt < :threshold")
    int deleteExpired(@Param("threshold") LocalDateTime threshold);
}
