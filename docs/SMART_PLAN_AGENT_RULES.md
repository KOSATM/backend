# SmartPlanAgent 턴 규칙 (v1.0)

## 🎯 핵심 철학

```
1. chatMemory가 컨텍스트의 중심
2. hasToolCalls()로 단계 구분
3. 탐색 → 질문 → 확정 → 쓰기 순서 유도
4. PendingAction은 예외 상황에만
```

---

## 📋 규칙 1: 턴 유형 분류

### 탐색 턴 (Exploration Turn)
```
특징:
- hasToolCalls() = true
- 읽기 전용 tool 호출 (search*, query*)
- 정보 수집 목적

예시:
User: "1일차 점심에 국밥 추가해줘"
Agent: [searchPlace("국밥", "서울")] → 3개 후보 반환
```

### 질문 턴 (Clarification Turn)
```
특징:
- hasToolCalls() = false
- Tool 호출 없음
- 사용자 확인 목적

예시:
User: (이전 턴에서 검색 완료)
Agent: "다음 중 어떤 국밥집으로 하시겠어요? 1) 종로 국밥 2) 광화문 국밥 3) 청와대 국밥"
```

### 확정 턴 (Confirmation Turn)
```
특징:
- hasToolCalls() = true
- 쓰기 tool 호출 (add*, update*, delete*, create*)
- DB 변경 목적

예시:
User: "2번으로 해줘"
Agent: [addPlace(...), createSnapshot(...)]
Response: "광화문 국밥을 추가했어요!"
```

---

## 📋 규칙 2: Tool 호출 제약

### ✅ 허용
```java
// 탐색 턴에서 여러 검색 tool
searchPlaceAgent.searchPlace("국밥", "종로")
searchPlaceAgent.searchPlace("카페", "강남")

// 확정 턴에서 쓰기 + 스냅샷
placeManagementAgent.addPlace(...)
versionManagementAgent.createSnapshot(...)
```

### ❌ 금지
```java
// 탐색과 쓰기 동시 호출
searchPlaceAgent.searchPlace(...) + placeManagementAgent.addPlace(...)

// 질문 턴에서 tool 호출
// → 발생 시 경고 로그, tool 결과 무시

// 3개 이상 tool 동시 호출
tool1() + tool2() + tool3()

// chatMemory 없이 DB 쓰기
```

---

## 📋 규칙 3: ChatMemory 의존

### Memory 저장 시점
```java
1. 사용자 메시지 수신 시
   history.add("User: " + userMsg)

2. LLM 응답 생성 후
   history.add("Assistant: " + response)

3. Tool 실행 결과
   history.add("Tool result: " + toolResult)
```

### Memory 활용
```java
// 짧은 응답 해석
User: "2번으로 해줘"
→ Memory에서 "1) 종로 국밥 2) 광화문 국밥" 찾음

// 대명사 해석
User: "그거 삭제해줘"
→ Memory에서 마지막 추가한 장소 찾음

// 맥락 유지
User: "시간은?"
→ Memory에서 "장소 추가 중" 상태 파악
```

---

## 📋 규칙 4: 예외 처리

### 탐색 없이 바로 쓰기 가능한 경우

```java
조건:
1. 장소가 명확 (placeId 또는 이름+주소)
2. 시간이 명확 (N일차 오전/오후 N시)
3. planContext에 이미 존재

예시:
User: "강남역 스타벅스 1일차 오전 9시에 추가해줘"
→ 검색 불필요, 바로 addPlace()
```

### 질문 턴에서 Tool이 불린 경우

```java
// LLM이 질문하면서 tool을 부를 수 있음
// → 시스템이 필터링

if (isQuestionTurn && response.hasToolCalls()) {
    log.warn("Question turn but tool called. Ignored.");
    // Tool 결과 무시, 텍스트 응답만 사용
}
```

### 복잡한 Multi-step 작업

```java
조건:
- 3턴 이상 필요
- 여러 파라미터 순차 수집
- 되돌릴 수 없는 작업

→ 이 경우에만 PendingAction 도입 고려
```

---

## 📋 규칙 5: PendingAction 사용 기준

### 사용하지 않는 경우 (현재)
```
✅ 단순 장소 추가/삭제/수정
✅ 시간 변경
✅ 순서 조정
✅ 스냅샷 생성

이유:
- chatMemory로 충분
- 2턴 이내 완료
- 실수 시 되돌리기 쉬움
```

### 사용하는 경우 (미래)
```
⚠️ 전체 일정 삭제
⚠️ 결제/예약 진행
⚠️ 외부 API 호출 (취소 불가)
⚠️ 5턴 이상 복잡한 작업

이유:
- 명시적 확인 필요
- 트랜잭션 보장
- 중간 상태 저장
```

---

## 🧪 테스트 시나리오: "국밥 추가"

```
턴 1: 탐색
User: "1일차 점심에 국밥 추가해줘"
Agent: [searchPlace("국밥")] → 3개 후보
Memory: [User: "국밥 추가", Tool: [후보1, 후보2, 후보3]]

턴 2: 질문
Agent: "어떤 국밥집으로 할까요? 1) 종로 2) 광화문 3) 청와대"
Memory: [Agent: "질문"]

턴 3: 확정
User: "2번으로 해줘"
Agent: [addPlace(광화문 국밥), createSnapshot()]
Memory: [User: "2번", Agent: "추가 완료"]
Response: "광화문 국밥을 1일차 점심에 추가했어요!"
```

---

## 🔍 디버깅 포인트

### Tool이 안 불릴 때
```
1. Prompt에 tool 정보가 포함되었는가?
2. LLM이 판단하기에 tool이 불필요했는가?
3. chatMemory에 이미 충분한 정보가 있는가?
```

### 질문 없이 바로 실행될 때
```
1. 정보가 충분히 명확한가?
2. 예외 케이스인가? (강남역 스타벅스 등)
3. LLM이 확신을 가졌는가?
```

### 무한 루프에 빠질 때
```
1. chatMemory가 제대로 저장되는가?
2. Tool 결과가 Memory에 포함되는가?
3. 같은 질문을 반복하는가?
```

---

## 📌 향후 개선 방향

### Phase 1 (현재)
- [x] ChatMemory 중심 구조
- [x] PromptBuilder 분리
- [x] 턴 규칙 정의
- [ ] 실제 테스트 검증

### Phase 2
- [ ] Tool 타입별 PromptBuilder 특화
- [ ] Tool 호출 검증 로직 강화
- [ ] Memory 압축/요약 전략

### Phase 3
- [ ] PendingAction 도입 (필요 시)
- [ ] 트랜잭션 관리
- [ ] 복잡한 Multi-step 지원

---

**작성일**: 2025-12-17  
**버전**: 1.0  
**담당**: SmartPlanAgent 설계팀
