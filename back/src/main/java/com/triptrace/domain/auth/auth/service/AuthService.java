package com.triptrace.domain.auth.auth.service;

import com.triptrace.domain.auth.auth.dto.SignupRequest;
import com.triptrace.domain.auth.auth.dto.SignupResponse;
import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.member.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 인증 흐름을 조립하는 서비스. (비밀번호 해시, 토큰 발급 등)
 * 회원 데이터의 중복검사·저장은 MemberService에 위임한다.
 */
@Service
@RequiredArgsConstructor
public class AuthService {
    private final MemberService memberService;
    private final PasswordEncoder passwordEncoder;

    // 회원가입: 비밀번호를 해시한 뒤 회원 저장을 MemberService에 위임하고 응답을 만든다.
    public SignupResponse signup(SignupRequest request) {
        String passwordHash = passwordEncoder.encode(request.password());

        Member member = memberService.signup(
            request.email(),
            request.username(),
            passwordHash,
            request.profileImageUrl()
        );

        return new SignupResponse(member);
    }
}
