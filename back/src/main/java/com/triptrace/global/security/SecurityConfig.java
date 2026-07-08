package com.triptrace.global.security;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.RequiredArgsConstructor;

/**
 * 인증의 "규칙표". 어떤 경로가 열려 있고 어디부터 로그인이 필요한지 정하고,
 * 우리가 만든 JwtFilter를 보안 필터 체인에 끼워 넣는다.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtProvider jwtProvider;
    @Value("${custom.cors.allowed-origins}")
    List<String> allowedOrigins;

    // 이 Bean이 반환하는 필터 체인이 곧 앱의 보안 규칙이 된다
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
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
                    "/api/v1/auth/logout",
                    "/api/v1/profile-images",
                    "/h2-console/**",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/images/**"
                ).permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/trips").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/trips/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/trips/*/posts").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/posts/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/feed/trips/**").permitAll()
                .anyRequest().authenticated() // 그 외 전부 로그인 필요
            )
            .addFilterBefore(new JwtFilter(jwtProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // 비밀번호 해시/검증에 쓰는 인코더. AuthService가 회원가입 해시·로그인 검증에 주입해 사용한다.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
