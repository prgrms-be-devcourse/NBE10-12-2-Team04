package com.triptrace.global.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 인증의 "규칙표". 어떤 경로가 열려 있고 어디부터 로그인이 필요한지 정하고,
 * 우리가 만든 JwtFilter를 보안 필터 체인에 끼워 넣는다.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtProvider jwtProvider;

    // 이 Bean이 반환하는 필터 체인이 곧 앱의 보안 규칙이 된다
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // 토큰 인증이라 CSRF 불필요
            .sessionManagement(session -> // 세션 안 씀: 매 요청 토큰으로만 판단
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(headers ->  // H2 콘솔(iframe) 표시 허용
                headers.frameOptions(frameOptions -> frameOptions.disable()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers( // 비로그인 허용 경로
                    "/api/v1/auth/signup",
                    "/api/v1/auth/login",
                    "/api/v1/auth/reissue",
                    "/h2-console/**",
                    "/swagger-ui/**",
                    "/v3/api-docs/**"
                ).permitAll()
                .anyRequest().authenticated() // 그 외 전부 로그인 필요
            )
            .addFilterBefore(new JwtFilter(jwtProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
