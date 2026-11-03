package com.example.stay.common.security;

/** 모든 feature 가 권한 판단에 쓰는 역할. (DB 에는 이름 문자열로 저장) */
public enum UserRole {
    GUEST, HOST, ADMIN
}
