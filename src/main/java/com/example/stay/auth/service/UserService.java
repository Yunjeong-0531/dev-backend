package com.example.stay.auth.service;

import com.example.stay.auth.dto.UserSummary;
import com.example.stay.auth.entity.SocialAccount;
import com.example.stay.auth.entity.User;
import com.example.stay.auth.enums.RevokedReason;
import com.example.stay.auth.repository.SocialAccountRepository;
import com.example.stay.auth.repository.UserRepository;
import com.example.stay.common.BusinessException;
import com.example.stay.auth.exception.AuthErrorCode;
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

    private User findActive(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));
        if (user.isWithdrawn()) {
            throw new BusinessException(AuthErrorCode.USER_NOT_FOUND);
        }
        return user;
    }
}
