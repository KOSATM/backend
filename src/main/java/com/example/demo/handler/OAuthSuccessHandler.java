package com.example.demo.handler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.demo.common.user.dao.UserDao;
import com.example.demo.common.user.dto.User;
import com.example.demo.common.user.service.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;

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
    private final ObjectMapper objectMapper;
    
    @Value("${app.frontend.uri}")
    private String frontendUri;
    
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        
        log.info("=== OAuthSuccessHandler.onAuthenticationSuccess 호출됨 ===");
        
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        
        // Google에서 받아온 모든 정보
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");
        String givenName = oAuth2User.getAttribute("given_name");
        String familyName = oAuth2User.getAttribute("family_name");
        String locale = oAuth2User.getAttribute("locale");
        Boolean emailVerified = oAuth2User.getAttribute("email_verified");
        String sub = oAuth2User.getAttribute("sub");
        
        log.info("Google OAuth 로그인: email={}, name={}, picture={}", email, name, picture);
        
        // DB에서 사용자 조회
        User user = userDao.selectUserByEmail(email);
        
        if (user == null) {
            // 새 사용자 생성
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
            // 기존 사용자 정보 업데이트
            log.info("기존 사용자: userId={}", user.getId());
            user.setName(name);
            user.setProfileImageUrl(picture);
            userDao.updateUser(user);
        }
        
        Long userId = user.getId();
        String token = jwtTokenProvider.generateToken(userId);
        
        log.info("JWT 토큰 생성 완료: userId={}", userId);
        
        // 프론트에 보낼 정보
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", userId);
        userInfo.put("token", token);
        userInfo.put("email", email);
        userInfo.put("name", name);
        userInfo.put("picture", picture);
        userInfo.put("givenName", givenName);
        userInfo.put("familyName", familyName);
        userInfo.put("locale", locale);
        userInfo.put("emailVerified", emailVerified);
        userInfo.put("oauthId", sub);
        userInfo.put("oauthProvider", "google");
        
        // JSON으로 인코딩
        String userInfoJson = objectMapper.writeValueAsString(userInfo);
        
        // Base64로 인코딩 (한글 포함된 데이터 안전하게 전달)
        String encodedUserInfo = Base64.getEncoder().encodeToString(userInfoJson.getBytes(StandardCharsets.UTF_8));
        
        // 프론트로 리다이렉트
        String redirectUrl = UriComponentsBuilder.fromUriString(frontendUri)
                .queryParam("userInfo", encodedUserInfo)
                .build()
                .toUriString();
        
        log.info("OAuth 성공 - 프론트로 리다이렉트: {}", frontendUri);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}