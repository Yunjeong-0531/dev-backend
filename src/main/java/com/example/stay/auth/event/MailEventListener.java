package com.example.stay.auth.event;

import com.example.stay.auth.service.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** 트랜잭션이 커밋된 뒤에만 비동기로 메일을 보낸다 (롤백된 토큰으로 메일이 나가는 것 방지). */
@Component
@RequiredArgsConstructor
public class MailEventListener {

    private final MailService mailService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEmailVerification(EmailVerificationRequestedEvent e) {
        mailService.sendEmailVerification(e.email(), e.name(), e.rawToken());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPasswordReset(PasswordResetRequestedEvent e) {
        mailService.sendPasswordReset(e.email(), e.name(), e.rawToken());
    }
}
