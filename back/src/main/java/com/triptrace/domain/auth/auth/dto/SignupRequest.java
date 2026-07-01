package com.triptrace.domain.auth.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record SignupRequest(
    @NotBlank
    String email,

    @NotBlank
    String username,

    @NotBlank
    String password,

    String profileImageUrl
) {
}
