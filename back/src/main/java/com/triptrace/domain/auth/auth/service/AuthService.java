package com.triptrace.domain.auth.auth.service;

import com.triptrace.domain.auth.auth.dto.LoginRequest;
import com.triptrace.domain.auth.auth.dto.SignupRequest;
import com.triptrace.domain.auth.auth.dto.SignupResponse;
import com.triptrace.domain.auth.auth.dto.TokenPair;
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
 * 인증 흐름을 조립하는 서비스. (비밀번호 해시/검증, 토큰 발급/재발급 등)
 * 회원 데이터의 중복검사·저장·조회는 MemberService에 위임한다.
 */
@Service
@RequiredArgsConstructor
public class AuthService {
    private final MemberService memberService;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenService refreshTokenService;

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
    public TokenPair login(LoginRequest request) {
        Member member = memberService.findByEmail(request.email());

        if (!passwordEncoder.matches(request.password(), member.getPasswordHash())) {
            throw new ServiceException("401-1", "이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        return issueTokens(member);
    }

    // 재발급(Rotation): RT 검증 → 탈취/만료 확인 → 기존 RT 폐기 → 새 AT/RT 발급.
    @Transactional
    public TokenPair reissue(String refreshToken) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
            .orElseThrow(() -> new ServiceException("401-1", "유효하지 않은 리프레시 토큰입니다."));

        // 탈취 감지: 이미 폐기된 RT가 다시 쓰였다면 해당 회원의 모든 RT를 무효화한다.
        // 아래 예외로 이 재발급 트랜잭션은 롤백되므로, 무효화는 별도 트랜잭션(REQUIRES_NEW)에서 커밋한다.
        if (storedToken.isRevoked()) {
            refreshTokenService.revokeAllByMember(storedToken.getMember());

            throw new ServiceException("401-1", "토큰이 탈취되었을 가능성이 있습니다. 재로그인 해주세요.");
        }

        if (storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ServiceException("401-1", "만료된 리프레시 토큰입니다.");
        }

        // 기존 RT는 삭제하지 않고 폐기 표시만 한 뒤, 새 토큰을 발급한다.
        storedToken.revoke();

        return issueTokens(storedToken.getMember());
    }

    // 로그아웃: 전달받은 RT를 폐기 표시한다. (같은 트랜잭션의 dirty checking으로 UPDATE 반영)
    @Transactional
    public void logout(String refreshToken) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
            .orElseThrow(() -> new ServiceException("401-1", "유효하지 않은 리프레시 토큰입니다."));

        storedToken.revoke();
    }

    // AT 발급 + 새 RT 발급·저장 후 한 쌍으로 반환. (로그인/재발급 공통)
    private TokenPair issueTokens(Member member) {
        String accessToken = jwtProvider.generateAccessToken(member.getId(), member.getEmail());
        String refreshToken = jwtProvider.generateRefreshToken();

        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(refreshTokenExpirationSeconds);
        refreshTokenRepository.save(new RefreshToken(member, refreshToken, expiresAt));

        return new TokenPair(accessToken, refreshToken);
    }
}
