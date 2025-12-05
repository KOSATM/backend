# Seoul Travel Itinerary Agent - 설치 및 실행 가이드

## 생성된 파일 목록

### 1. Controller
- **경로**: `com/example/demo/common/test/controller/TestController.java`
- **역할**: Seoul travel itinerary 요청을 처리하는 REST API 엔드포인트 제공
- **주요 메서드**:
  - `POST /api/test/chat` - 여행 요청 처리
  - `POST /api/test/create-itinerary` - 일정 생성
  - `GET /api/test/health` - 상태 확인

### 2. Service
- **경로**: `com/example/demo/common/test/service/TestService.java`
- **역할**: 비즈니스 로직 처리
- **주요 기능**:
  - Chat 요청 처리
  - Seoul 여행 일정 생성

### 3. DTOs
- **ChatRequest.java**: 클라이언트로부터 받는 요청 데이터
  ```java
  - message: String (여행 요청 메시지)
  - userId: Long (사용자 ID)
  ```

- **ChatResponse.java**: 클라이언트로 보내는 응답 데이터
  ```java
  - message: String (AI 응답)
  - userId: Long (사용자 ID)
  - timestamp: Long (요청 시간)
  ```

### 4. Global Response Wrapper
- **경로**: `com/example/demo/common/global/response/ResponseWrapper.java`
- **역할**: 표준화된 API 응답 형식 제공
- **구조**:
  ```java
  {
    "code": 200,
    "message": "Success message",
    "data": { ... },
    "success": true
  }
  ```

### 5. Test HTML Page
- **경로**: `static/test-itinerary.html`
- **역할**: 웹 UI를 통한 API 테스트
- **기능**:
  - 여행 요청 입력
  - Quick Preset 버튼 (3일, 5일, 7일)
  - 실시간 응답 표시
  - 반응형 디자인

## 기존 파일 수정

### TestAgent.java
- Seoul 외국인 여행객 전용 일정 생성 AI 에이전트
- 일정 생성 규칙:
  - 최대 7일
  - 서울 내만
  - 첫/마지막 날: 3-4개 활동
  - 중간 날짜: 7-8개 활동
  - 지하철 교통편
  - 식사 포함
  - 같은 지역 활동 그룹핑

## API 엔드포인트

### 1. Chat 요청
```bash
POST /api/test/chat
Content-Type: application/json
X-User-Id: 1

{
  "message": "I want a 5-day Seoul itinerary",
  "userId": 1
}
```

**Response:**
```json
{
  "code": 200,
  "message": "Chat response generated successfully",
  "success": true,
  "data": {
    "message": "✅ Seoul Travel Itinerary Created Successfully!...",
    "userId": 1,
    "timestamp": 1234567890
  }
}
```

### 2. 일정 생성 요청
```bash
POST /api/test/create-itinerary
Content-Type: application/json
X-User-Id: 1

{
  "message": "Create a 5-day Seoul travel plan",
  "userId": 1
}
```

### 3. 상태 확인
```bash
GET /api/test/health
```

## 실행 방법

### 1. 애플리케이션 시작
```bash
mvn spring-boot:run
```

### 2. 웹 UI 접속
```
http://localhost:8080/test-itinerary.html
```

### 3. 직접 API 호출 (cURL)
```bash
curl -X POST http://localhost:8080/api/test/chat \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -d '{"message":"I want a 5-day Seoul itinerary"}'
```

## 필요한 환경 설정

### application.properties
다음 설정이 이미 추가되었습니다:
```properties
spring.ai.openai.api-key=${OPENAI_API_KEY}
spring.ai.openai.chat.options.model=gpt-4o-mini
```

**환경변수 설정**:
```bash
export OPENAI_API_KEY=your-api-key-here
```

## 폴더 구조

```
backend/src/main/
├── java/com/example/demo/common/
│   ├── test/
│   │   ├── agent/
│   │   │   └── TestAgent.java (수정됨)
│   │   ├── controller/
│   │   │   └── TestController.java (생성)
│   │   ├── service/
│   │   │   └── TestService.java (생성)
│   │   └── dto/
│   │       ├── ChatRequest.java (생성)
│   │       └── ChatResponse.java (생성)
│   └── global/
│       └── response/
│           └── ResponseWrapper.java (생성)
└── resources/
    ├── static/
    │   └── test-itinerary.html (생성)
    └── application.properties (수정됨)
```

## 테스트 시나리오

### 예제 1: 5일 서울 일정
```json
{
  "message": "I want a 5-day Seoul itinerary",
  "userId": 1
}
```

**응답 형식**:
```
✅ Seoul Travel Itinerary Created Successfully!

Plan ID: #123
Duration: 2024-01-01 ~ 2024-01-05 (5 days)
Budget: ₩500,000

📅 Daily Itinerary:

Day 1 (2024-01-01):
  📍 District: Jongno (Subway recommended)
  10:00 Jongno Downtown Tour
  11:00 Gyeongbokgung Palace Tour
  13:00 Lunch - Tteokbokki
...
```

## 주요 기능

1. **AI 기반 일정 생성**
   - OpenAI API를 활용한 자연스러운 응답
   - 서울 여행 규칙 자동 적용

2. **사용자별 관리**
   - userId 기반 요청 처리
   - 각 사용자 데이터 분리

3. **다양한 지역**
   - Jongno, Gangnam, Hongdae, Insadong, Myeongdong
   - 각 지역별 대표 활동 제시

4. **시간 기반 일정**
   - HH:MM 형식의 정확한 시간 표시
   - 이동 시간 고려

## 문제 해결

### OpenAI API Key 오류
```
Error: Invalid API Key
```
→ `application.properties`에서 `OPENAI_API_KEY` 확인

### CORS 오류
→ `GlobalCorsConfig.java` 확인

### Plan Service 오류
→ PlanService가 정상 작동하는지 확인

## 확장 기능

향후 추가 가능한 기능:
- 숙소 예약 연동
- 식당 추천
- 날씨 정보 통합
- 비용 계산 상세화
- 사진 갤러리 추천
