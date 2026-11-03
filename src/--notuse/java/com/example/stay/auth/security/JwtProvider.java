package com.example.stay.auth.security;

import com.example.stay.auth.config.AuthProperties;
import com.example.stay.auth.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

@Component
public class JwtProvider {

    private final SecretKey key;
    private final Duration accessTokenTtl;

    public JwtProvider(AuthProperties props) {
        byte[] secret = props.jwt().secret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("app.auth.jwt.secret 은 32바이트 이상이어야 합니다.");
        }
        this.key = Keys.hmacShaKeyFor(secret);
        this.accessTokenTtl = props.jwt().accessTokenTtl();
    }

    public String createAccessToken(Long userId, UserRole role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role.name())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTokenTtl.toMillis()))
                .signWith(key)
                .compact();
    }

    /** 서명/만료 검증 후 AuthUser 반환. 실패 시 JwtException 또는 IllegalArgumentException. */
    public AuthUser parse(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
        return new AuthUser(Long.valueOf(claims.getSubject()),
                UserRole.valueOf(claims.get("role", String.class)));
    }

    public long accessTokenTtlSeconds() {
        return accessTokenTtl.toSeconds();
    }
}
