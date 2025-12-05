#!/bin/bash

# 간소화된 Plan API 테스트 (User 없이 직접 확인)
BASE_URL="http://localhost:8080"

echo "================================"
echo "Plan API 기능 테스트"
echo "================================"
echo ""

# DB에 User 직접 추가 (psql)
echo "[사전 준비] DB에 테스트 User 추가"
PGPASSWORD=postgres psql -h localhost -U postgres -d kosatm -c "INSERT INTO users (email, name) VALUES ('test@example.com', 'Test User') ON CONFLICT (email) DO NOTHING RETURNING id;" 2>/dev/null || echo "User may already exist"

USER_ID=1  # 기존 또는 새로 생성된 User

echo "사용할 User ID: $USER_ID"
echo ""

# 1. Plan 생성
echo "[1] Plan 생성 (3일 계획)"
curl -s -X POST ${BASE_URL}/plans \
  -H "Content-Type: application/json" \
  -d "{
    \"userId\": ${USER_ID},
    \"budget\": 500000,
    \"startDate\": \"2025-12-10\",
    \"endDate\": \"2025-12-12\",
    \"title\": \"서울 여행\"
  }" | jq .

PLAN_ID=$(curl -s -X POST ${BASE_URL}/plans \
  -H "Content-Type: application/json" \
  -d "{
    \"userId\": ${USER_ID},
    \"budget\": 300000,
    \"startDate\": \"2025-12-15\",
    \"endDate\": \"2025-12-17\",
    \"title\": \"테스트 여행2\"
  }" | jq -r '.id')

echo ""
echo "✅ Plan ID: $PLAN_ID"
echo ""

# 2. Day 생성 (1일차)
echo "[2] Day 생성 (1일차 - confirm 불필요)"
curl -s -X POST ${BASE_URL}/plans/days \
  -H "Content-Type: application/json" \
  -d "{
    \"planId\": ${PLAN_ID},
    \"dayIndex\": 1,
    \"title\": \"Day 1\"
  }" | jq .
echo ""

# 3. Day 생성 시도 (5일차 - confirm 없음, 예외 예상)
echo "[3] Day 생성 시도 (5일차, confirm 없음 → 예외 예상)"
curl -s -X POST ${BASE_URL}/plans/days \
  -H "Content-Type: application/json" \
  -d "{
    \"planId\": ${PLAN_ID},
    \"dayIndex\": 5,
    \"title\": \"Day 5\"
  }" | jq .
echo ""

# 4. Day 생성 미리보기
echo "[4] Day 생성 미리보기 (5일차)"
curl -s -X GET "${BASE_URL}/plans/${PLAN_ID}/days/create-preview?dayIndex=5" | jq .
echo ""

# 5. Day 생성 (confirm=true)
echo "[5] Day 생성 (5일차, confirm=true → 성공 예상)"
curl -s -X POST "${BASE_URL}/plans/days?confirm=true" \
  -H "Content-Type: application/json" \
  -d "{
    \"planId\": ${PLAN_ID},
    \"dayIndex\": 5,
    \"title\": \"Day 5 (확장됨)\"
  }" | jq .
echo ""

# 6. Plan 확인 (endDate 확장 확인)
echo "[6] Plan 조회 (endDate 확장 확인)"
curl -s -X GET "${BASE_URL}/plans/${PLAN_ID}" | jq '.endDate'
echo ""

# 7. 2일차, 3일차 추가
echo "[7] Day 2, 3 생성"
curl -s -X POST ${BASE_URL}/plans/days \
  -H "Content-Type: application/json" \
  -d "{\"planId\": ${PLAN_ID}, \"dayIndex\": 2, \"title\": \"Day 2\"}" > /dev/null

curl -s -X POST ${BASE_URL}/plans/days \
  -H "Content-Type: application/json" \
  -d "{\"planId\": ${PLAN_ID}, \"dayIndex\": 3, \"title\": \"Day 3\"}" > /dev/null

DAY1_ID=$(curl -s -X GET "${BASE_URL}/plans/${PLAN_ID}" | jq -r '.days[0].day.id // empty')
echo "Day 1 ID: $DAY1_ID"
echo ""

if [ ! -z "$DAY1_ID" ] && [ "$DAY1_ID" != "null" ]; then
  # 8. Day 이동 미리보기
  echo "[8] Day 이동 미리보기 (1일차 → 7일차)"
  curl -s -X GET "${BASE_URL}/plans/days/${DAY1_ID}/move-preview?toIndex=7" | jq .
  echo ""

  # 9. Day 이동 (confirm 없음)
  echo "[9] Day 이동 시도 (confirm 없음 → 예외 예상)"
  curl -s -X POST "${BASE_URL}/plans/days/${DAY1_ID}/move?toIndex=7" \
    -H "Content-Type: application/json" | jq .
  echo ""

  # 10. Day 이동 (confirm=true)
  echo "[10] Day 이동 (confirm=true → 성공 예상)"
  curl -s -X POST "${BASE_URL}/plans/days/${DAY1_ID}/move?toIndex=7&confirm=true" \
    -H "Content-Type: application/json" | jq '.plan.endDate'
  echo ""
else
  echo "Day 1 ID를 가져올 수 없어 이동 테스트를 건너뜁니다."
fi

echo "================================"
echo "테스트 완료"
echo "================================"
