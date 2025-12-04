# Plan CRUD API - Postman 빠른 시작 가이드

## 즉시 테스트하기 (5분 안에)

### 1️⃣ 서버 실행
```bash
cd /home/runner/work/backend/backend
./gradlew bootRun
```

### 2️⃣ Postman에서 아래 요청들을 순서대로 실행

#### 📝 1단계: Plan 생성
```
POST http://localhost:8080/api/plans?userId=1&days=3
```
✅ **성공**: 201 Created + Plan 정보가 반환됨
💡 응답에서 `"id": 1` 값을 확인하세요 (다음 단계에서 사용)

---

#### 📖 2단계: Plan 조회
```
GET http://localhost:8080/api/plans/1
```
✅ **성공**: 200 OK + Plan 기본 정보

---

#### 📚 3단계: Plan 상세 조회 (Days + Places 포함)
```
GET http://localhost:8080/api/plans/1/detail
```
✅ **성공**: 200 OK + Plan, Days, Places 전체 정보
💡 3일치 데이터와 각 날짜마다 2개의 장소가 있어야 합니다

---

#### 📋 4단계: 사용자의 모든 Plan 조회
```
GET http://localhost:8080/api/plans/user/1
```
✅ **성공**: 200 OK + Plan 배열

---

#### ⚠️ 5단계: 미구현 기능 확인

**Plan 수정 (미구현)**
```
PUT http://localhost:8080/api/plans/1
Headers: Content-Type: application/json
Body: {"budget": 600000}
```
✅ **예상**: 501 Not Implemented

**Plan 삭제 (미구현)**
```
DELETE http://localhost:8080/api/plans/1
```
✅ **예상**: 501 Not Implemented

---

## 더 자세한 정보가 필요하면?

- **전체 가이드**: `POSTMAN_GUIDE.md` 읽어보기
- **요약**: `PLAN_CRUD_TESTING_SUMMARY.md` 읽어보기
- **자동화 테스트**: `./gradlew test --tests PlanControllerIntegrationTest`

---

## 문제 해결

### ❌ "Connection refused"
→ 서버가 실행 중인지 확인하세요: `./gradlew bootRun`

### ❌ "500 Internal Server Error"
→ PostgreSQL 데이터베이스가 연결되어 있는지 확인하세요

### ❌ "404 Not Found"
→ URL에 `/api` prefix가 있는지 확인하세요: `/api/plans`

---

## 성공 체크리스트

- [ ] POST로 Plan 생성 성공 (201 Created)
- [ ] GET으로 Plan 조회 성공 (200 OK)
- [ ] GET detail로 Days + Places 조회 성공 (200 OK)
- [ ] GET user로 사용자 Plan 목록 조회 성공 (200 OK)
- [ ] PUT, DELETE가 501 Not Implemented 반환 확인

모두 체크되었다면 Plan CRUD API가 정상적으로 작동하고 있습니다! ✅
