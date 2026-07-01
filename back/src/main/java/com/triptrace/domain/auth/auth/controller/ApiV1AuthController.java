package com.triptrace.domain.auth.auth.controller;

import com.triptrace.domain.auth.auth.dto.LoginRequest;
import com.triptrace.domain.auth.auth.dto.LoginResponse;
import com.triptrace.domain.auth.auth.dto.ReissueResponse;
import com.triptrace.domain.auth.auth.dto.SignupRequest;
import com.triptrace.domain.auth.auth.dto.SignupResponse;
import com.triptrace.domain.auth.auth.dto.TokenPair;
import com.triptrace.domain.auth.auth.service.AuthService;
import com.triptrace.global.rsData.RsData;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ApiV1AuthController {
    private final AuthService authService;

    @Value("${custom.refreshToken.expirationSeconds}")
    private long refreshTokenExpirationSeconds;

    @PostMapping("/auth/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public RsData<SignupResponse> signup(@RequestBody @Valid SignupRequest request) {
        SignupResponse response = authService.signup(request);

        return new RsData<>("201-1", "회원가입 성공", response);
    }

    @PostMapping("/auth/login")
    public RsData<LoginResponse> login(
        @RequestBody @Valid LoginRequest request,
        HttpServletResponse response
    ) {
        TokenPair tokens = authService.login(request);
        addRefreshTokenCookie(response, tokens.refreshToken());

        return new RsData<>("200-1", "로그인 성공", new LoginResponse(tokens.accessToken()));
    }

    @PostMapping("/auth/reissue")
    public RsData<ReissueResponse> reissue(
        @CookieValue(value = "refreshToken", required = false) String refreshToken,
        HttpServletResponse response
    ) {
        TokenPair tokens = authService.reissue(refreshToken);
        addRefreshTokenCookie(response, tokens.refreshToken());

        return new RsData<>("200-1", "토큰 재발급 성공", new ReissueResponse(tokens.accessToken()));
    }

    // RT를 HttpOnly 쿠키로 내려준다. (로그인/재발급 공통)
    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
            .httpOnly(true)
            .sameSite("Strict")
            .path("/api/v1/auth")
            .maxAge(Duration.ofSeconds(refreshTokenExpirationSeconds))
            .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
