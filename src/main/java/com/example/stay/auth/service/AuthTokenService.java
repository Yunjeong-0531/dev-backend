package com.example.stay.auth.service;

import com.example.stay.auth.config.AuthProperties;
import com.example.stay.auth.dto.TokenResponse;
import com.example.stay.auth.dto.UserSummary;
import com.example.stay.auth.entity.RefreshToken;
import com.example.stay.auth.entity.User;
import com.example.stay.auth.enums.RevokedReason;
import com.example.stay.auth.enums.UserStatus;
import com.example.stay.auth.repository.RefreshTokenRepository;
import com.example.stay.common.security.JwtProvider;
import com.example.stay.common.BusinessException;
import com.example.stay.common.ClientInfo;
import com.example.stay.auth.exception.AuthErrorCode;
import com.example.stay.common.TokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/** 액세스/리프레시 토큰 발급, rotation, 폐기를 담당. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthTokenService {

    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthProperties props;

    private record Issued(RefreshToken entity, TokenResponse response) {
    }

    /** 로그인 성공 시: 새 family 로 세션 시작 */
    @Transactional
    public TokenResponse issueNewSession(User user, ClientInfo client) {
        return issue(user, UUID.randomUUID().toString(), client).response();
    }

    /**
     * 리프레시 토큰 rotation.
     *  - 정상: 기존 토큰 폐기(ROTATED) + 같은 family 로 새 토큰 발급
     *  - 이미 rotation 된 토큰이 다시 들어오면 탈취로 간주 → family 전체 폐기
     * 예외를 던져도 폐기 결과는 커밋되어야 하므로 noRollbackFor 지정.
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public TokenResponse rotate(String rawRefreshToken, ClientInfo client) {
        String hash = TokenUtil.sha256Hex(rawRefreshToken);
        RefreshToken old = refreshTokenRepository.findByTokenHashForUpdate(hash)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN));

        if (old.isRevoked()) {
            if (old.getRevokedReason() == RevokedReason.ROTATED) {
                log.warn("리프레시 토큰 재사용 감지: userId={}, familyId={}, ip={}",
                        old.getUser().getId(), old.getFamilyId(), client.ip());
                refreshTokenRepository.revokeAllByFamilyId(
                        old.getFamilyId(), RevokedReason.REUSE_DETECTED, LocalDateTime.now());
            }
            throw new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }
        if (old.isExpired()) {
            throw new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        User user = old.getUser();
        if (user.getStatus() == UserStatus.WITHDRAWN || user.getStatus() == UserStatus.SUSPENDED) {
            old.revoke(RevokedReason.LOGOUT);
            throw new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        Issued next = issue(user, old.getFamilyId(), client);
        old.markRotated(next.entity().getId());
        return next.response();
    }

    /** 로그아웃: 해당 리프레시 토큰만 폐기. 없는/이미 폐기된 토큰이어도 조용히 성공(멱등). */
    @Transactional
    public void revoke(String rawRefreshToken) {
        refreshTokenRepository.findByTokenHash(TokenUtil.sha256Hex(rawRefreshToken))
                .ifPresent(t -> t.revoke(RevokedReason.LOGOUT));
    }

    /** 회원의 모든 리프레시 토큰 폐기 (전체 로그아웃/비밀번호 변경/탈퇴). 벌크 쿼리이므로 영속성 컨텍스트가 비워진다. */
    @Transactional
    public void revokeAll(Long userId, RevokedReason reason) {
        refreshTokenRepository.revokeAllByUserId(userId, reason, LocalDateTime.now());
    }

    private Issued issue(User user, String familyId, ClientInfo client) {
        String raw = TokenUtil.generate();
        LocalDateTime expiresAt = LocalDateTime.now().plus(props.refreshTokenTtl());
        RefreshToken saved = refreshTokenRepository.save(RefreshToken.issue(
                user, TokenUtil.sha256Hex(raw), familyId, expiresAt,
                client.deviceName(), client.userAgent(), client.ip()));

        String access = jwtProvider.createAccessToken(user.getId(), user.getRole());
        TokenResponse response = TokenResponse.of(
                access, jwtProvider.accessTokenTtlSeconds(),
                raw, props.refreshTokenTtl().toSeconds(),
                UserSummary.from(user));
        return new Issued(saved, response);
    }
}
