package com.example.stay.common.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 호스트(HOST) 또는 관리자(ADMIN)만 호출 가능. 컨트롤러 클래스 또는 메서드에 붙인다.
 * "본인 숙소만 수정" 같은 소유권 검사는 이 애노테이션이 아니라 서비스에서 한다.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasAnyRole('HOST','ADMIN')")
public @interface HostOrAdmin {
}
