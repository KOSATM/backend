# Postman 테스트 가이드 - Seoul Travel Itinerary API

## 1. Postman Collection 가져오기

### 방법 1: 파일로 Import
1. Postman 열기
2. **File** → **Import** 클릭
3. **Seoul_Travel_Itinerary_API.postman_collection.json** 파일 선택
4. **Import** 버튼 클릭

### 방법 2: 수동 생성
Postman에서 직접 요청을 만들 수도 있습니다.

---

## 2. API 테스트

### 2.1 Health Check (선택사항)
**목적**: 서버가 실행 중인지 확인

```
GET http://localhost:8080/api/test/health
```

**Response:**
```json
{
  "code": 200,
  "message": "Seoul Travel Itinerary Agent is running",
  "data": "OK",
  "success": true
}
```

---

### 2.2 3일 서울 일정 생성

#### 방법 1: GET 방식 (권장)
**Method**: GET  
**URL**: `http://localhost:8080/api/test/create-itinerary?days=3`

**Headers:**
```
X-User-Id: 1
```

**Body**: 없음

#### 방법 2: POST 방식
**Method**: POST  
**URL**: `http://localhost:8080/api/test/create-itinerary`

**Headers:**
```
Content-Type: application/json
X-User-Id: 1
```

**Body (Raw - JSON):**
```json
{
  "days": 3
}
```

**Response Example:**
```json
{
  "code": 200,
  "message": "Seoul travel itinerary created successfully",
  "success": true,
  "data": {
    "message": "✅ Seoul Travel Itinerary Created Successfully!\n\nPlan ID: #123\nDuration: 2024-01-01 ~ 2024-01-03 (3 days)\nBudget: ₩500,000\n\n📅 Daily Itinerary:\n\nDay 1 (2024-01-01):\n  📍 District: Jongno (Subway recommended)\n  10:00 Jongno Downtown Tour\n  11:00 Gyeongbokgung Palace Tour\n  13:00 Lunch - Tteokbokki\n\nDay 2 (2024-01-02):\n  📍 District: Gangnam (Subway recommended)\n  09:00 Gangnam Style Street Tour\n  10:30 Myeongdong Shopping\n  12:30 Lunch - Korean Fusion\n  14:00 Lotte World Tower\n  16:30 Cafe & Rest\n  18:00 Dinner - BBQ Restaurant\n  20:00 Gangnam Nightlife Tour\n\nDay 3 (2024-01-03):\n  📍 District: Hongdae (Subway recommended)\n  10:00 Hongdae Street Art Tour\n  11:30 Indie Museum\n  13:00 Lunch - Bibimbap\n\n📍 Transportation: Subway only\n💡 Tips: Consider subway travel time between activities (usually 15-30 minutes)\n🍽️ Meals are included in daily activity count",
    "userId": 1,
    "timestamp": 1704067200000
  }
}
```

---

### 2.3 5일 서울 일정 생성

**Method**: GET  
**URL**: `http://localhost:8080/api/test/create-itinerary?days=5`

**Headers:**
```
X-User-Id: 1
```

**Body**: 없음

**Response**: 5일 상세 일정이 포함된 응답

---

### 2.4 7일 서울 일정 생성 (최대)

**Method**: GET  
**URL**: `http://localhost:8080/api/test/create-itinerary?days=7`

**Headers:**
```
X-User-Id: 1
```

**Body**: 없음

**Response**: 7일 상세 일정이 포함된 응답

---

## 3. Parameter 설명

### Request Body

| Parameter | Type | Required | Description | Example |
|-----------|------|----------|-------------|---------|
| `days` | Integer | Yes | 여행 일수 (1-7일) | 5 |

### Request Headers

| Header | Value | Description |
|--------|-------|-------------|
| `Content-Type` | application/json | JSON 형식 지정 |
| `X-User-Id` | 1 (또는 사용자 ID) | 사용자 ID |

### Response Body

| Field | Type | Description |
|-------|------|-------------|
| `code` | Integer | HTTP 상태 코드 (200=성공, 400=오류) |
| `message` | String | 응답 메시지 |
| `success` | Boolean | 성공 여부 |
| `data` | Object | 응답 데이터 |
| `data.message` | String | 생성된 일정 상세 내용 |
| `data.userId` | Long | 사용자 ID |
| `data.timestamp` | Long | 요청 시간 (Unix Timestamp) |

---

## 4. 오류 처리

### Invalid Days (1-7 범위 벗어남)
```json
{
  "code": 400,
  "message": "Invalid days. Please provide 1-7 days for Seoul itinerary.",
  "success": false
}
```

### Server Error
```json
{
  "code": 400,
  "message": "Failed to create itinerary: [에러 메시지]",
  "success": false
}
```

---

## 5. 테스트 시나리오

### 시나리오 1: 기본 3일 일정
1. **Request**: `{ "days": 3 }`
2. **Expected**: 3일 일정 생성 (첫날 3-4개, 3일 3-4개 활동)

### 시나리오 2: 5일 일정
1. **Request**: `{ "days": 5 }`
2. **Expected**: 5일 일정 생성 (첫날 3-4개, 2-4일 7-8개, 5일 3-4개 활동)

### 시나리오 3: 최대 7일 일정
1. **Request**: `{ "days": 7 }`
2. **Expected**: 7일 전체 일정 생성

### 시나리오 4: 유효하지 않은 범위
1. **Request**: `{ "days": 0 }` 또는 `{ "days": 8 }`
2. **Expected**: 오류 메시지 반환

### 시나리오 5: 없는 필드
1. **Request**: `{}` (days 필드 없음)
2. **Expected**: null 체크로 오류 메시지 반환

---

## 6. Postman 테스트 팁

### 6.1 변수 설정 (선택사항)
Postman에서 환경 변수를 사용하여 더 쉽게 관리할 수 있습니다:

1. **Environments** → **Create New**
2. 환경명: `Seoul Travel Local`
3. Variables 추가:
   ```
   base_url: http://localhost:8080
   user_id: 1
   ```
4. URL을 `{{base_url}}/api/test/create-itinerary`로 변경
5. Header에서 `{{user_id}}`로 사용

### 6.2 요청 전 확인 사항
- ✅ 서버가 `http://localhost:8080`에서 실행 중
- ✅ `X-User-Id` 헤더 설정
- ✅ Body에 `days` 값이 1-7 범위

### 6.3 응답 검증
- ✅ Status Code: **200**
- ✅ Response Body에 `"success": true`
- ✅ `data.message` 필드 확인

---

## 7. cURL 명령어 예시

### GET 방식 (권장)

#### 3일 일정 생성
```bash
curl -X GET "http://localhost:8080/api/test/create-itinerary?days=3" \
  -H "X-User-Id: 1"
```

#### 5일 일정 생성
```bash
curl -X GET "http://localhost:8080/api/test/create-itinerary?days=5" \
  -H "X-User-Id: 1"
```

#### 7일 일정 생성
```bash
curl -X GET "http://localhost:8080/api/test/create-itinerary?days=7" \
  -H "X-User-Id: 1"
```

### POST 방식 (대체)

#### 3일 일정 생성
```bash
curl -X POST http://localhost:8080/api/test/create-itinerary \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -d '{"days": 3}'
```

#### 5일 일정 생성
```bash
curl -X POST http://localhost:8080/api/test/create-itinerary \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -d '{"days": 5}'
```

#### 7일 일정 생성
```bash
curl -X POST http://localhost:8080/api/test/create-itinerary \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -d '{"days": 7}'
```

### Health Check
```bash
curl http://localhost:8080/api/test/health
```

---

## 8. 응답 예시 (3일 기준)

```
✅ Seoul Travel Itinerary Created Successfully!

Plan ID: #123
Duration: 2024-01-01 ~ 2024-01-03 (3 days)
Budget: ₩500,000

📅 Daily Itinerary:

Day 1 (2024-01-01):
  📍 District: Jongno (Subway recommended)
  10:00 Jongno Downtown Tour
  11:00 Gyeongbokgung Palace Tour
  13:00 Lunch - Tteokbokki

Day 2 (2024-01-02):
  📍 District: Gangnam (Subway recommended)
  09:00 Gangnam Style Street Tour
  10:30 Myeongdong Shopping
  12:30 Lunch - Korean Fusion
  14:00 Lotte World Tower
  16:30 Cafe & Rest
  18:00 Dinner - Korean BBQ
  20:00 Gangnam Night Tour

Day 3 (2024-01-03):
  📍 District: Hongdae (Subway recommended)
  10:00 Hongdae Street Art Tour
  11:30 Indie Museum
  13:00 Lunch - Bibimbap

📍 Transportation: Subway only
💡 Tips: Consider subway travel time between activities (usually 15-30 minutes)
🍽️ Meals are included in daily activity count
```

---

## 9. 문제 해결

| 문제 | 원인 | 해결 |
|------|------|------|
| Connection refused | 서버 미실행 | `mvn spring-boot:run` 실행 |
| 400 Invalid days | days 값이 1-7 범위 벗어남 | 1-7 사이의 값으로 변경 |
| 500 Internal Server Error | 데이터베이스 오류 | PlanService, 데이터베이스 연결 확인 |
| Content-Type 오류 | 헤더 누락 | `application/json` 헤더 추가 |

---

## 10. 파일 위치

**Postman Collection 파일**:  
`c:\Users\cakes\Desktop\KOSA_ATM\backend\Seoul_Travel_Itinerary_API.postman_collection.json`

**Import 방법**:
1. Postman 실행
2. `File` → `Import`
3. 위의 JSON 파일 선택
4. 자동으로 4개의 요청이 생성됨

---
