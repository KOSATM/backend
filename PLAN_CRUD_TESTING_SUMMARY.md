# Plan CRUD 테스트 가이드 요약

## 문제 상황
"plan crud가 제대로 작동하고 있는건지 알고 싶어 포스트맨으로 요청 어떻게 보내야 할까?"

## 해결 방안

### 1. 📖 상세한 Postman 테스트 가이드 작성 (POSTMAN_GUIDE.md)
Plan CRUD API의 모든 엔드포인트를 Postman으로 테스트하는 방법을 한글로 상세히 작성했습니다.

**포함된 내용:**
- ✅ **CREATE (생성)**: POST /api/plans - 여행 계획 생성
- ✅ **READ (조회)**: 
  - GET /api/plans/{planId} - 단건 조회
  - GET /api/plans/{planId}/detail - 상세 조회 (Days + Places 포함)
  - GET /api/plans/user/{userId} - 사용자별 목록 조회
- ⚠️ **UPDATE (수정)**: PUT /api/plans/{planId} - 미구현 (501)
- ⚠️ **DELETE (삭제)**: DELETE /api/plans/{planId} - 미구현 (501)

**각 엔드포인트마다 제공된 정보:**
- 요청 URL 및 HTTP 메서드
- 필수/선택 파라미터 설명
- Postman 설정 방법
- 예상 응답 예시 (JSON)
- 에러 케이스 처리

### 2. 🧪 자동화된 통합 테스트 작성 (PlanControllerIntegrationTest.java)
Postman 대신 자동으로 API를 검증할 수 있는 통합 테스트를 작성했습니다.

**테스트 항목 (총 10개):**
1. ✅ Plan 생성 - 모든 파라미터 지정
2. ✅ Plan 생성 - 기본값 사용
3. ✅ Plan 단건 조회 - 성공 케이스
4. ✅ Plan 단건 조회 - 404 Not Found
5. ✅ Plan 상세 조회 - Days와 Places 포함
6. ✅ 사용자별 Plan 목록 조회 - 성공 케이스
7. ✅ 사용자별 Plan 목록 조회 - 빈 목록
8. ⚠️ Plan 수정 - 501 Not Implemented
9. ⚠️ Plan 삭제 - 501 Not Implemented
10. ✅ 다양한 일수로 Plan 생성

**테스트 실행 방법:**
```bash
./gradlew test --tests PlanControllerIntegrationTest
```

## 현재 구현 상태

### ✅ 구현된 기능 (정상 작동)
- **POST /api/plans**: 여행 계획 생성 + 자동으로 Days와 샘플 Places 생성
  - 파라미터: userId (필수), days (기본값 3), budget (기본값 500000), startDate (기본값 오늘)
  - 각 Day마다 2개의 샘플 Place가 자동 생성됨
- **GET /api/plans/{planId}**: Plan 기본 정보 조회
- **GET /api/plans/{planId}/detail**: Plan + Days + Places 전체 조회
- **GET /api/plans/user/{userId}**: 특정 사용자의 모든 Plan 조회

### ⚠️ 미구현된 기능
- **PUT /api/plans/{planId}**: Plan 수정 (501 Not Implemented 반환)
- **DELETE /api/plans/{planId}**: Plan 삭제 (501 Not Implemented 반환)

이 두 기능을 구현하려면:
1. `PlanDao`에 update, delete 메서드 추가
2. `PlanMapper.xml`에 UPDATE, DELETE SQL 작성
3. `PlanService`에 비즈니스 로직 구현
4. `PlanController`의 미구현 부분을 실제 로직으로 교체

## Postman 테스트 시작하기

### 1단계: 서버 실행
```bash
./gradlew bootRun
```

### 2단계: Postman에서 테스트

#### 예시 1: Plan 생성
```
POST http://localhost:8080/api/plans?userId=1&days=3&budget=500000&startDate=2025-12-10
```

**응답 예시:**
```json
{
    "id": 1,
    "userId": 1,
    "budget": 500000,
    "startDate": "2025-12-10",
    "endDate": "2025-12-12",
    "isEnded": false
}
```

#### 예시 2: Plan 상세 조회
```
GET http://localhost:8080/api/plans/1/detail
```

**응답 예시:**
```json
{
    "plan": {
        "id": 1,
        "userId": 1,
        "budget": 500000,
        "startDate": "2025-12-10",
        "endDate": "2025-12-12"
    },
    "days": [
        {
            "day": {
                "id": 1,
                "planId": 1,
                "dayIndex": 1,
                "title": "Day 1",
                "planDate": "2025-12-10"
            },
            "places": [
                {
                    "id": 1,
                    "title": "Morning Activity",
                    "placeName": "Sample Place 1-1",
                    "startAt": "2025-12-10T09:00:00+09:00",
                    "endAt": "2025-12-10T12:00:00+09:00"
                },
                {
                    "id": 2,
                    "title": "Afternoon Activity",
                    "placeName": "Sample Place 1-2",
                    "startAt": "2025-12-10T14:00:00+09:00",
                    "endAt": "2025-12-10T18:00:00+09:00"
                }
            ]
        }
        // ... Day 2, Day 3
    ]
}
```

### 3단계: 전체 시나리오 테스트
1. Plan 생성 → `planId` 확인
2. Plan 기본 정보 조회
3. Plan 상세 정보 조회 (Days + Places)
4. 사용자 Plan 목록 조회
5. 미구현 기능 확인 (UPDATE, DELETE → 501 응답)

## 파일 구조

```
/home/runner/work/backend/backend/
├── POSTMAN_GUIDE.md                          # 📖 Postman 테스트 가이드 (이 파일)
├── PLAN_CRUD_TESTING_SUMMARY.md              # 📄 이 요약 문서
└── src/
    ├── main/java/com/example/demo/planner/plan/
    │   ├── controller/PlanController.java     # 🎯 API 엔드포인트
    │   ├── service/PlanService.java           # 💼 비즈니스 로직
    │   └── dto/entity/Plan.java               # 📦 Plan 엔티티
    └── test/java/com/example/demo/planner/plan/
        └── controller/
            └── PlanControllerIntegrationTest.java  # 🧪 통합 테스트
```

## 다음 단계

### Postman으로 수동 테스트하고 싶다면:
1. `POSTMAN_GUIDE.md` 파일을 열어서 단계별로 따라하세요
2. 각 API 엔드포인트의 요청/응답 예시가 자세히 나와있습니다

### 자동화된 테스트를 실행하고 싶다면:
```bash
./gradlew test --tests PlanControllerIntegrationTest
```

### UPDATE/DELETE 기능을 구현하고 싶다면:
1. `PlanDao.java`에 메서드 추가
2. `PlanMapper.xml`에 SQL 쿼리 추가
3. `PlanService.java`에 로직 구현
4. `PlanController.java`의 미구현 부분 수정

## 문제 해결

### "Connection refused" 에러
→ 서버가 실행 중인지 확인: `./gradlew bootRun`

### "500 Internal Server Error"
→ 데이터베이스 연결 확인 (`application.properties`)

### "404 Not Found" (전체 경로에서)
→ URL에 `/api` prefix가 있는지 확인

---

**작성일**: 2025-12-04
**작성자**: GitHub Copilot Coding Agent
