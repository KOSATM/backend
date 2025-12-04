package com.example.demo.common.user.service;

import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class JwtTokenProvider {
    
    @Value("${jwt.secret}")
    private String secretKeyString;
    
    @Value("${jwt.expiration}")
    private long expirationTime;
    
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKeyString.getBytes());
    }
    
    // JWT 토큰 생성
    public String generateToken(Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime);
        
        return Jwts.builder()
            .setSubject(userId.toString())
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)  // HS512 → HS256
            .compact();
    }
    
    // JWT 토큰에서 userId 추출
    public Long getUserIdFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
            
            return Long.parseLong(claims.getSubject());
        } catch (Exception e) {
            log.error("JWT 토큰 파싱 실패", e);
            return null;
        }
    }
    
    // JWT 토큰 유효성 검증
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            log.error("JWT 토큰 검증 실패: {}", e.getMessage());
            return false;
        }
    }
}