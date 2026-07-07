package com.triptrace.domain.member.member.controller;

import com.triptrace.domain.member.member.dto.MemberMeResponse;
import com.triptrace.domain.member.member.dto.MemberModifyRequest;
import com.triptrace.domain.member.member.dto.ProfileImageUploadResponse;
import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.member.member.service.MemberService;
import com.triptrace.domain.member.member.service.ProfileImageStorage;
import com.triptrace.global.rsData.RsData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ApiV1MemberController {
    private final MemberService memberService;
    private final ProfileImageStorage profileImageStorage;

    @GetMapping("/users/me")
    public RsData<MemberMeResponse> getMe(@AuthenticationPrincipal Long memberId) {
        return new RsData<>(
            "200-1",
            "내 회원 정보 조회에 성공했습니다.",
            new MemberMeResponse(memberService.findById(memberId))
        );
    }

    @PatchMapping("/users/me")
    public RsData<MemberMeResponse> modifyMe(
        @AuthenticationPrincipal Long memberId,
        @RequestBody @Valid MemberModifyRequest request
    ) {
        Member member = memberService.modify(memberId, request);

        return new RsData<>(
            "200-1",
            "회원 정보가 수정되었습니다.",
            new MemberMeResponse(member)
        );
    }

    @PostMapping("/profile-images")
    public RsData<ProfileImageUploadResponse> uploadProfileImage(
        @RequestParam("image") MultipartFile image
    ) {
        String profileImageUrl = profileImageStorage.store(image);

        return new RsData<>(
            "201-1",
            "프로필 이미지 업로드에 성공했습니다.",
            new ProfileImageUploadResponse(profileImageUrl)
        );
    }
}
