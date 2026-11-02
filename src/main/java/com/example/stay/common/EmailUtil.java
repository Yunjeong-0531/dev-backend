package com.example.stay.common;

import java.util.Locale;

public final class EmailUtil {

    private EmailUtil() {
    }

    /** 앞뒤 공백 제거 + 소문자. null 이면 null. */
    public static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
