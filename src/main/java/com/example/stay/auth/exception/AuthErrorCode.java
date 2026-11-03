package com.example.stay.auth.exception;

import com.example.stay.common.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    ACCOUNT_LOCKED(HttpStatus.LOCKED, "로그인 실패가 반복되어 계정이 일시적으로 잠겼습니다."),
    ACCOUNT_SUSPENDED(HttpStatus.FORBIDDEN, "이용이 정지된 계정입니다."),
    EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN, "이메일 인증이 필요합니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원 정보를 찾을 수 없습니다."),
    CURRENT_PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "현재 비밀번호가 일치하지 않습니다."),

    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다."),
    INVALID_TOKEN(HttpStatus.BAD_REQUEST, "유효하지 않은 토큰입니다."),
    TOKEN_EXPIRED(HttpStatus.BAD_REQUEST, "만료된 토큰입니다."),
    TOKEN_ALREADY_USED(HttpStatus.BAD_REQUEST, "이미 사용된 토큰입니다."),

    INVALID_TERMS(HttpStatus.BAD_REQUEST, "유효하지 않은 약관입니다."),
    REQUIRED_TERMS_NOT_AGREED(HttpStatus.BAD_REQUEST, "필수 약관에 모두 동의해야 합니다."),

    UNSUPPORTED_PROVIDER(HttpStatus.BAD_REQUEST, "지원하지 않는 소셜 로그인입니다."),
    SOCIAL_AUTH_FAILED(HttpStatus.UNAUTHORIZED, "소셜 로그인 인증에 실패했습니다."),
    SOCIAL_EMAIL_CONFLICT(HttpStatus.CONFLICT, "이미 같은 이메일로 가입된 계정이 있습니다. 이메일로 로그인한 뒤 소셜 계정을 연동해 주세요."),
    SOCIAL_ALREADY_LINKED(HttpStatus.CONFLICT, "이미 가입된 소셜 계정입니다."),
    INVALID_SIGNUP_TOKEN(HttpStatus.BAD_REQUEST, "가입 정보가 만료되었거나 유효하지 않습니다. 다시 시도해 주세요."),
    EMAIL_REQUIRED(HttpStatus.BAD_REQUEST, "이메일을 입력해 주세요.");

    private final HttpStatus status;
    private final String message;
}
