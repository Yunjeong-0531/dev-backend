package com.example.stay.common;

import org.springframework.http.HttpStatus;

/**
 * 에러 코드 규약. 각 feature 가 자기 enum 을 만들어 이 인터페이스를 구현한다.
 *   common  : CommonErrorCode  (입력 오류, 401/403, 429, 500 등 모든 feature 공통)
 *   auth    : AuthErrorCode    (auth/exception)
 *   그 외   : <Feature>ErrorCode
 * enum 이면 name() 은 자동으로 구현된다. 응답의 code 필드에는 name() 이 내려간다.
 * (feature 사이에 같은 이름이 생기지 않도록 구체적인 이름을 쓸 것: 예 ACCOMMODATION_NOT_FOUND)
 */
public interface ErrorCode {

    String name();

    HttpStatus getStatus();

    String getMessage();
}
