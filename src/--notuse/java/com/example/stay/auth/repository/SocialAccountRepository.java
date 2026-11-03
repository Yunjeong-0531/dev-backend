package com.example.stay.auth.repository;

import com.example.stay.auth.entity.SocialAccount;
import com.example.stay.auth.enums.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

    /** 소셜 로그인 시 회원까지 한 번에 조회 */
    @Query("select sa from SocialAccount sa join fetch sa.user " +
           "where sa.provider = :provider and sa.providerUserId = :providerUserId")
    Optional<SocialAccount> findByProviderAndProviderUserId(@Param("provider") SocialProvider provider,
                                                            @Param("providerUserId") String providerUserId);

    List<SocialAccount> findAllByUserId(Long userId);

    boolean existsByUserIdAndProvider(Long userId, SocialProvider provider);

    long countByUserId(Long userId);

    void deleteByUserIdAndProvider(Long userId, SocialProvider provider);
}
