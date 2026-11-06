package com.example.stay.auth.service;

import com.example.stay.auth.config.AuthProperties;
import com.example.stay.auth.repository.UserRepository;
import com.example.stay.common.EmailUtil;
import com.example.stay.common.security.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 최초 관리자 만들기.
 * 1) 일반 회원으로 가입하고 이메일 인증을 마친다
 * 2) 환경변수 INITIAL_ADMIN_EMAIL(app.auth.initial-admin-email)에 그 이메일을 넣고 서버를 (재)시작한다
 * → 해당 계정이 ADMIN 으로 승격된다. 이후 관리자 추가는 PATCH /api/admin/users/{id}/role 로 한다.
 * 이메일 인증이 끝난 계정만 승격하므로, 남의 이메일로 먼저 가입해서 관리자가 되는 것을 막는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrapRunner implements ApplicationRunner {

    private final AuthProperties props;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String raw = props.initialAdminEmail();
        if (raw == null || raw.isBlank()) {
            return;
        }
        String email = EmailUtil.normalize(raw);
        userRepository.findByEmail(email).ifPresentOrElse(user -> {
            if (user.getRole() == UserRole.ADMIN) {
                return;
            }
            if (user.isWithdrawn() || !user.isEmailVerified()) {
                log.warn("초기 관리자 승격 건너뜀: 탈퇴했거나 이메일 인증이 끝나지 않은 계정입니다. ({})", email);
                return;
            }
            user.changeRole(UserRole.ADMIN);
            log.info("초기 관리자 승격 완료: {}", email);
        }, () -> log.info("초기 관리자 계정({})이 아직 가입되지 않았습니다. 가입 + 이메일 인증 후 서버를 재시작하세요.", email));
    }
}
