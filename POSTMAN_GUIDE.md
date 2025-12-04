# Plan CRUD API - Postman 테스트 가이드

이 문서는 Plan CRUD API를 Postman으로 테스트하는 방법을 설명합니다.

## 서버 정보
- **Base URL**: `http://localhost:8080`
- **API Prefix**: `/api/plans`

## 사전 준비
1. PostgreSQL 데이터베이스가 실행 중이고 연결 가능한지 확인합니다
   - `application.properties`에 설정된 데이터베이스에 연결할 수 있어야 합니다
   - 필요한 테이블들(plan, plan_day, plan_place)이 생성되어 있어야 합니다
2. 환경 변수가 설정되어 있는지 확인합니다
   - `OPENAI_API_KEY`: OpenAI API 키 (필요시)
   - `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`: AWS 자격증명 (필요시)
3. 애플리케이션을 실행합니다
   ```bash
   ./gradlew bootRun
   ```
   또는 IDE에서 `DemoApplication` 클래스를 실행합니다
4. 서버가 8080 포트에서 정상적으로 실행되었는지 로그를 확인합니다

---

## 1. Plan 생성 (CREATE) ✅ 구현됨

여행 계획을 생성하고 샘플 데이터(Days와 Places)를 자동으로 생성합니다.

### 요청
```
POST http://localhost:8080/api/plans?userId=1&days=3&budget=500000&startDate=2025-12-10
```

### Query Parameters
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| userId | Long | ✅ 필수 | - | 사용자 ID |
| days | Integer | 선택 | 3 | 여행 일수 |
| budget | BigDecimal | 선택 | 500000 | 예산 |
| startDate | LocalDate | 선택 | 오늘 | 여행 시작일 (YYYY-MM-DD) |

### Postman 설정
- **Method**: POST
- **URL**: `http://localhost:8080/api/plans`
- **Params 탭**:
  - `userId` = `1`
  - `days` = `3`
  - `budget` = `500000`
  - `startDate` = `2025-12-10`

### 응답 예시 (201 Created)
```json
{
    "id": 1,
    "userId": 1,
    "budget": 500000,
    "startDate": "2025-12-10",
    "endDate": "2025-12-12",
    "createdAt": "2025-12-04T00:00:00Z",
    "updatedAt": null,
    "isEnded": false,
    "title": null
}
```

---

## 2. Plan 단건 조회 (READ) ✅ 구현됨

특정 Plan의 기본 정보를 조회합니다.

### 요청
```
GET http://localhost:8080/api/plans/{planId}
```

### Path Variable
- `planId`: 조회할 Plan의 ID (예: 1)

### Postman 설정
- **Method**: GET
- **URL**: `http://localhost:8080/api/plans/1`

### 응답 예시 (200 OK)
```json
{
    "id": 1,
    "userId": 1,
    "budget": 500000,
    "startDate": "2025-12-10",
    "endDate": "2025-12-12",
    "createdAt": "2025-12-04T00:00:00Z",
    "updatedAt": null,
    "isEnded": false,
    "title": null
}
```

### 응답 (404 Not Found)
Plan이 존재하지 않는 경우 404 상태 코드와 빈 응답을 반환합니다.

---

## 3. Plan 상세 조회 (READ) ✅ 구현됨

Plan과 함께 연관된 모든 Days 및 Places 정보를 조회합니다.

### 요청
```
GET http://localhost:8080/api/plans/{planId}/detail
```

### Path Variable
- `planId`: 조회할 Plan의 ID (예: 1)

### Postman 설정
- **Method**: GET
- **URL**: `http://localhost:8080/api/plans/1/detail`

### 응답 예시 (200 OK)
```json
{
    "plan": {
        "id": 1,
        "userId": 1,
        "budget": 500000,
        "startDate": "2025-12-10",
        "endDate": "2025-12-12",
        "createdAt": "2025-12-04T00:00:00Z",
        "updatedAt": null,
        "isEnded": false,
        "title": null
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
                    "dayId": 1,
                    "title": "Morning Activity",
                    "placeName": "Sample Place 1-1",
                    "address": "Seoul, South Korea",
                    "lat": 37.5665,
                    "lng": 126.9780,
                    "startAt": "2025-12-10T09:00:00+09:00",
                    "endAt": "2025-12-10T12:00:00+09:00",
                    "expectedCost": 20000
                },
                {
                    "id": 2,
                    "dayId": 1,
                    "title": "Afternoon Activity",
                    "placeName": "Sample Place 1-2",
                    "address": "Seoul, South Korea",
                    "lat": 37.4979,
                    "lng": 127.0276,
                    "startAt": "2025-12-10T14:00:00+09:00",
                    "endAt": "2025-12-10T18:00:00+09:00",
                    "expectedCost": 30000
                }
            ]
        },
        {
            "day": {
                "id": 2,
                "planId": 1,
                "dayIndex": 2,
                "title": "Day 2",
                "planDate": "2025-12-11"
            },
            "places": [
                {
                    "id": 3,
                    "dayId": 2,
                    "title": "Morning Activity",
                    "placeName": "Sample Place 2-1",
                    "address": "Seoul, South Korea",
                    "lat": 37.5665,
                    "lng": 126.9780,
                    "startAt": "2025-12-11T09:00:00+09:00",
                    "endAt": "2025-12-11T12:00:00+09:00",
                    "expectedCost": 20000
                },
                {
                    "id": 4,
                    "dayId": 2,
                    "title": "Afternoon Activity",
                    "placeName": "Sample Place 2-2",
                    "address": "Seoul, South Korea",
                    "lat": 37.4979,
                    "lng": 127.0276,
                    "startAt": "2025-12-11T14:00:00+09:00",
                    "endAt": "2025-12-11T18:00:00+09:00",
                    "expectedCost": 30000
                }
            ]
        },
        {
            "day": {
                "id": 3,
                "planId": 1,
                "dayIndex": 3,
                "title": "Day 3",
                "planDate": "2025-12-12"
            },
            "places": [
                {
                    "id": 5,
                    "dayId": 3,
                    "title": "Morning Activity",
                    "placeName": "Sample Place 3-1",
                    "address": "Seoul, South Korea",
                    "lat": 37.5665,
                    "lng": 126.9780,
                    "startAt": "2025-12-12T09:00:00+09:00",
                    "endAt": "2025-12-12T12:00:00+09:00",
                    "expectedCost": 20000
                },
                {
                    "id": 6,
                    "dayId": 3,
                    "title": "Afternoon Activity",
                    "placeName": "Sample Place 3-2",
                    "address": "Seoul, South Korea",
                    "lat": 37.4979,
                    "lng": 127.0276,
                    "startAt": "2025-12-12T14:00:00+09:00",
                    "endAt": "2025-12-12T18:00:00+09:00",
                    "expectedCost": 30000
                }
            ]
        }
    ]
}
```

---

## 4. 사용자별 Plan 목록 조회 (READ) ✅ 구현됨

특정 사용자의 모든 Plan 목록을 조회합니다.

### 요청
```
GET http://localhost:8080/api/plans/user/{userId}
```

### Path Variable
- `userId`: 사용자 ID (예: 1)

### Postman 설정
- **Method**: GET
- **URL**: `http://localhost:8080/api/plans/user/1`

### 응답 예시 (200 OK)
```json
[
    {
        "id": 1,
        "userId": 1,
        "budget": 500000,
        "startDate": "2025-12-10",
        "endDate": "2025-12-12",
        "createdAt": "2025-12-04T00:00:00Z",
        "updatedAt": null,
        "isEnded": false,
        "title": null
    },
    {
        "id": 2,
        "userId": 1,
        "budget": 1000000,
        "startDate": "2025-12-20",
        "endDate": "2025-12-24",
        "createdAt": "2025-12-04T01:00:00Z",
        "updatedAt": null,
        "isEnded": false,
        "title": null
    }
]
```

---

## 5. Plan 수정 (UPDATE) ⚠️ 미구현

현재 구현되지 않았습니다. 호출 시 501 Not Implemented를 반환합니다.

### 요청
```
PUT http://localhost:8080/api/plans/{planId}
```

### Postman 설정
- **Method**: PUT
- **URL**: `http://localhost:8080/api/plans/1`
- **Headers**:
  - `Content-Type`: `application/json`
- **Body (raw, JSON)**:
```json
{
    "budget": 600000,
    "startDate": "2025-12-15",
    "endDate": "2025-12-17",
    "isEnded": false,
    "title": "Updated Title"
}
```

### 응답 (501 Not Implemented)
```
HTTP/1.1 501 Not Implemented
```

---

## 6. Plan 삭제 (DELETE) ⚠️ 미구현

현재 구현되지 않았습니다. 호출 시 501 Not Implemented를 반환합니다.

### 요청
```
DELETE http://localhost:8080/api/plans/{planId}
```

### Postman 설정
- **Method**: DELETE
- **URL**: `http://localhost:8080/api/plans/1`

### 응답 (501 Not Implemented)
```
HTTP/1.1 501 Not Implemented
```

---

## 테스트 시나리오

다음 순서대로 테스트하면 Plan CRUD의 작동을 확인할 수 있습니다:

### 시나리오 1: 기본 CRUD 테스트
1. **Plan 생성**: `POST /api/plans?userId=1&days=3`
   - 응답에서 생성된 `planId`를 메모합니다 (예: `1`)
2. **Plan 조회**: `GET /api/plans/1`
   - Plan 기본 정보가 정상적으로 반환되는지 확인
3. **Plan 상세 조회**: `GET /api/plans/1/detail`
   - Plan, Days, Places가 모두 포함되어 반환되는지 확인
   - 3일치 데이터와 각 날짜마다 2개의 Place가 있는지 확인
4. **사용자 Plan 목록**: `GET /api/plans/user/1`
   - 해당 사용자의 모든 Plan 목록이 반환되는지 확인

### 시나리오 2: 다양한 파라미터 테스트
1. **최소 파라미터로 생성**: `POST /api/plans?userId=2`
   - 기본값(3일, 50만원, 오늘 날짜)으로 생성되는지 확인
2. **모든 파라미터 지정**: `POST /api/plans?userId=3&days=5&budget=1000000&startDate=2025-12-20`
   - 지정한 값대로 생성되는지 확인
3. **긴 여행 생성**: `POST /api/plans?userId=1&days=7`
   - 7일치 데이터가 모두 생성되는지 확인

### 시나리오 3: 에러 케이스 테스트
1. **존재하지 않는 Plan 조회**: `GET /api/plans/99999`
   - 404 Not Found 반환 확인
2. **Plan 없는 사용자 조회**: `GET /api/plans/user/99999`
   - 빈 배열 `[]` 반환 확인
3. **미구현 기능 호출**: `PUT /api/plans/1`, `DELETE /api/plans/1`
   - 501 Not Implemented 반환 확인

---

## Postman Collection 생성하기

Postman에서 Collection을 만들어 모든 요청을 저장해두면 편리합니다:

1. Postman에서 **New Collection** 클릭
2. Collection 이름: `Plan CRUD API`
3. 각 API를 폴더로 구분:
   - **Create** 폴더
   - **Read** 폴더
   - **Update** 폴더 (미구현)
   - **Delete** 폴더 (미구현)

### Environment 변수 설정
반복되는 값을 변수로 관리하면 편리합니다:

1. Postman에서 **Environments** 클릭
2. **New Environment** 생성
3. 변수 추가:
   - `baseUrl`: `http://localhost:8080`
   - `userId`: `1`
   - `planId`: `1` (테스트 후 업데이트)

사용 예시:
```
{{baseUrl}}/api/plans?userId={{userId}}
```

---

## 로그 확인

API 호출 시 서버 콘솔에서 다음과 같은 로그를 확인할 수 있습니다:

```
INFO  PlanController.createPlan(): 여행 계획 생성 요청: userId=1, days=3
INFO  PlanService.createPlanWithSampleData(): 샘플 데이터 포함 여행 계획 생성 시작: userId=1, days=3
INFO  PlanService.createPlanWithSampleData(): Plan 생성 완료: planId=1
DEBUG PlanService.createPlanWithSampleData(): PlanDay 생성 완료: dayId=1, dayIndex=1
DEBUG PlanService.createPlanWithSampleData(): PlanPlace 2개 생성 완료: dayId=1
...
INFO  PlanService.createPlanWithSampleData(): 샘플 데이터 포함 여행 계획 생성 완료: planId=1, 총 3일, 6개 장소
INFO  PlanController.createPlan(): 여행 계획 생성 완료: planId=1
```

---

## 문제 해결

### 1. 연결 거부 오류
```
Could not connect to localhost:8080
```
**해결방법**: 애플리케이션이 실행 중인지 확인하세요.

### 2. 500 Internal Server Error
```
HTTP/1.1 500 Internal Server Error
```
**해결방법**: 
- PostgreSQL 데이터베이스 연결 확인
- 서버 콘솔의 에러 로그 확인
- 테이블이 존재하는지 확인 (plan, plan_day, plan_place)

### 3. 404 Not Found (전체 경로)
```
GET http://localhost:8080/plans/1 → 404
```
**해결방법**: `/api` prefix를 빼먹었을 수 있습니다. `/api/plans/1`로 수정하세요.

---

## 다음 단계

현재 구현되지 않은 기능들:
- ⚠️ **Plan 수정 (UPDATE)**: PlanService에 update 메서드 추가 필요
- ⚠️ **Plan 삭제 (DELETE)**: PlanService에 delete 메서드 추가 필요

이 기능들을 구현하려면 다음이 필요합니다:
1. `PlanDao`에 `updatePlan()`, `deletePlan()` 메서드 추가
2. `PlanMapper.xml`에 UPDATE, DELETE SQL 추가
3. `PlanService`에 비즈니스 로직 구현
4. `PlanController`의 NOT_IMPLEMENTED 부분을 실제 구현으로 변경

---

## 자동화된 테스트

Postman으로 수동 테스트하는 대신, 자동화된 통합 테스트를 실행할 수도 있습니다:

```bash
./gradlew test --tests PlanControllerIntegrationTest
```

통합 테스트 파일은 다음 위치에 있습니다:
- `/src/test/java/com/example/demo/planner/plan/controller/PlanControllerIntegrationTest.java`

이 테스트는 다음 항목들을 자동으로 검증합니다:
- ✅ Plan 생성 (기본값 및 커스텀 파라미터)
- ✅ Plan 단건 조회 (성공 및 404 케이스)
- ✅ Plan 상세 조회 (Days와 Places 포함)
- ✅ 사용자별 Plan 목록 조회
- ✅ Plan 수정/삭제 미구현 확인 (501 응답)
- ✅ 다양한 일수로 Plan 생성

**주의**: 통합 테스트는 실제 데이터베이스를 사용하므로 데이터베이스가 연결되어 있어야 합니다.

---

## 참고 정보

- **Controller**: `/src/main/java/com/example/demo/planner/plan/controller/PlanController.java`
- **Service**: `/src/main/java/com/example/demo/planner/plan/service/PlanService.java`
- **Entity**: `/src/main/java/com/example/demo/planner/plan/dto/entity/Plan.java`
- **Integration Test**: `/src/test/java/com/example/demo/planner/plan/controller/PlanControllerIntegrationTest.java`
- **Server Port**: 8080 (application.properties)
- **API Base Path**: `/api/plans`
