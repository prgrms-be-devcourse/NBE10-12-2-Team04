package com.triptrace.domain.auth.auth.dto;

/**
 * 발급된 (accessToken, refreshToken) 한 쌍을 서비스 → 컨트롤러로 전달하는 내부 전용 객체.
 * 로그인·재발급이 공용으로 사용한다. accessToken은 응답 body로, refreshToken은 HttpOnly 쿠키로 나뉘어 나간다.
 */
public record TokenPair(
    String accessToken,
    String refreshToken
) {
}
