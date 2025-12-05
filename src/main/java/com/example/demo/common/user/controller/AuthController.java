package com.example.demo.common.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response, Authentication authentication) {
        log.info("=== 로그아웃 요청 ===");
        
        if (authentication != null) {
            log.info("사용자 로그아웃: {}", authentication.getName());
        }
        
        // 프론트에서 localStorage 토큰 삭제하도록 응답
        return ResponseEntity.ok().body(new LogoutResponse("로그아웃 성공"));
    }
    
    static class LogoutResponse {
        public String message;
        
        LogoutResponse(String message) {
            this.message = message;
        }
    }
}