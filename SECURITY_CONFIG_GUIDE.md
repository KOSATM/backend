# SecurityConfig 수정 가이드

## 문제
기존 프로젝트의 SecurityConfig가 있어서 `/api/test/**` 엔드포인트가 차단되고 있습니다.

## 해결 방법

### 1단계: 기존 SecurityConfig 찾기
다음 경로 중 하나에 있을 것입니다:
- `src/main/java/com/example/demo/config/SecurityConfig.java`
- `src/main/java/com/example/demo/handler/SecurityConfig.java`

### 2단계: 수정할 코드

기존 SecurityConfig에서 `authorizeHttpRequests` 섹션을 찾아서 다음처럼 수정하세요:

**수정 전:**
```java
.authorizeHttpRequests(authz -> authz
    .anyRequest().authenticated()
)
```

**수정 후:**
```java
.authorizeHttpRequests(authz -> authz
    .requestMatchers("/api/test/**").permitAll()           // ← 이 줄 추가
    .requestMatchers("/actuator/health").permitAll()
    .requestMatchers("/static/**").permitAll()
    .requestMatchers("/*.html").permitAll()
    .anyRequest().authenticated()
)
```

### 3단계: CSRF 비활성화 (필요시)

```java
.csrf(csrf -> csrf.disable())
```

### 4단계: 서버 재시작

```bash
# Ctrl+C로 종료
mvn clean
mvn spring-boot:run
```

---

## 만약 SecurityConfig가 없다면

새로운 파일 생성: `src/main/java/com/example/demo/config/ApiSecurityConfig.java`

```java
package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class ApiSecurityConfig {

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
            .httpBasic(basic -> basic.disable())
            .oauth2Login(oauth2 -> {
                // OAuth2 설정 유지
            });

        return http.build();
    }
}
```

---

## 테스트

```bash
# Health Check
curl http://localhost:8080/api/test/health

# 5일 일정
curl "http://localhost:8080/api/test/create-itinerary?days=5" \
  -H "X-User-Id: 1"
```

---
