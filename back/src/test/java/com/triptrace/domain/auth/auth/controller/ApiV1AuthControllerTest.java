package com.triptrace.domain.auth.auth.controller;

import com.triptrace.domain.auth.auth.dto.SignupRequest;
import com.triptrace.domain.auth.auth.service.AuthService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiV1AuthControllerTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private AuthService authService;

    @Test
    @DisplayName("회원가입 API - 성공 시 201")
    void signup() throws Exception {
        mvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "user@test.com",
                      "username": "user",
                      "password": "password1234",
                      "profileImageUrl": "imageUrl"
                    }
                    """))
            .andDo(print())
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.resultCode").value("201-1"))
            .andExpect(jsonPath("$.data.email").value("user@test.com"))
            .andExpect(jsonPath("$.data.username").value("user"));
    }

    @Test
    @DisplayName("회원가입 API - 이메일 중복 시 409")
    void signupDuplicateEmail() throws Exception {
        authService.signup(new SignupRequest("dup@test.com", "user1", "password1234", null));

        mvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "dup@test.com",
                      "username": "user2",
                      "password": "password1234"
                    }
                    """))
            .andDo(print())
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.resultCode").value("409-1"));
    }

    @Test
    @DisplayName("회원가입 API - 필수값 누락 시 400")
    void signupValidationFail() throws Exception {
        mvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "",
                      "username": "user",
                      "password": "password1234"
                    }
                    """))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.resultCode").value("400-1"));
    }

    @Test
    @DisplayName("로그인 API - 성공 시 200 + RT 쿠키")
    void login() throws Exception {
        authService.signup(new SignupRequest("login@test.com", "loginuser", "password1234", null));

        mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "login@test.com",
                      "password": "password1234"
                    }
                    """))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.data.accessToken").exists())
            .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refreshToken=")))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")));
    }

    @Test
    @DisplayName("로그인 API - 비밀번호 불일치 시 401")
    void loginWrongPassword() throws Exception {
        authService.signup(new SignupRequest("wrong@test.com", "wronguser", "password1234", null));

        mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "wrong@test.com",
                      "password": "wrongpassword"
                    }
                    """))
            .andDo(print())
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.resultCode").value("401-1"));
    }

    @Test
    @DisplayName("토큰 재발급 API - 성공 시 200 + 새 AT")
    void reissue() throws Exception {
        authService.signup(new SignupRequest("reissue@test.com", "reissueuser", "password1234", null));
        String refreshToken = loginAndGetRefreshToken("reissue@test.com", "password1234");

        mvc.perform(post("/api/v1/auth/reissue")
                .cookie(new Cookie("refreshToken", refreshToken)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.data.accessToken").exists());
    }

    @Test
    @DisplayName("로그아웃 API - 성공 시 200 + 쿠키 만료")
    void logout() throws Exception {
        authService.signup(new SignupRequest("logout@test.com", "logoutuser", "password1234", null));
        String refreshToken = loginAndGetRefreshToken("logout@test.com", "password1234");

        mvc.perform(post("/api/v1/auth/logout")
                .cookie(new Cookie("refreshToken", refreshToken)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));
    }

    private String loginAndGetRefreshToken(String email, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "%s",
                      "password": "%s"
                    }
                    """.formatted(email, password)))
            .andExpect(status().isOk())
            .andReturn();

        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        return setCookie.split(";", 2)[0].substring("refreshToken=".length());
    }
}
