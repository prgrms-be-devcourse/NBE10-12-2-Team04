package com.triptrace.domain.auth.auth.controller;

import com.triptrace.domain.auth.auth.dto.LoginRequest;
import com.triptrace.domain.auth.auth.dto.LoginResponse;
import com.triptrace.domain.auth.auth.dto.LoginResult;
import com.triptrace.domain.auth.auth.dto.SignupRequest;
import com.triptrace.domain.auth.auth.dto.SignupResponse;
import com.triptrace.domain.auth.auth.service.AuthService;
import com.triptrace.global.rsData.RsData;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
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
        LoginResult result = authService.login(request);

        // RT는 body에 노출하지 않고 HttpOnly 쿠키로만 내려준다.
        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", result.refreshToken())
            .httpOnly(true)
            .sameSite("Strict")
            .path("/api/v1/auth")
            .maxAge(Duration.ofSeconds(refreshTokenExpirationSeconds))
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

        return new RsData<>("200-1", "로그인 성공", new LoginResponse(result.accessToken()));
    }
}
