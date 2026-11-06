package com.example.stay.accommodation.exception;

import com.example.stay.common.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** feature 마다 자기 에러 코드 enum 을 만든다. 이름은 다른 feature 와 겹치지 않게 구체적으로. */
@Getter
@RequiredArgsConstructor
public enum AccommodationErrorCode implements ErrorCode {

    ACCOMMODATION_NOT_FOUND(HttpStatus.NOT_FOUND, "숙소를 찾을 수 없습니다."),
    NOT_ACCOMMODATION_OWNER(HttpStatus.FORBIDDEN, "본인이 등록한 숙소만 수정/삭제할 수 있습니다.");

    private final HttpStatus status;
    private final String message;
}
