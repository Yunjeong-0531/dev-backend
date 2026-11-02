package com.example.stay.auth.service;

import com.example.stay.auth.repository.EmailVerificationRepository;
import com.example.stay.auth.repository.PasswordResetTokenRepository;
import com.example.stay.auth.repository.PendingSocialSignupRepository;
import com.example.stay.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** 만료된 토큰성 데이터를 매일 새벽에 정리. (다중 인스턴스라면 ShedLock 등으로 중복 실행 방지 필요) */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthCleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PendingSocialSignupRepository pendingSocialSignupRepository;

    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void cleanup() {
        // 만료 후 하루 지난 것만 삭제 (재사용 감지/분쟁 확인 여유)
        LocalDateTime threshold = LocalDateTime.now().minusDays(1);
        int rt = refreshTokenRepository.deleteExpired(threshold);
        int ev = emailVerificationRepository.deleteExpired(threshold);
        int pr = passwordResetTokenRepository.deleteExpired(threshold);
        int ps = pendingSocialSignupRepository.deleteExpired(threshold);
        log.info("인증 데이터 정리: refresh={}, emailVerif={}, pwReset={}, pendingSocial={}", rt, ev, pr, ps);
    }
}
