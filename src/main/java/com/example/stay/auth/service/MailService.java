package com.example.stay.auth.service;

import com.example.stay.auth.config.AuthProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * 인증 메일 발송. 링크는 백엔드 API 가 아니라 "프론트 페이지"로 연결한다.
 * 프론트 페이지가 token 을 읽어 POST /api/auth/email/verify 를 호출하는 구조
 * (메일 보안 스캐너가 GET 링크를 미리 열어 토큰이 소모되는 문제 방지).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;
    private final AuthProperties props;

    public void sendEmailVerification(String to, String name, String rawToken) {
        String link = props.frontendBaseUrl() + "/verify-email?token=" + rawToken;
        String body = name + "님, 가입을 환영합니다.\n\n"
                + "아래 링크를 눌러 이메일 인증을 완료해 주세요.\n"
                + link + "\n\n"
                + "링크는 " + props.emailVerificationTtl().toHours() + "시간 동안 유효합니다.\n"
                + "본인이 가입하지 않았다면 이 메일을 무시해 주세요.";
        send(to, "[숙박예약] 이메일 인증을 완료해 주세요", body);
    }

    public void sendPasswordReset(String to, String name, String rawToken) {
        String link = props.frontendBaseUrl() + "/reset-password?token=" + rawToken;
        String body = name + "님, 비밀번호 재설정을 요청하셨습니다.\n\n"
                + "아래 링크에서 새 비밀번호를 설정해 주세요.\n"
                + link + "\n\n"
                + "링크는 " + props.passwordResetTtl().toMinutes() + "분 동안 유효하며 한 번만 사용할 수 있습니다.\n"
                + "본인이 요청하지 않았다면 이 메일을 무시해 주세요. 비밀번호는 변경되지 않습니다.";
        send(to, "[숙박예약] 비밀번호 재설정 안내", body);
    }

    private void send(String to, String subject, String text) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(props.mailFrom());
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(text);
            mailSender.send(msg);
        } catch (Exception e) {
            // 토큰이 담긴 본문은 로그에 남기지 않는다
            log.error("메일 발송 실패: to={}, subject={}", to, subject, e);
        }
    }
}
