package com.triptrace.domain.auth.auth.dto;

public record LoginResponse(
    String accessToken,
    String tokenType
) {
    public LoginResponse(String accessToken) {
        this(accessToken, "Bearer");
    }
}
