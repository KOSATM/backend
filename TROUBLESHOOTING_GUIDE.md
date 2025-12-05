# Seoul Travel Itinerary API - 문제 해결 가이드

## 문제: 500 Internal Server Error 또는 Spring Security 관련 오류

### 원인
1. **Spring Security 설정** - `/api/test/create-itinerary` 엔드포인트가 보안으로 인해 차단됨
2. **CSRF 토큰** - POST/GET 요청이 CSRF 검증 실패
3. **인증 부족** - 엔드포인트 접근 권한 없음

### 해결 방법

#### 1단계: SecurityConfig 확인

**기존 SecurityConfig 파일이 있는 경우** (`com/example/demo/config/SecurityConfig.java`):

```java
// 이 부분을 추가하세요:
.authorizeHttpRequests(authz -> authz
    .requestMatchers("/api/test/**").permitAll()  // ← 이 줄 추가
    .requestMatchers("/actuator/health").permitAll()
    .requestMatchers("/static/**").permitAll()
    .anyRequest().authenticated()
)
.csrf(csrf -> csrf.disable())
```

#### 2단계: 애플리케이션 재시작

```bash
# 터미널에서 Ctrl+C로 현재 프로세스 중지
mvn spring-boot:run
```

#### 3단계: 다시 테스트

```bash
# GET 요청 테스트
curl -X GET "http://localhost:8080/api/test/create-itinerary?days=5" \
  -H "X-User-Id: 1"
```

---

## 빠른 해결책

### Option 1: SecurityConfig 파일 생성 (신규)

파일: `com/example/demo/config/SecurityConfig.java`

```java
package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/test/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/static/**").permitAll()
                .requestMatchers("/*.html").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable());

        return http.build();
    }
}
```

### Option 2: 기존 SecurityConfig 수정

기존 파일에서 다음을 찾아 수정:

```java
// 수정 전
authorizeHttpRequests(authz -> authz
    .anyRequest().authenticated()
)

// 수정 후
authorizeHttpRequests(authz -> authz
    .requestMatchers("/api/test/**").permitAll()  // ← 추가
    .anyRequest().authenticated()
)
```

---

## Postman 테스트

### Health Check (문제 진단)

```
GET http://localhost:8080/api/test/health
```

**성공 응답:**
```json
{
  "code": 200,
  "message": "Seoul Travel Itinerary Agent is running",
  "data": "OK",
  "success": true
}
```

### 3일 일정 생성

```
GET http://localhost:8080/api/test/create-itinerary?days=3
Header: X-User-Id: 1
```

---

## 일반적인 오류 메시지

| 오류 | 해결책 |
|------|--------|
| 403 Forbidden | SecurityConfig에서 `/api/test/**` 허용 |
| 405 Method Not Allowed | GET/POST 메서드 지원 확인 |
| 500 Internal Server Error | 로그 확인, SecurityConfig 수정 |
| CSRF token is missing | CSRF 비활성화 (`csrf.disable()`) |

---

## 로그 확인 방법

Spring Boot 실행 시 다음 로그를 확인하세요:

```
SecurityConfig 로드됨:
2024-XX-XX XX:XX:XX - SecurityFilterChain initialized successfully

또는 오류:
2024-XX-XX XX:XX:XX - Error in SecurityFilterChain
```

---

## 완전한 테스트 절차

1. **서버 중지**: `Ctrl+C`
2. **SecurityConfig 확인/생성**
3. **서버 재시작**: `mvn spring-boot:run`
4. **Health Check**: `GET /api/test/health`
5. **Itinerary 생성**: `GET /api/test/create-itinerary?days=5`

---

## 추가 도움

문제 해결이 안 될 경우:

1. **로그 전체 확인**: `SecurityFilterChain`, `SecurityConfig` 검색
2. **포트 확인**: 8080이 사용 중이 아닌지 확인
3. **Spring 버전**: `pom.xml`에서 Spring Security 버전 확인
4. **캐시 삭제**: `mvn clean` 후 재빌드

---
