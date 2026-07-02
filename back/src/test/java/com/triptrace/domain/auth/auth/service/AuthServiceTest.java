package com.triptrace.domain.auth.auth.service;

import com.triptrace.domain.auth.auth.dto.LoginRequest;
import com.triptrace.domain.auth.auth.dto.SignupRequest;
import com.triptrace.domain.auth.auth.dto.TokenPair;
import com.triptrace.domain.auth.auth.entity.RefreshToken;
import com.triptrace.domain.auth.auth.repository.RefreshTokenRepository;
import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.member.member.repository.MemberRepository;
import com.triptrace.global.exception.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class AuthServiceTest {
    @Autowired
    private AuthService authService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("회원가입 시 비밀번호는 해시되어 저장된다.")
    void signup() {
        authService.signup(new SignupRequest("user@test.com", "user", "password1234", "imageUrl"));

        Member member = memberRepository.findByEmail("user@test.com").orElseThrow();
        assertThat(member.getPasswordHash()).isNotEqualTo("password1234");
        assertThat(passwordEncoder.matches("password1234", member.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("로그인 성공 시 AT와 RT가 발급되고 RT가 저장된다.")
    void login() {
        signup("login@test.com", "loginuser", "password1234");

        TokenPair tokens = authService.login(new LoginRequest("login@test.com", "password1234"));

        assertThat(tokens.accessToken()).isNotBlank();
        assertThat(tokens.refreshToken()).isNotBlank();

        RefreshToken saved = refreshTokenRepository.findByToken(tokens.refreshToken()).orElseThrow();
        assertThat(saved.isRevoked()).isFalse();
        assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    @DisplayName("비밀번호가 틀리면 로그인할 수 없다.")
    void loginWrongPassword() {
        signup("wrong@test.com", "wronguser", "password1234");

        assertThatThrownBy(() -> authService.login(new LoginRequest("wrong@test.com", "wrongpassword")))
            .isInstanceOf(ServiceException.class)
            .hasMessage("401-1 : 이메일 또는 비밀번호가 올바르지 않습니다.");
    }

    @Test
    @DisplayName("존재하지 않는 이메일로는 로그인할 수 없다.")
    void loginNoEmail() {
        assertThatThrownBy(() -> authService.login(new LoginRequest("none@test.com", "password1234")))
            .isInstanceOf(ServiceException.class)
            .hasMessage("401-1 : 이메일 또는 비밀번호가 올바르지 않습니다.");
    }

    @Test
    @DisplayName("재발급 시 기존 RT는 폐기되고 새 토큰이 발급된다. (Rotation)")
    void reissue() {
        signup("reissue@test.com", "reissueuser", "password1234");
        TokenPair oldTokens = authService.login(new LoginRequest("reissue@test.com", "password1234"));

        TokenPair newTokens = authService.reissue(oldTokens.refreshToken());

        assertThat(newTokens.refreshToken()).isNotEqualTo(oldTokens.refreshToken());

        RefreshToken oldRt = refreshTokenRepository.findByToken(oldTokens.refreshToken()).orElseThrow();
        RefreshToken newRt = refreshTokenRepository.findByToken(newTokens.refreshToken()).orElseThrow();
        assertThat(oldRt.isRevoked()).isTrue();
        assertThat(newRt.isRevoked()).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 RT로 재발급하면 401 예외가 발생한다.")
    void reissueInvalidToken() {
        assertThatThrownBy(() -> authService.reissue("not-exist-token"))
            .isInstanceOf(ServiceException.class)
            .hasMessage("401-1 : 유효하지 않은 리프레시 토큰입니다.");
    }

    @Test
    @DisplayName("만료된 RT로 재발급하면 401 예외가 발생한다.")
    void reissueExpiredToken() {
        signup("expired@test.com", "expireduser", "password1234");
        Member member = memberRepository.findByEmail("expired@test.com").orElseThrow();
        refreshTokenRepository.save(
            new RefreshToken(member, "expired-token", LocalDateTime.now().minusDays(1))
        );

        assertThatThrownBy(() -> authService.reissue("expired-token"))
            .isInstanceOf(ServiceException.class)
            .hasMessage("401-1 : 만료된 리프레시 토큰입니다.");
    }

    @Test
    @DisplayName("이미 폐기된 RT를 재사용하면 탈취로 감지되어 401 예외가 발생한다.")
    void reissueRevokedToken() {
        signup("theft@test.com", "theftuser", "password1234");
        TokenPair tokens = authService.login(new LoginRequest("theft@test.com", "password1234"));

        // 1회 재발급 → 기존 RT는 폐기 상태가 됨
        authService.reissue(tokens.refreshToken());

        // 폐기된 RT를 다시 사용 → 탈취 감지
        assertThatThrownBy(() -> authService.reissue(tokens.refreshToken()))
            .isInstanceOf(ServiceException.class)
            .hasMessage("401-1 : 토큰이 탈취되었을 가능성이 있습니다. 재로그인 해주세요.");
    }

    @Test
    @DisplayName("로그아웃하면 RT가 폐기된다.")
    void logout() {
        signup("logout@test.com", "logoutuser", "password1234");
        TokenPair tokens = authService.login(new LoginRequest("logout@test.com", "password1234"));

        authService.logout(tokens.refreshToken());

        RefreshToken rt = refreshTokenRepository.findByToken(tokens.refreshToken()).orElseThrow();
        assertThat(rt.isRevoked()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 RT로 로그아웃하면 401 예외가 발생한다.")
    void logoutInvalidToken() {
        assertThatThrownBy(() -> authService.logout("not-exist-token"))
            .isInstanceOf(ServiceException.class)
            .hasMessage("401-1 : 유효하지 않은 리프레시 토큰입니다.");
    }

    private void signup(String email, String username, String password) {
        authService.signup(new SignupRequest(email, username, password, null));
    }
}
