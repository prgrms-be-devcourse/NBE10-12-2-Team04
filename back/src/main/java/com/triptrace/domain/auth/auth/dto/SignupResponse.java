package com.triptrace.domain.auth.auth.dto;

import com.triptrace.domain.member.member.entity.Member;

import java.time.LocalDateTime;

public record SignupResponse(
    Long id,
    String email,
    String username,
    LocalDateTime createdAt
) {
    public SignupResponse(Member member) {
        this(
            member.getId(),
            member.getEmail(),
            member.getUsername(),
            member.getCreatedAt()
        );
    }
}
