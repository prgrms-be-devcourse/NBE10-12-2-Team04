package com.triptrace.domain.member.member.dto;

import jakarta.validation.constraints.Size;

// 내 정보 부분 수정 요청. 넘어오지 않은(null) 필드는 기존 값을 유지한다.
public record MemberModifyRequest(
    @Size(min = 2, max = 50)
    String username,

    @Size(max = 100)
    String intro,

    @Size(max = 500)
    String profileImageUrl
) {
}
