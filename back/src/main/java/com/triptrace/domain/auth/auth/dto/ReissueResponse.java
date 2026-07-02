package com.triptrace.domain.auth.auth.dto;

public record ReissueResponse(
    String accessToken,
    String tokenType
) {
    public ReissueResponse(String accessToken) {
        this(accessToken, "Bearer");
    }
}
