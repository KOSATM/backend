# 중요: ApiSecurityConfig.java 파일 삭제 필수

## 문제
두 개의 SecurityConfig 클래스가 존재하면서 충돌이 발생했습니다:
- `SecurityConfig.java` (기존)
- `ApiSecurityConfig.java` (새로 생성)

## 해결 방법

### 1단계: ApiSecurityConfig.java 삭제
다음 파일을 삭제하세요:
```
src/main/java/com/example/demo/config/ApiSecurityConfig.java
```

**삭제 방법:**
1. VS Code에서 파일 우클릭
2. "Delete" 선택
3. 확인

또는 터미널에서:
```bash
rm src/main/java/com/example/demo/config/ApiSecurityConfig.java
```

### 2단계: SecurityConfig.java 확인
`SecurityConfig.java` 파일이 다음을 포함하는지 확인하세요:

```java
.authorizeHttpRequests(authz -> authz
    .requestMatchers("/api/test/**").permitAll()  // ← 이 줄이 있어야 함
    .requestMatchers("/static/**").permitAll()
    .requestMatchers("/*.html").permitAll()
    .requestMatchers("/test-itinerary.html").permitAll()
    .requestMatchers("/actuator/health").permitAll()
    .anyRequest().authenticated()
)
```

### 3단계: 서버 재시작
```bash
mvn clean spring-boot:run
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
