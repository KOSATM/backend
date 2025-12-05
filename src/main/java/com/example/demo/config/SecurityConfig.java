package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import com.example.demo.common.user.service.OAuthService;
import com.example.demo.handler.OAuthSuccessHandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Spring Security 설정
 * OAuth2 로그인 및 로그아웃 기능을 담당합니다.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final OAuthService oAuthService;
    private final OAuthSuccessHandler oAuthSuccessHandler;

    /**
     * Spring Security 필터 체인 설정
     * - CSRF 및 CORS 비활성화
     * - OAuth2 로그인 설정
     * - 로그아웃 처리 설정
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("SecurityConfig 초기화됨");

        http
                // CSRF 공격 방지 비활성화 (API 서버용)
                .csrf(csrf -> csrf.disable())
                // CORS 설정 비활성화
                .cors(cors -> cors.disable())
                // 요청별 권한 설정
                .authorizeHttpRequests(authz -> authz
                        // 공개 엔드포인트
                        .requestMatchers("/", "/login/**", "/oauth2/**", "/api/auth/**").permitAll()
                        .requestMatchers("/users", "/api/users/**").permitAll()
                        // 나머지는 모두 허용 (실제로는 인증 필요)
                        .anyRequest().permitAll())
                // Google OAuth2 로그인 설정
                .oauth2Login(oauth2 -> oauth2
                        // OAuth 사용자 정보 로드 서비스
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(oAuthService))
                        // 로그인 성공 핸들러
                        .successHandler(oAuthSuccessHandler))
                // 로그아웃 설정
                .logout(logout -> logout
                        // 로그아웃 URL
                        .logoutUrl("/api/auth/logout")
                        // 로그아웃 성공 후 리다이렉트 URL
                        .logoutSuccessUrl("http://localhost:80")
                        // HTTP 세션 무효화
                        .invalidateHttpSession(true)
                        // 인증 정보 제거
                        .clearAuthentication(true)
                        // 쿠키 삭제
                        .deleteCookies("JSESSIONID")
                );

        return http.build();
    }

}