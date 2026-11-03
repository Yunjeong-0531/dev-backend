package com.example.stay.auth.repository;

import com.example.stay.auth.entity.LoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {

    /** 특정 이메일에 대한 최근 실패 횟수 */
    long countByEmailAndSuccessFalseAndCreatedAtAfter(String email, LocalDateTime after);

    /** 특정 IP 의 최근 실패 횟수 (무차별 대입 탐지) */
    long countByIpAddressAndSuccessFalseAndCreatedAtAfter(String ipAddress, LocalDateTime after);

    /** 내 로그인 기록 조회 */
    List<LoginHistory> findTop20ByUserIdOrderByCreatedAtDesc(Long userId);
}
