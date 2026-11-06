package com.example.stay.auth.service;

import com.example.stay.auth.dto.UserSummary;
import com.example.stay.auth.entity.SocialAccount;
import com.example.stay.auth.entity.User;
import com.example.stay.auth.enums.RevokedReason;
import com.example.stay.auth.repository.SocialAccountRepository;
import com.example.stay.auth.repository.UserRepository;
import com.example.stay.common.BusinessException;
import com.example.stay.auth.exception.AuthErrorCode;
import com.example.stay.common.security.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final AuthTokenService tokenService;

    @Transactional(readOnly = true)
    public UserSummary getMe(Long userId) {
        return UserSummary.from(findActive(userId));
    }

    /**
     * DELETE /api/users/me : soft delete + 개인정보 익명화.
     * TODO: 예약 도메인 추가 후 "진행 중/예정 예약이 있으면 탈퇴 불가" 검증을 여기에 넣을 것.
     */
    @Transactional
    public void withdraw(Long userId) {
        User user = findActive(userId);
        user.withdraw();

        // 소셜 연동 해제 (같은 소셜 계정으로 재가입 가능하도록)
        List<SocialAccount> socials = socialAccountRepository.findAllByUserId(userId);
        socialAccountRepository.deleteAll(socials);

        // 벌크 쿼리는 마지막에 (위 변경이 먼저 flush 된다)
        tokenService.revokeAll(userId, RevokedReason.WITHDRAWN);
    }

    /**
     * PATCH /api/admin/users/{userId}/role (ADMIN 전용).
     *  - 본인의 역할은 바꿀 수 없다 (마지막 관리자가 스스로 권한을 잃는 사고 방지)
     *  - 역할이 바뀐 사용자는 모든 세션을 폐기해 다시 로그인하게 한다 (액세스 토큰은 만료(기본 15분)까지 이전 역할로 유효)
     */
    @Transactional
    public UserSummary changeRole(Long actorId, Long targetId, UserRole newRole) {
        if (actorId.equals(targetId)) {
            throw new BusinessException(AuthErrorCode.CANNOT_CHANGE_OWN_ROLE);
        }
        User target = findActive(targetId);
        if (target.getRole() == newRole) {
            return UserSummary.from(target);
        }
        target.changeRole(newRole);
        tokenService.revokeAll(targetId, RevokedReason.ROLE_CHANGED);   // 벌크 쿼리는 마지막에
        return UserSummary.from(target);
    }

    private User findActive(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));
        if (user.isWithdrawn()) {
            throw new BusinessException(AuthErrorCode.USER_NOT_FOUND);
        }
        return user;
    }
}
