package com.example.stay.auth.repository;

import com.example.stay.auth.entity.PendingSocialSignup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PendingSocialSignupRepository extends JpaRepository<PendingSocialSignup, Long> {

    Optional<PendingSocialSignup> findBySignupTokenHash(String signupTokenHash);

    /** 만료 데이터 정리 배치용 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from PendingSocialSignup p where p.expiresAt < :threshold")
    int deleteExpired(@Param("threshold") LocalDateTime threshold);
}
