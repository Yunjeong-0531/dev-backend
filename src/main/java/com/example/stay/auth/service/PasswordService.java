package com.example.stay.auth.service;

import com.example.stay.auth.config.AuthProperties;
import com.example.stay.auth.dto.TokenResponse;
import com.example.stay.auth.entity.PasswordResetToken;
import com.example.stay.auth.entity.User;
import com.example.stay.auth.enums.RevokedReason;
import com.example.stay.auth.event.PasswordResetRequestedEvent;
import com.example.stay.auth.repository.PasswordResetTokenRepository;
import com.example.stay.auth.repository.UserRepository;
import com.example.stay.common.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 주의: 벌크 UPDATE/DELETE 쿼리(clearAutomatically)는 영속성 컨텍스트를 비우므로,
 * 엔티티 변경은 반드시 벌크 쿼리 "앞"에서 끝내야 한다 (벌크 쿼리 전에 자동 flush 됨).
 */
@Service
@RequiredArgsConstructor
public class PasswordService {

    private static final int MAX_RESET_REQUESTS_PER_10_MIN = 3;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final AuthTokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final AuthProperties props;

    /**
     * POST /api/auth/password/forgot
     * 가입 여부와 무관하게 항상 같은 응답을 주기 위해, 대상이 없거나 조건 불충족이면 조용히 반환한다.
     */
    @Transactional
    public void forgot(String rawEmail, ClientInfo client) {
        String email = EmailUtil.normalize(rawEmail);
        userRepository.findByEmail(email)
                .filter(u -> !u.isWithdrawn())
                .ifPresent(user -> {
                    long recent = resetTokenRepository.countByUserIdAndCreatedAtAfter(
                            user.getId(), LocalDateTime.now().minusMinutes(10));
                    if (recent >= MAX_RESET_REQUESTS_PER_10_MIN) {
                        return;
                    }
                    String raw = TokenUtil.generate();
                    resetTokenRepository.save(PasswordResetToken.create(
                            user, TokenUtil.sha256Hex(raw),
                            LocalDateTime.now().plus(props.passwordResetTtl()), client.ip()));
                    eventPublisher.publishEvent(
                            new PasswordResetRequestedEvent(user.getEmail(), user.getName(), raw));
                });
    }

    /** POST /api/auth/password/reset */
    @Transactional
    public void reset(String rawToken, String newPassword) {
        PasswordResetToken token = resetTokenRepository.findByTokenHash(TokenUtil.sha256Hex(rawToken))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));
        if (token.isUsed()) {
            throw new BusinessException(ErrorCode.TOKEN_ALREADY_USED);
        }
        if (token.isExpired()) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
        }
        User user = token.getUser();
        if (user.isWithdrawn()) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        // 1) 엔티티 변경
        user.changePassword(passwordEncoder.encode(newPassword));
        if (!user.isEmailVerified()) {
            user.verifyEmail();     // 메일 수신을 증명했으므로 이메일 인증도 완료 처리
        }
        token.markUsed();

        // 2) 벌크 쿼리 (이 시점에 위 변경이 flush 된다)
        resetTokenRepository.deleteUnusedByUserId(user.getId());   // 같은 사용자의 남은 재설정 토큰 무효화
        tokenService.revokeAll(user.getId(), RevokedReason.PASSWORD_CHANGED);
    }

    /**
     * PATCH /api/users/me/password (로그인 상태)
     * 모든 기기의 세션을 끊고, 현재 기기에는 새 세션을 발급해 돌려준다.
     */
    @Transactional
    public TokenResponse change(Long userId, String currentPassword, String newPassword, ClientInfo client) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (user.isWithdrawn()) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        // 비밀번호가 이미 있으면 현재 비밀번호 확인 필수 (소셜 전용 가입자의 최초 설정은 예외)
        if (user.hasPassword()) {
            if (currentPassword == null
                    || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
                throw new BusinessException(ErrorCode.CURRENT_PASSWORD_MISMATCH);
            }
        }
        user.changePassword(passwordEncoder.encode(newPassword));
        tokenService.revokeAll(userId, RevokedReason.PASSWORD_CHANGED);   // 벌크: 영속성 컨텍스트 clear (flush 선행)
        return tokenService.issueNewSession(user, client);
    }
}
