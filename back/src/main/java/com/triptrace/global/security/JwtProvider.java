package com.triptrace.global.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * 토큰 자체를 다루는 클래스. (발급 + 검증)
 * 발급은 로그인/재발급 시 AuthService가, 검증은 요청마다 JwtFilter가 사용한다.
 */
@Component
public class JwtProvider {
    private final SecretKey secretKey;
    private final long accessTokenExpirationSeconds;

    public JwtProvider(
        @Value("${custom.jwt.secretKey}") String secretKey,
        @Value("${custom.accessToken.expirationSeconds}") long accessTokenExpirationSeconds
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationSeconds = accessTokenExpirationSeconds;
    }

    // [발급] 로그인 성공 시 내려줄 액세스 토큰 생성
    public String generateAccessToken(Long memberId, String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpirationSeconds * 1000);

        return Jwts.builder()
            .claim("memberId", memberId)
            .claim("email", email)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(secretKey, Jwts.SIG.HS256)
            .compact();
    }

    // [발급] DB에 저장할 리프레시 토큰 생성 (JWT 아닌 UUID)
    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }

    // [검증] 액세스 토큰이 위변조/만료되지 않았는지 확인
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // [해석] 토큰에서 로그인 사용자 식별 정보 추출
    public Long getMemberId(String token) {
        return ((Number) parseClaims(token).get("memberId")).longValue();
    }

    public String getEmail(String token) {
        return parseClaims(token).get("email", String.class);
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
