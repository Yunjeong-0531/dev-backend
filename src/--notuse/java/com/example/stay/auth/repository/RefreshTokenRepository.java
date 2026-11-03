package com.example.stay.auth.repository;

import com.example.stay.auth.entity.RefreshToken;
import com.example.stay.auth.enums.RevokedReason;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * refresh(rotation) 처리용. 같은 토큰으로 동시에 요청이 들어와
     * 둘 다 성공하는 상황을 막기 위해 비관적 락으로 조회한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select rt from RefreshToken rt join fetch rt.user where rt.tokenHash = :tokenHash")
    Optional<RefreshToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    /** 로그인 중인 기기 목록 */
    @Query("select rt from RefreshToken rt " +
           "where rt.user.id = :userId and rt.revokedAt is null and rt.expiresAt > :now " +
           "order by rt.createdAt desc")
    List<RefreshToken> findActiveByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /** 재사용 감지 시: 같은 family 전체 폐기 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update RefreshToken rt set rt.revokedAt = :now, rt.revokedReason = :reason " +
           "where rt.familyId = :familyId and rt.revokedAt is null")
    int revokeAllByFamilyId(@Param("familyId") String familyId,
                            @Param("reason") RevokedReason reason,
                            @Param("now") LocalDateTime now);

    /** 전체 기기 로그아웃 / 비밀번호 변경 / 탈퇴 시: 회원의 모든 토큰 폐기 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update RefreshToken rt set rt.revokedAt = :now, rt.revokedReason = :reason " +
           "where rt.user.id = :userId and rt.revokedAt is null")
    int revokeAllByUserId(@Param("userId") Long userId,
                          @Param("reason") RevokedReason reason,
                          @Param("now") LocalDateTime now);

    /** 만료 데이터 정리 배치용 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from RefreshToken rt where rt.expiresAt < :threshold")
    int deleteExpired(@Param("threshold") LocalDateTime threshold);
}
