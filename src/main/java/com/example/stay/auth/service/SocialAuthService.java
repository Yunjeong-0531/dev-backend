package com.example.stay.auth.service;

import com.example.stay.auth.config.AuthProperties;
import com.example.stay.auth.config.SocialProperties;
import com.example.stay.auth.dto.SocialCompleteRequest;
import com.example.stay.auth.dto.SocialLoginRequest;
import com.example.stay.auth.dto.SocialLoginResponse;
import com.example.stay.auth.dto.TokenResponse;
import com.example.stay.auth.entity.LoginHistory;
import com.example.stay.auth.entity.PendingSocialSignup;
import com.example.stay.auth.entity.SocialAccount;
import com.example.stay.auth.entity.User;
import com.example.stay.auth.enums.LoginMethod;
import com.example.stay.auth.enums.SocialProvider;
import com.example.stay.auth.enums.UserStatus;
import com.example.stay.auth.repository.LoginHistoryRepository;
import com.example.stay.auth.repository.PendingSocialSignupRepository;
import com.example.stay.auth.repository.SocialAccountRepository;
import com.example.stay.auth.repository.UserRepository;
import com.example.stay.auth.service.social.SocialProfile;
import com.example.stay.auth.service.social.SocialProfileClient;
import com.example.stay.common.*;
import com.example.stay.auth.exception.AuthErrorCode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class SocialAuthService {

    private final Map<SocialProvider, SocialProfileClient> clients = new EnumMap<>(SocialProvider.class);
    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final PendingSocialSignupRepository pendingRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final AuthTokenService tokenService;
    private final EmailVerificationService emailVerificationService;
    private final TermService termService;
    private final AuthProperties props;
    private final SocialProperties socialProps;

    public SocialAuthService(List<SocialProfileClient> profileClients,
                             UserRepository userRepository,
                             SocialAccountRepository socialAccountRepository,
                             PendingSocialSignupRepository pendingRepository,
                             LoginHistoryRepository loginHistoryRepository,
                             AuthTokenService tokenService,
                             EmailVerificationService emailVerificationService,
                             TermService termService,
                             AuthProperties props,
                             SocialProperties socialProps) {
        profileClients.forEach(c -> clients.put(c.provider(), c));
        this.userRepository = userRepository;
        this.socialAccountRepository = socialAccountRepository;
        this.pendingRepository = pendingRepository;
        this.loginHistoryRepository = loginHistoryRepository;
        this.tokenService = tokenService;
        this.emailVerificationService = emailVerificationService;
        this.termService = termService;
        this.props = props;
        this.socialProps = socialProps;
    }

    /**
     * POST /api/auth/social/{provider}
     *  - 이미 연동된 계정: 바로 로그인
     *  - 신규: 가입 대기 레코드를 만들고 signupToken 반환 (→ /social/complete 에서 약관 동의 후 가입 완료)
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public SocialLoginResponse login(String providerName, SocialLoginRequest req, ClientInfo client) {
        SocialProvider provider = parseProvider(providerName);
        SocialProfileClient profileClient = clientOf(provider);
        SocialProfile profile = profileClient.fetch(resolveAccessToken(provider, profileClient, req));

        var existing = socialAccountRepository
                .findByProviderAndProviderUserId(provider, profile.providerUserId());
        if (existing.isPresent()) {
            User user = existing.get().getUser();
            LoginMethod method = LoginMethod.valueOf(provider.name());
            if (user.getStatus() == UserStatus.SUSPENDED) {
                loginHistoryRepository.save(LoginHistory.fail(
                        user.getId(), user.getEmail(), method, "SUSPENDED", client.ip(), client.userAgent()));
                throw new BusinessException(AuthErrorCode.ACCOUNT_SUSPENDED);
            }
            user.recordLoginSuccess();
            loginHistoryRepository.save(LoginHistory.success(
                    user.getId(), user.getEmail(), method, client.ip(), client.userAgent()));
            return SocialLoginResponse.loggedIn(tokenService.issueNewSession(user, client));
        }

        // 신규. 제공자가 "검증했다"고 한 이메일만 신뢰한다.
        String verifiedEmail = profile.emailVerified() ? EmailUtil.normalize(profile.email()) : null;

        // 같은 이메일의 기존 회원이 있으면 자동 연동하지 않는다 (계정 탈취 위험).
        // 이메일 로그인 후 설정에서 소셜 계정을 연동하도록 안내.
        if (verifiedEmail != null && userRepository.existsByEmail(verifiedEmail)) {
            throw new BusinessException(AuthErrorCode.SOCIAL_EMAIL_CONFLICT);
        }

        String rawSignupToken = TokenUtil.generate();
        pendingRepository.save(PendingSocialSignup.create(
                TokenUtil.sha256Hex(rawSignupToken), provider, profile.providerUserId(),
                verifiedEmail, profile.name(), LocalDateTime.now().plus(props.signupTokenTtl())));
        return SocialLoginResponse.signupRequired(rawSignupToken, verifiedEmail, profile.name());
    }

    /** POST /api/auth/social/complete : 약관 동의 + 추가정보 입력 후 가입 완료 및 로그인 */
    @Transactional
    public TokenResponse complete(SocialCompleteRequest req, ClientInfo client) {
        PendingSocialSignup pending = pendingRepository
                .findBySignupTokenHash(TokenUtil.sha256Hex(req.signupToken()))
                .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_SIGNUP_TOKEN));
        if (pending.isExpired()) {
            throw new BusinessException(AuthErrorCode.INVALID_SIGNUP_TOKEN);
        }

        String email;
        boolean emailVerified;
        if (pending.getProviderEmail() != null) {
            email = pending.getProviderEmail();
            emailVerified = true;
        } else {
            if (req.email() == null || req.email().isBlank()) {
                throw new BusinessException(AuthErrorCode.EMAIL_REQUIRED);
            }
            email = EmailUtil.normalize(req.email());
            emailVerified = false;      // 직접 입력한 이메일은 인증 메일로 확인
        }
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(emailVerified
                    ? AuthErrorCode.SOCIAL_EMAIL_CONFLICT : AuthErrorCode.DUPLICATE_EMAIL);
        }

        User user;
        try {
            user = userRepository.save(User.createSocial(
                    email, resolveName(req, pending, email), normalizePhone(req.phone()), emailVerified));
        } catch (DataIntegrityViolationException e) {   // 이메일 동시 가입
            throw new BusinessException(AuthErrorCode.DUPLICATE_EMAIL);
        }
        try {
            socialAccountRepository.save(SocialAccount.link(
                    user, pending.getProvider(), pending.getProviderUserId(), pending.getProviderEmail()));
        } catch (DataIntegrityViolationException e) {   // 같은 소셜 계정으로 동시 가입
            throw new BusinessException(AuthErrorCode.SOCIAL_ALREADY_LINKED);
        }
        termService.recordAgreements(user, req.agreedTermIds(), client.ip());
        pendingRepository.delete(pending);      // 일회용

        // 엔티티 변경은 벌크 쿼리를 쓰는 issue() 보다 먼저 (벌크 쿼리 후엔 user 가 detach 됨)
        user.recordLoginSuccess();
        loginHistoryRepository.save(LoginHistory.success(
                user.getId(), email, LoginMethod.valueOf(pending.getProvider().name()),
                client.ip(), client.userAgent()));
        if (!emailVerified) {
            emailVerificationService.issue(user);
        }
        return tokenService.issueNewSession(user, client);
    }

    // ------------------------------------------------------------------

    /**
     * 제공자 액세스 토큰을 얻는다.
     *  - 인가 코드: redirectUri 가 "우리 프론트의 콜백 주소"와 정확히 같을 때만 교환한다
     *    (frontend-base-url + /auth/callback/{provider}). 제공자 콘솔에 등록한 redirect URI 와 같아야 한다.
     *  - 액세스 토큰: 개발용 설정(allow-access-token)일 때만 허용
     */
    private String resolveAccessToken(SocialProvider provider, SocialProfileClient profileClient,
                                      SocialLoginRequest req) {
        if (hasText(req.accessToken())) {
            if (!socialProps.allowAccessToken()) {
                throw new BusinessException(CommonErrorCode.INVALID_INPUT);
            }
            return req.accessToken();
        }
        if (!hasText(req.code()) || !hasText(req.redirectUri())) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT);
        }
        String expectedRedirect = props.frontendBaseUrl() + "/auth/callback/" + provider.name().toLowerCase(Locale.ROOT);
        if (!expectedRedirect.equals(req.redirectUri())) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT);
        }
        return profileClient.exchangeCode(req.code(), req.redirectUri(), req.state());
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }


    private SocialProvider parseProvider(String name) {
        try {
            return SocialProvider.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(AuthErrorCode.UNSUPPORTED_PROVIDER);
        }
    }

    private SocialProfileClient clientOf(SocialProvider provider) {
        SocialProfileClient c = clients.get(provider);
        if (c == null) {       // 예: APPLE (아직 미구현)
            throw new BusinessException(AuthErrorCode.UNSUPPORTED_PROVIDER);
        }
        return c;
    }

    private static String resolveName(SocialCompleteRequest req, PendingSocialSignup pending, String email) {
        String name = req.name();
        if (name == null || name.isBlank()) name = pending.getProviderName();
        if (name == null || name.isBlank()) name = email.substring(0, email.indexOf('@'));
        name = name.trim();
        return name.length() > 50 ? name.substring(0, 50) : name;
    }

    private static String normalizePhone(String phone) {
        return (phone == null || phone.isBlank()) ? null : phone.replace("-", "");
    }
}
