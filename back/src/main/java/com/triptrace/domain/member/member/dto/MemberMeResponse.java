package com.triptrace.domain.member.member.dto;

import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.member.member.entity.MemberStatus;

import java.time.LocalDateTime;

public record MemberMeResponse(
    Long id,
    String email,
    String username,
    String nickname,
    String profileImageUrl,
    MemberStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public MemberMeResponse(Member member) {
        this(
            member.getId(),
            member.getEmail(),
            member.getUsername(),
            member.getUsername(),
            member.getProfileImageUrl(),
            member.getStatus(),
            member.getCreatedAt(),
            member.getUpdatedAt()
        );
    }
}
