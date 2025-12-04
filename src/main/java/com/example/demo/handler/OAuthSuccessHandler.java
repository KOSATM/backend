package com.example.demo.handler;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.demo.common.user.dao.UserDao;
import com.example.demo.common.user.dto.User;
import com.example.demo.common.user.service.JwtTokenProvider;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class OAuthSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    
    private final JwtTokenProvider jwtTokenProvider;
    private final UserDao userDao;
    
    @Value("${app.oauth2.authorized-redirect-uri}")
    private String redirectUri;
    
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        
        log.info("=== OAuthSuccessHandler.onAuthenticationSuccess 호출됨 ===");
        
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");
        
        log.info("Google OAuth 로그인: email={}, name={}", email, name);
        
        // DB에서 사용자 조회
        User user = userDao.selectUserByEmail(email);
        
        if (user == null) {
            log.info("신규 사용자 생성: email={}", email);
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setName(name);
            newUser.setProfileImageUrl(picture);
            newUser.setIsActive(true);
            
            userDao.insertUser(newUser);
            user = userDao.selectUserByEmail(email);
            log.info("신규 사용자 생성 완료: userId={}", user.getId());
        } else {
            log.info("기존 사용자: userId={}", user.getId());
        }
        
        Long userId = user.getId();
        String token = jwtTokenProvider.generateToken(userId);
        
        log.info("JWT 토큰 생성 완료: userId={}", userId);
        
        String redirectUrl = UriComponentsBuilder.fromUriString(redirectUri)
            .queryParam("token", token)
            .queryParam("userId", userId)
            .queryParam("email", email)
            .build()
            .toUriString();
        
        log.info("프론트로 리다이렉트: {}", redirectUrl);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}