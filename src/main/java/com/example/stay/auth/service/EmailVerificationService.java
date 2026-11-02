package com.example.stay.auth.service;

import com.example.stay.auth.config.AuthProperties;
import com.example.stay.auth.entity.EmailVerification;
import com.example.stay.auth.entity.User;
import com.example.stay.auth.enums.UserStatus;
import com.example.stay.auth.event.EmailVerificationRequestedEvent;
import com.example.stay.auth.repository.EmailVerificationRepository;
import com.example.stay.auth.repository.UserRepository;
import com.example.stay.common.BusinessException;
import com.example.stay.common.EmailUtil;
import com.example.stay.common.ErrorCode;
import com.example.stay.common.TokenUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationRepository emailVerificationRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AuthProperties props;

    /** 인증 토큰 생성 + 메일 발송 이벤트 발행(커밋 후 발송). 기존 미사용 토큰은 제거. */
    @Transactional
    public void issue(User user) {
        emailVerificationRepository.deleteUnusedByUserId(user.getId());
        String raw = TokenUtil.generate();
        emailVerificationRepository.save(EmailVerification.create(
                user, user.getEmail(), TokenUtil.sha256Hex(raw),
                LocalDateTime.now().plus(props.emailVerificationTtl())));
        eventPublisher.publishEvent(new EmailVerificationRequestedEvent(user.getEmail(), user.getName(), raw));
    }

    /** POST /api/auth/email/verify */
    @Transactional
    public void verify(String rawToken) {
        EmailVerification ev = emailVerificationRepository.findByTokenHash(TokenUtil.sha256Hex(rawToken))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));
        if (ev.isUsed()) {
            throw new BusinessException(ErrorCode.TOKEN_ALREADY_USED);
        }
        if (ev.isExpired()) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
        }
        User user = ev.getUser();
        // 탈퇴했거나, 토큰 발급 이후 이메일이 바뀐 경우는 무효
        if (user.isWithdrawn() || !user.getEmail().equals(ev.getEmail())) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        ev.markUsed();
        user.verifyEmail();
    }

    /**
     * POST /api/auth/email/resend
     * 가입 여부/인증 여부와 무관하게 항상 같은 응답을 주기 위해, 조건 불충족 시 조용히 반환한다.
     */
    @Transactional
    public void resend(String rawEmail) {
        String email = EmailUtil.normalize(rawEmail);
        userRepository.findByEmail(email)
                .filter(u -> u.getStatus() == UserStatus.PENDING && !u.isEmailVerified())
                .ifPresent(user -> {
                    boolean coolingDown = emailVerificationRepository
                            .findTopByUserIdOrderByCreatedAtDesc(user.getId())
                            .map(last -> last.getCreatedAt()
                                    .plus(props.emailResendCooldown()).isAfter(LocalDateTime.now()))
                            .orElse(false);
                    if (!coolingDown) {
                        issue(user);
                    }
                });
    }
}
