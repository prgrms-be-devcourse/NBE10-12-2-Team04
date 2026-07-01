package com.triptrace.domain.auth.auth.controller;

import com.triptrace.domain.auth.auth.dto.SignupRequest;
import com.triptrace.domain.auth.auth.dto.SignupResponse;
import com.triptrace.domain.auth.auth.service.AuthService;
import com.triptrace.global.rsData.RsData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ApiV1AuthController {
    private final AuthService authService;

    @PostMapping("/auth/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public RsData<SignupResponse> signup(@RequestBody @Valid SignupRequest request) {
        SignupResponse response = authService.signup(request);

        return new RsData<>("201-1", "회원가입 성공", response);
    }
}
