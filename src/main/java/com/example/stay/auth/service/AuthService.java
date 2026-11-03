package com.example.stay.auth.service;

import com.example.stay.auth.config.AuthProperties;
import com.example.stay.auth.dto.*;
import com.example.stay.auth.entity.LoginHistory;
import com.example.stay.auth.entity.User;
import com.example.stay.auth.enums.LoginMethod;
import com.example.stay.auth.enums.RevokedReason;
import com.example.stay.auth.enums.UserStatus;
import com.example.stay.auth.repository.LoginHistoryRepository;
import com.example.stay.auth.repository.UserRepository;
import com.example.stay.common.BusinessException;
import com.example.stay.common.ClientInfo;
import com.example.stay.common.EmailUtil;
import com.example.stay.auth.exception.AuthErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenService tokenService;
    private final EmailVerificationService emailVerificationService;
    private final TermService termService;
    private final AuthProperties props;

    /** 존재하지 않는 계정으로 로그인 시도할 때도 bcrypt 비교를 한 번 수행해 응답 시간 차이를 줄이기 위한 더미 해시 */
    private String dummyHash;

    @PostConstruct
    void init() {
        this.dummyHash = passwordEncoder.encode("dummy-password-for-timing");
    }

    // ------------------------------------------------------------------
    // 회원가입
    // ------------------------------------------------------------------
    @Transactional
    public SignupResponse signup(SignupRequest req, ClientInfo client) {
        String email = EmailUtil.normalize(req.email());
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(AuthErrorCode.DUPLICATE_EMAIL);
        }
        User user;
        try {
            user = userRepository.save(User.createLocal(
                    email,
                    passwordEncoder.encode(req.password()),
                    req.name().trim(),
                    normalizePhone(req.phone())));
        } catch (DataIntegrityViolationException e) {   // existsByEmail 이후 동시에 가입된 경우
            throw new BusinessException(AuthErrorCode.DUPLICATE_EMAIL);
        }

        termService.recordAgreements(user, req.agreedTermIds(), client.ip());
        emailVerificationService.issue(user);
        return SignupResponse.from(user);
    }

    // ------------------------------------------------------------------
    // 이메일 중복 확인
    // ------------------------------------------------------------------
    @Transactional(readOnly = true)
    public CheckEmailResponse checkEmail(String rawEmail) {
        return new CheckEmailResponse(!userRepository.existsByEmail(EmailUtil.normalize(rawEmail)));
    }

    // ------------------------------------------------------------------
    // 로그인
    // 실패 시 예외를 던지지만 실패 횟수/이력은 커밋되어야 하므로 noRollbackFor 지정
    // ------------------------------------------------------------------
    @Transactional(noRollbackFor = BusinessException.class)
    public TokenResponse login(LoginRequest req, ClientInfo client) {
        String email = EmailUtil.normalize(req.email());
        User user = userRepository.findByEmail(email).orElse(null);

        // 없는 계정 / 탈퇴 계정: 존재 여부를 숨기기 위해 동일한 에러
        if (user == null || user.isWithdrawn()) {
            passwordEncoder.matches(req.password(), dummyHash);
            recordFail(null, email, "BAD_CREDENTIALS", client);
            throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
        }
        if (user.isLocked()) {
            recordFail(user.getId(), email, "LOCKED", client);
            throw new BusinessException(AuthErrorCode.ACCOUNT_LOCKED);
        }
        // 소셜 전용 계정(비밀번호 없음)도 동일한 에러
        boolean passwordOk;
        if (user.hasPassword()) {
            passwordOk = passwordEncoder.matches(req.password(), user.getPasswordHash());
        } else {
            passwordEncoder.matches(req.password(), dummyHash);   // 응답 시간 맞추기용
            passwordOk = false;
        }
        if (!passwordOk) {
            user.recordLoginFailure(props.maxLoginAttempts(), props.lockDuration());
            recordFail(user.getId(), email, "BAD_CREDENTIALS", client);
            throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            recordFail(user.getId(), email, "SUSPENDED", client);
            throw new BusinessException(AuthErrorCode.ACCOUNT_SUSPENDED);
        }
        if (user.getStatus() == UserStatus.PENDING && props.requireEmailVerifiedForLogin()) {
            recordFail(user.getId(), email, "UNVERIFIED", client);
            throw new BusinessException(AuthErrorCode.EMAIL_NOT_VERIFIED);
        }

        user.recordLoginSuccess();
        loginHistoryRepository.save(LoginHistory.success(
                user.getId(), email, LoginMethod.PASSWORD, client.ip(), client.userAgent()));
        return tokenService.issueNewSession(user, client);
    }

    // ------------------------------------------------------------------
    // 토큰 재발급 / 로그아웃
    // ------------------------------------------------------------------
    public TokenResponse refresh(String rawRefreshToken, ClientInfo client) {
        return tokenService.rotate(rawRefreshToken, client);
    }

    public void logout(String rawRefreshToken) {
        tokenService.revoke(rawRefreshToken);
    }

    public void logoutAll(Long userId) {
        tokenService.revokeAll(userId, RevokedReason.LOGOUT_ALL);
    }

    // ------------------------------------------------------------------
    private void recordFail(Long userId, String email, String reason, ClientInfo client) {
        loginHistoryRepository.save(LoginHistory.fail(
                userId, email, LoginMethod.PASSWORD, reason, client.ip(), client.userAgent()));
    }

    private static String normalizePhone(String phone) {
        return (phone == null || phone.isBlank()) ? null : phone.replace("-", "");
    }
}
