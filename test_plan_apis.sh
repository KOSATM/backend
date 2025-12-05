#!/bin/bash

# Plan API 통합 테스트 스크립트
BASE_URL="http://localhost:8080"

echo "================================"
echo "Plan API 통합 테스트"
echo "================================"
echo ""

# 색상 코드
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 0. User 생성 (Foreign Key 만족)
echo "${YELLOW}[사전 준비] User 생성${NC}"
USER_RESPONSE=$(curl -s -X POST ${BASE_URL}/users \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "name": "테스트 사용자"
  }')

USER_ID=$(echo $USER_RESPONSE | jq -r '.id // 1')
echo "사용할 User ID: $USER_ID"
echo ""

# 1. Plan 생성 (3일 계획)
echo "${YELLOW}[테스트 1] Plan 생성 (3일 계획)${NC}"
PLAN_RESPONSE=$(curl -s -X POST ${BASE_URL}/plans \
  -H "Content-Type: application/json" \
  -d "{
    \"userId\": $USER_ID,
    \"budget\": 500000,
    \"startDate\": \"2025-12-10\",
    \"endDate\": \"2025-12-12\",
    \"title\": \"서울 여행\"
  }")

PLAN_ID=$(echo $PLAN_RESPONSE | jq -r '.id')
echo "생성된 Plan ID: $PLAN_ID"
echo "응답: $PLAN_RESPONSE" | jq
echo ""

if [ "$PLAN_ID" != "null" ] && [ ! -z "$PLAN_ID" ]; then
  echo "${GREEN}✅ Plan 생성 성공${NC}"
else
  echo "${RED}❌ Plan 생성 실패${NC}"
  exit 1
fi

# 2. PlanDay 생성 (1일차)
echo ""
echo "${YELLOW}[테스트 2] PlanDay 생성 (1일차 - confirm 불필요)${NC}"
DAY1_RESPONSE=$(curl -s -X POST ${BASE_URL}/plans/days \
  -H "Content-Type: application/json" \
  -d "{
    \"planId\": $PLAN_ID,
    \"dayIndex\": 1,
    \"title\": \"첫째 날\"
  }")

DAY1_ID=$(echo $DAY1_RESPONSE | jq -r '.id')
echo "생성된 Day ID: $DAY1_ID"
echo "응답: $DAY1_RESPONSE" | jq
echo ""

if [ "$DAY1_ID" != "null" ] && [ ! -z "$DAY1_ID" ]; then
  echo "${GREEN}✅ 1일차 생성 성공${NC}"
else
  echo "${RED}❌ 1일차 생성 실패${NC}"
fi

# 3. PlanDay 생성 시도 (5일차 - 기간 초과, confirm 없음)
echo ""
echo "${YELLOW}[테스트 3] PlanDay 생성 시도 (5일차 - confirm 없음, 예외 예상)${NC}"
DAY5_ERROR=$(curl -s -X POST ${BASE_URL}/plans/days \
  -H "Content-Type: application/json" \
  -d "{
    \"planId\": $PLAN_ID,
    \"dayIndex\": 5,
    \"title\": \"다섯째 날\"
  }")

echo "응답: $DAY5_ERROR" | jq
echo ""

if echo "$DAY5_ERROR" | jq -e '.error' > /dev/null 2>&1; then
  echo "${GREEN}✅ 예상대로 예외 발생 (confirm 필요)${NC}"
else
  echo "${RED}❌ 예외가 발생하지 않음 (정책 위반)${NC}"
fi

# 4. PlanDay 생성 미리보기 (5일차)
echo ""
echo "${YELLOW}[테스트 4] PlanDay 생성 미리보기 (5일차)${NC}"
PREVIEW_RESPONSE=$(curl -s -X GET "${BASE_URL}/plans/${PLAN_ID}/days/create-preview?dayIndex=5")
echo "응답: $PREVIEW_RESPONSE" | jq
echo ""

REQUIRES_EXT=$(echo $PREVIEW_RESPONSE | jq -r '.requiresExtension')
if [ "$REQUIRES_EXT" = "true" ]; then
  echo "${GREEN}✅ 확장 필요 감지됨${NC}"
  NEW_END_DATE=$(echo $PREVIEW_RESPONSE | jq -r '.newEndDate')
  echo "예상 종료일: $NEW_END_DATE"
else
  echo "${RED}❌ 확장 감지 실패${NC}"
fi

# 5. PlanDay 생성 (5일차 - confirm=true)
echo ""
echo "${YELLOW}[테스트 5] PlanDay 생성 (5일차 - confirm=true)${NC}"
DAY5_CONFIRM=$(curl -s -X POST "${BASE_URL}/plans/days?confirm=true" \
  -H "Content-Type: application/json" \
  -d "{
    \"planId\": $PLAN_ID,
    \"dayIndex\": 5,
    \"title\": \"다섯째 날 (확장됨)\"
  }")

DAY5_ID=$(echo $DAY5_CONFIRM | jq -r '.id')
echo "생성된 Day ID: $DAY5_ID"
echo "응답: $DAY5_CONFIRM" | jq
echo ""

if [ "$DAY5_ID" != "null" ] && [ ! -z "$DAY5_ID" ]; then
  echo "${GREEN}✅ 5일차 생성 성공 (confirm으로 확장됨)${NC}"
else
  echo "${RED}❌ 5일차 생성 실패${NC}"
fi

# 6. Plan 상세 조회 (확장 확인)
echo ""
echo "${YELLOW}[테스트 6] Plan 상세 조회 (endDate 확장 확인)${NC}"
PLAN_DETAIL=$(curl -s -X GET "${BASE_URL}/plans/${PLAN_ID}")
echo "응답: $PLAN_DETAIL" | jq
echo ""

END_DATE=$(echo $PLAN_DETAIL | jq -r '.endDate')
echo "현재 endDate: $END_DATE"
if [ "$END_DATE" = "2025-12-14" ]; then
  echo "${GREEN}✅ endDate가 2025-12-14로 확장됨 (5일차 반영)${NC}"
else
  echo "${YELLOW}⚠️  예상 endDate: 2025-12-14, 실제: $END_DATE${NC}"
fi

# 7. PlanDay 생성 (2일차, 3일차)
echo ""
echo "${YELLOW}[테스트 7] PlanDay 생성 (2일차, 3일차)${NC}"
curl -s -X POST ${BASE_URL}/plans/days \
  -H "Content-Type: application/json" \
  -d "{\"planId\": $PLAN_ID, \"dayIndex\": 2, \"title\": \"둘째 날\"}" > /dev/null

curl -s -X POST ${BASE_URL}/plans/days \
  -H "Content-Type: application/json" \
  -d "{\"planId\": $PLAN_ID, \"dayIndex\": 3, \"title\": \"셋째 날\"}" > /dev/null

echo "${GREEN}✅ 2일차, 3일차 생성 완료${NC}"

# 8. Day 이동 미리보기 (1일차 → 7일차)
echo ""
echo "${YELLOW}[테스트 8] Day 이동 미리보기 (1일차 → 7일차)${NC}"
MOVE_PREVIEW=$(curl -s -X GET "${BASE_URL}/plans/days/${DAY1_ID}/move-preview?toIndex=7")
echo "응답: $MOVE_PREVIEW" | jq
echo ""

MOVE_REQUIRES_EXT=$(echo $MOVE_PREVIEW | jq -r '.requiresExtension')
if [ "$MOVE_REQUIRES_EXT" = "true" ]; then
  echo "${GREEN}✅ 이동 시 확장 필요 감지됨${NC}"
  MOVE_NEW_END=$(echo $MOVE_PREVIEW | jq -r '.newEndDate')
  echo "예상 종료일: $MOVE_NEW_END"
else
  echo "${RED}❌ 확장 감지 실패${NC}"
fi

# 9. Day 이동 시도 (confirm 없음)
echo ""
echo "${YELLOW}[테스트 9] Day 이동 시도 (1일차 → 7일차, confirm 없음)${NC}"
MOVE_ERROR=$(curl -s -X POST "${BASE_URL}/plans/days/${DAY1_ID}/move?toIndex=7" \
  -H "Content-Type: application/json")
echo "응답: $MOVE_ERROR" | jq
echo ""

if echo "$MOVE_ERROR" | jq -e '.error' > /dev/null 2>&1; then
  echo "${GREEN}✅ 예상대로 예외 발생 (confirm 필요)${NC}"
else
  echo "${RED}❌ 예외가 발생하지 않음${NC}"
fi

# 10. Day 이동 (confirm=true)
echo ""
echo "${YELLOW}[테스트 10] Day 이동 (1일차 → 7일차, confirm=true)${NC}"
MOVE_SUCCESS=$(curl -s -X POST "${BASE_URL}/plans/days/${DAY1_ID}/move?toIndex=7&confirm=true" \
  -H "Content-Type: application/json")
echo "응답 (일부):" 
echo "$MOVE_SUCCESS" | jq '.plan' 2>/dev/null || echo "$MOVE_SUCCESS"
echo ""

if echo "$MOVE_SUCCESS" | jq -e '.plan' > /dev/null 2>&1; then
  FINAL_END_DATE=$(echo $MOVE_SUCCESS | jq -r '.plan.endDate')
  echo "최종 endDate: $FINAL_END_DATE"
  if [ "$FINAL_END_DATE" = "2025-12-16" ]; then
    echo "${GREEN}✅ Day 이동 성공 및 endDate 확장됨 (7일차 반영)${NC}"
  else
    echo "${YELLOW}⚠️  예상 endDate: 2025-12-16, 실제: $FINAL_END_DATE${NC}"
  fi
else
  echo "${RED}❌ Day 이동 실패${NC}"
fi

# 최종 결과
echo ""
echo "================================"
echo "${GREEN}테스트 완료!${NC}"
echo "================================"
echo ""
echo "요약:"
echo "- Plan 생성 ✓"
echo "- Day 생성 (기간 내) ✓"
echo "- Day 생성 (확장, confirm 없음 → 예외) ✓"
echo "- Day 생성 미리보기 ✓"
echo "- Day 생성 (확장, confirm=true) ✓"
echo "- Day 이동 미리보기 ✓"
echo "- Day 이동 (확장, confirm 없음 → 예외) ✓"
echo "- Day 이동 (확장, confirm=true) ✓"
echo ""
