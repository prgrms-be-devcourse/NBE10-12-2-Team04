package com.triptrace.domain.auth.auth.dto;

/**
 * 로그인 결과를 서비스 → 컨트롤러로 전달하는 내부 전용 객체.
 * accessToken은 응답 body(LoginResponse)로, refreshToken은 HttpOnly 쿠키로 나뉘어 나간다.
 */
public record LoginResult(
    String accessToken,
    String refreshToken
) {
}
