package com.example.stay.auth.dto;

public final class Patterns {

    /** 8~64자, 영문+숫자 각 1자 이상, 공백 불가 */
    public static final String PASSWORD = "^(?=.*[A-Za-z])(?=.*\\d)\\S{8,64}$";
    public static final String PASSWORD_MESSAGE = "비밀번호는 영문과 숫자를 포함해 8~64자여야 합니다.";

    public static final String PHONE = "^01[0-9]-?\\d{3,4}-?\\d{4}$";
    public static final String PHONE_MESSAGE = "휴대폰 번호 형식이 올바르지 않습니다.";

    private Patterns() {
    }
}
