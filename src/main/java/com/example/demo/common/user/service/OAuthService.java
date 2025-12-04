package com.example.demo.common.user.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.example.demo.common.user.dao.UserDao;
import com.example.demo.common.user.dto.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class OAuthService extends DefaultOAuth2UserService {
    
    private final UserDao userDao;
    
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        log.info("=== OAuth loadUser 호출됨 ===");
        
        try {
            OAuth2User oAuth2User = super.loadUser(userRequest);
            log.info("Google에서 받은 attributes: {}", oAuth2User.getAttributes());
            
            OAuth2User processedUser = processOAuthUser(oAuth2User);
            log.info("processedUser 반환: {}", processedUser.getAttributes());
            
            return processedUser;
        } catch (Exception e) {
            log.error("OAuth 사용자 처리 실패", e);
            throw new OAuth2AuthenticationException("OAuth 사용자 처리 실패: " + e.getMessage());
        }
    }
    
    private OAuth2User processOAuthUser(OAuth2User oAuth2User) {
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");
        
        log.info("OAuth 사용자 처리 시작: email={}, name={}", email, name);
        
        User existingUser = userDao.selectUserByEmail(email);
        
        if (existingUser == null) {
            log.info("신규 사용자 생성: email={}", email);
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setName(name);
            newUser.setProfileImageUrl(picture);
            newUser.setIsActive(true);
            
            userDao.insertUser(newUser);
            existingUser = userDao.selectUserByEmail(email);
            
            if (existingUser == null) {
                throw new RuntimeException("신규 사용자 저장 후 조회 실패: " + email);
            }
            
            log.info("신규 사용자 생성 완료: userId={}", existingUser.getId());
        } else {
            log.info("기존 사용자 발견: userId={}", existingUser.getId());
        }
        
        // 새로운 attributes Map 생성
        Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());
        attributes.put("userId", existingUser.getId());
        
        log.info("userId를 attributes에 추가: userId={}, 최종 attributes: {}", existingUser.getId(), attributes.keySet());
        
        // 새로운 OAuth2User 객체 생성하여 반환
        DefaultOAuth2User newOAuth2User = new DefaultOAuth2User(
            oAuth2User.getAuthorities(),
            attributes,
            "sub"
        );
        
        log.info("DefaultOAuth2User 생성 완료: userId={}", newOAuth2User.getAttributes().get("userId"));
        
        return newOAuth2User;
    }
}