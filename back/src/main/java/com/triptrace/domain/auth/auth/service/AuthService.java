package com.triptrace.domain.auth.auth.service;

import com.triptrace.domain.auth.auth.dto.LoginRequest;
import com.triptrace.domain.auth.auth.dto.LoginResult;
import com.triptrace.domain.auth.auth.dto.SignupRequest;
import com.triptrace.domain.auth.auth.dto.SignupResponse;
import com.triptrace.domain.auth.auth.entity.RefreshToken;
import com.triptrace.domain.auth.auth.repository.RefreshTokenRepository;
import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.member.member.service.MemberService;
import com.triptrace.global.exception.ServiceException;
import com.triptrace.global.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 인증 흐름을 조립하는 서비스. (비밀번호 해시/검증, 토큰 발급 등)
 * 회원 데이터의 중복검사·저장·조회는 MemberService에 위임한다.
 */
@Service
@RequiredArgsConstructor
public class AuthService {
    private final MemberService memberService;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${custom.refreshToken.expirationSeconds}")
    private long refreshTokenExpirationSeconds;

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

    // 로그인: 이메일 조회 → 비밀번호 검증 → AT 발급 + RT 발급·저장. RT 문자열은 컨트롤러가 쿠키로 처리한다.
    @Transactional
    public LoginResult login(LoginRequest request) {
        Member member = memberService.findByEmail(request.email());

        if (!passwordEncoder.matches(request.password(), member.getPasswordHash())) {
            throw new ServiceException("401-1", "이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        String accessToken = jwtProvider.generateAccessToken(member.getId(), member.getEmail());
        String refreshToken = jwtProvider.generateRefreshToken();

        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(refreshTokenExpirationSeconds);
        refreshTokenRepository.save(new RefreshToken(member, refreshToken, expiresAt));

        return new LoginResult(accessToken, refreshToken);
    }
}
