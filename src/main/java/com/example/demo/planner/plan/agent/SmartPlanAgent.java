package com.example.demo.planner.plan.agent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import com.example.demo.common.chat.intent.dto.IntentCommand;
import com.example.demo.common.chat.pipeline.AiAgentResponse;
import com.example.demo.common.global.agent.AiAgent;
import com.example.demo.planner.plan.dto.context.PlanContext;
import com.example.demo.planner.plan.dto.entity.Plan;
import com.example.demo.planner.plan.service.action.PlanActionExecutor;
import com.example.demo.planner.plan.service.create.PlanService;

import lombok.extern.slf4j.Slf4j;

/**
 * 🧠 SmartPlanAgent - LLM Full-Reasoning Architecture with Function Calling
 *
 * 완전히 새로운 아키텍처:
 * - LLM이 전체 일정 JSON을 보고 직접 reasoning
 * - action 분류, slot-filling, 응답 생성 모두 LLM이 처리
 * - Function Calling을 통해 필요시 실제 DB 변경 가능
 * - 유지보수 최소화 (프롬프트만 수정)
 *
 * 핵심 흐름:
 * 1. 전체 일정 JSON 준비
 * 2. 대화 히스토리 로드
 * 3. LLM에게 모든 정보 전달 + 사용 가능한 함수 제공
 * 4. LLM이 필요시 함수 호출 (deletePlaceByName, swapPlaces 등)
 * 5. LLM이 생성한 자연어 응답 반환
 */
@Component
@Slf4j
public class SmartPlanAgent implements AiAgent {

    private final ChatClient chatClient;
    private final PlanService planService;
    private final PlanActionExecutor planActionExecutor;

    // 유저별 multi-turn 대화 기록 (임시 저장소 → 실서비스에서는 Redis 권장)
    private final Map<Long, List<String>> chatHistory = new HashMap<>();

    public SmartPlanAgent(
            ChatClient.Builder chatClientBuilder,
            PlanService planService,
            PlanActionExecutor planActionExecutor
    ) {
        this.chatClient = chatClientBuilder.build();
        this.planService = planService;
        this.planActionExecutor = planActionExecutor;
    }

    @Override
    public AiAgentResponse execute(IntentCommand command, Long userId) {

        String userMessage = command.getOriginalUserMessage();
        log.info("🧠 === SmartPlanAgent (LLM Full-Reasoning) ===");
        log.info("📝 사용자 메시지: {}", userMessage);
        log.info("👤 사용자 ID: {}", userId);

        // 1단계: 전체 일정 불러오기
        PlanContext planContext = loadPlanContext(userId);

        if (!planContext.hasActivePlan()) {
            return AiAgentResponse.of("현재 활성화된 여행 일정이 없습니다. 먼저 여행 계획을 생성해주세요.");
        }

        String planJson = planContext.toJson();
        log.info("📅 일정 JSON 로드 완료 (길이: {}자)", planJson.length());

        // 2단계: 대화 히스토리 로드
        List<String> history = chatHistory.computeIfAbsent(userId, k -> new ArrayList<>());
        history.add("User: " + userMessage);

        // 3단계: LLM 프롬프트 구성
        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(planJson, history, userMessage);

        log.info("🤖 LLM 호출 중...");

        // 4단계: LLM 호출 (String으로 받음 - 수동 Function Calling)
        String llmResponse = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();

        log.info("✅ LLM 응답 생성 완료");
        log.info("💬 응답 내용:\n{}", llmResponse);

        // 5단계: Function Call 감지 및 처리 (마크다운 코드 블록 제거)
        String cleanResponse = llmResponse.replaceAll("```\\s*", "").trim();

        String answer;
        if (cleanResponse.startsWith("FUNCTION_CALL:")) {
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("🔧 Function Call 감지");
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            // 첫 줄에서 function call 추출
            String[] lines = cleanResponse.split("\n", 2);
            String functionCallLine = lines[0].replace("FUNCTION_CALL:", "").trim();
            String naturalResponse = lines.length > 1 ? lines[1].trim() : "";

            log.info("📞 Function Call: {}", functionCallLine);

            // Function 실행
            String result = parseFunctionCallAndExecute(functionCallLine, planContext.getActivePlan().getId());

            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            if (result.startsWith("✅")) {
                // 성공 → 일정 다시 로드하여 보여주기
                log.info("🔄 일정 변경 감지 → DB에서 최신 데이터 다시 로드");
                planContext = loadPlanContext(userId);
                planJson = planContext.toJson();
                log.info("✅ 최신 일정 로드 완료 (길이: {}자)", planJson.length());

                // LLM에게 변경된 일정 보여주도록 요청
                String confirmationPrompt = String.format("""
                        Function 실행 결과:
                        %s

                        변경된 일정:
                        %s

                        변경 사항을 확인하고 사용자에게 안내해주세요. 변경된 일정을 보여주세요.
                        """, result, planJson);

                answer = chatClient.prompt()
                        .system(systemPrompt)
                        .user(confirmationPrompt)
                        .call()
                        .content();
            } else if (result.startsWith("🔍")) {
                // 검색 결과 → 그대로 사용자에게 보여주기 (FUNCTION_CALL 텍스트는 제거됨)
                answer = result;
            } else {
                // 실패 또는 기타 → 에러 메시지 + 자연어 응답
                answer = result + (naturalResponse.isEmpty() ? "" : "\n\n" + naturalResponse);
            }
        } else {
            // Function Call 없는 일반 응답
            answer = llmResponse;
        }

        log.info("✅ LLM 응답 생성 완료");
        log.info("💬 응답 내용: {}", answer);

        // 6단계: 대화 기록 저장
        history.add("Assistant: " + answer);

        // 히스토리 관리 (최근 10턴만 유지)
        if (history.size() > 20) {
            history.subList(0, history.size() - 20).clear();
        }

        return AiAgentResponse.of(answer);
    }

    /**
     * Function Call 문자열 파싱 및 실행
     * 예: "deletePlace(planId=343, placeName="덕수궁")"
     */
    private String parseFunctionCallAndExecute(String functionCallLine, Long planId) {
        try {
            // 함수명 추출
            int parenIndex = functionCallLine.indexOf('(');
            if (parenIndex == -1) {
                return "❌ Function Call 형식 오류: " + functionCallLine;
            }

            String functionName = functionCallLine.substring(0, parenIndex).trim();
            String argsStr = functionCallLine.substring(parenIndex + 1, functionCallLine.lastIndexOf(')')).trim();

            log.info("   ├─ Function: {}", functionName);
            log.info("   └─ Arguments: {}", argsStr);

            // 파라미터 파싱 (key=value 형식)
            Map<String, String> params = new HashMap<>();
            if (!argsStr.isEmpty()) {
                String[] pairs = argsStr.split(",");
                for (String pair : pairs) {
                    String[] kv = pair.split("=", 2);
                    if (kv.length == 2) {
                        String key = kv[0].trim();
			key = normalizeKey(key);
                        String value = kv[1].trim().replaceAll("^\"|\"$", ""); // 따옴표 제거
                        // null 또는 빈 문자열은 넣지 않음
                        if (value != null && !value.isEmpty() && !value.equalsIgnoreCase("null")) {
                            params.put(key, value);
                        }
                    }
                }
            }

            // Function 실행
            return dispatchFunction(functionName, params, planId);

        } catch (Exception e) {
            log.error("Function Call 파싱 실패", e);
            return "❌ Function Call 처리 중 오류: " + e.getMessage();
        }
    }

    /**
     * Function 디스패칭
     */
    private String dispatchFunction(String functionName, Map<String, String> params, Long planId) {
        try {
            return switch (functionName) {
                case "deletePlace" -> planActionExecutor.deletePlace(planId, params.get("placeName"));

                case "swapPlaces" -> planActionExecutor.swapPlaces(
                        planId,
                        Integer.parseInt(params.get("dayIndex")),
                        Integer.parseInt(params.get("index1")),
                        Integer.parseInt(params.get("index2"))
                );

                case "swapPlacesBetweenDays" -> planActionExecutor.swapPlacesBetweenDays(
                        planId,
                        Integer.parseInt(params.get("day1")),
                        Integer.parseInt(params.get("index1")),
                        Integer.parseInt(params.get("day2")),
                        Integer.parseInt(params.get("index2"))
                );

                case "replacePlace" -> planActionExecutor.replacePlace(
                        planId,
                        params.get("oldPlaceName"),
                        params.get("newPlaceName")
                );

                case "searchPlace" -> planActionExecutor.searchPlace(
                        params.get("searchQuery")
                );

                case "replacePlaceWithSelection" -> planActionExecutor.replacePlaceWithSelection(
                        planId,
                        params.get("oldPlaceName"),
                        params.get("newPlaceName"),
                        Integer.parseInt(params.get("selectedIndex"))
                );

                case "addPlace" -> planActionExecutor.addPlace(
                        planId,
                        Integer.parseInt(params.get("dayIndex")),
                        params.get("placeName"),
                        params.get("startTime")
                );

                case "addPlaceAtPosition" -> planActionExecutor.addPlaceAtPosition(
                        planId,
                        Integer.parseInt(params.get("dayIndex")),
                        Integer.parseInt(params.get("position")),
                        params.get("placeName"),
                        params.containsKey("duration") ? Integer.parseInt(params.get("duration")) : null
                );

                case "updatePlaceTime" -> planActionExecutor.updatePlaceTime(
                        planId,
                        params.get("placeName"),
                        params.get("newTime")
                );

                case "deleteDay" -> planActionExecutor.deleteDay(
                        planId,
                        Integer.parseInt(params.get("dayIndex"))
                );

                case "swapDays" -> planActionExecutor.swapDays(
                        planId,
                        Integer.parseInt(params.get("day1")),
                        Integer.parseInt(params.get("day2"))
                );

                case "extendPlan" -> planActionExecutor.extendPlan(
                        planId,
                        Integer.parseInt(params.get("extraDays"))
                );

                case "deletePlan" -> planActionExecutor.deletePlan(planId);

                default -> "❌ 알 수 없는 함수: " + functionName;
            };
        } catch (Exception e) {
            log.error("Function 실행 실패: {}", functionName, e);
            return "❌ Function 실행 중 오류: " + e.getMessage();
        }
    }

    private String normalizeKey(String key) {
    return switch (key) {
        case "old", "oldName", "oldPlace", "old_place", "old_place_name" ->
            "oldPlaceName";
        case "new", "newName", "newPlace", "new_place", "new_place_name" ->
            "newPlaceName";
        case "day", "dayIdx", "day_index" ->
            "dayIndex";
        case "idx", "index" ->
            "index";
        case "pos", "position" ->
            "position";
        default -> key;
    };
}


    /**
     * LLM 시스템 프롬프트 생성
     * - LLM의 역할 정의
     * - 할 수 있는 작업 나열
     * - 응답 규칙 명시
     */
    private String buildSystemPrompt() {
        return """
당신은 **기존 여행 일정을 관리하는 AI 어시스턴트**입니다.

⚠️ **Function Call 출력 규칙**:
일정 수정이 필요한 경우, 응답의 **첫 줄**에 다음 형식으로 function call을 명시하세요:
```
FUNCTION_CALL: 함수명(파라미터1="값1", 파라미터2="값2", ...)
```

예시:
```
FUNCTION_CALL: deletePlace(planId=343, placeName="덕수궁")
```

그 다음 줄부터는 사용자에게 자연스럽게 설명하세요.

사용자의 여행 일정이 JSON 형식으로 제공됩니다.
사용자의 요청을 정확히 파악하여 가장 자연스럽고 유용한 한국어 응답을 생성하세요.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
### 🛠️ 사용 가능한 함수 (Function Calling) - 총 13개
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

#### 📍 장소 관련 (8개)

1. **addPlace(planId, dayIndex, placeName, startTime?)**
   - 특정 날짜에 새로운 장소 추가
   - 네이버 검색으로 자동으로 주소/좌표 찾음
   - startTime은 선택 사항 (없으면 마지막 장소 + 30분)
   - 예: "2일차에 남산타워 추가해줘" → `FUNCTION_CALL: addPlace(planId=343, dayIndex=1, placeName="남산타워")`

2. **deletePlace(planId, placeName)**
   - 특정 장소를 일정에서 삭제
   - Fuzzy matching 지원 (비슷한 이름도 찾음)
   - 예: "창경궁 삭제해줘" → `FUNCTION_CALL: deletePlace(planId=343, placeName="창경궁")`

3. **searchPlace(searchQuery)** ⭐ 신규! 먼저 검색하고 확인받기
   - 장소를 네이버에서 검색하여 여러 후보 보여주기
   - **장소 교체/추가 전에 먼저 사용 권장**
   - 검색 결과를 사용자에게 보여주고 선택받음
   - 예: "창경궁으로 바꿔줘" → 먼저 `FUNCTION_CALL: searchPlace(searchQuery="창경궁")`
     → 결과: "1. 창경궁(관광지), 2. 창경궁초밥(식당), 3. ..."
     → 사용자 선택 대기

4. **replacePlaceWithSelection(planId, oldPlaceName, newPlaceName, selectedIndex)** ⭐ 신규!
   - searchPlace 후 사용자가 선택한 장소로 교체
   - 예: 사용자가 "1번"이라고 선택 → `FUNCTION_CALL: replacePlaceWithSelection(planId=343, oldPlaceName="덕수궁", newPlaceName="창경궁", selectedIndex=1)`

5. **replacePlace(planId, oldPlaceName, newPlaceName)** ⚠️ 비권장 (자동 선택)
   - 기존 장소를 다른 장소로 교체 (첫 번째 검색 결과 자동 선택)
   - **가능하면 searchPlace → replacePlaceWithSelection 사용하세요**
   - 명확한 경우에만 사용 (예: "경복궁을 63빌딩으로")
   - 예: "덕수궁을 창경궁으로 바꿔줘" → `FUNCTION_CALL: replacePlace(planId=343, oldPlaceName="덕수궁", newPlaceName="창경궁")`

6. **swapPlaces(planId, dayIndex, placeIndex1, placeIndex2)**
   - 같은 날짜 내에서 두 장소의 순서 교환
   - 예: "1일차의 첫 번째와 두 번째 순서 바꿔줘" → `FUNCTION_CALL: swapPlaces(planId=343, dayIndex=0, index1=0, index2=1)`

7. **swapPlacesBetweenDays(planId, dayIndex1, placeIndex1, dayIndex2, placeIndex2)**
   - 서로 다른 날짜 간 장소 교환
   - 예: "1일차 첫 번째와 2일차 두 번째 바꿔줘" → `FUNCTION_CALL: swapPlacesBetweenDays(planId=343, day1=0, index1=0, day2=1, index2=1)`

8. **addPlaceAtPosition(planId, dayIndex, position, placeName, duration)** ⭐ 신규! 특정 위치 삽입
   - 특정 위치에 장소를 삽입하고 뒤 일정들을 자동으로 밀어냄
   - position: 1=첫번째, 2=두번째, 3=세번째...
   - duration: 소요시간(분), 기본값 120분
   - **중요**: 삽입 후 position 이후의 모든 일정이 duration만큼 뒤로 밀림
   - 예: "2일차 3번째에 짜장면 추가해줘(1시간 소요)"
     → `FUNCTION_CALL: addPlaceAtPosition(planId=343, dayIndex=2, position=3, placeName="짜장면", duration=60)`
     → 결과: 2일차 3번째에 삽입, 기존 3번째 이후 일정들이 60분씩 뒤로

9. **updatePlaceTime(planId, placeName, newTime)**
   - 특정 장소의 시간 변경
   - 예: "경복궁 시간을 10시로 바꿔줘" → `FUNCTION_CALL: updatePlaceTime(planId=343, placeName="경복궁", newTime="10:00")`

#### 📅 날짜 관련 (2개)

10. **deleteDay(planId, dayIndex)**
   - 특정 날짜 전체 삭제
   - 예: "3일차 일정 전체 삭제해줘" → `FUNCTION_CALL: deleteDay(planId=343, dayIndex=2)`

11. **swapDays(planId, dayIndex1, dayIndex2)**
   - 두 날짜의 일정 전체 교환
   - 예: "1일차와 2일차 일정 바꿔줘" → `FUNCTION_CALL: swapDays(planId=343, day1=0, day2=1)`

12. **extendPlan(planId, extraDays)**
   - 여행 기간 늘리기
   - 예: "하루 더 늘려줘" → `FUNCTION_CALL: extendPlan(planId=343, extraDays=1)`

#### 🗑️ 전체 일정 관리 (1개)

13. **deletePlan(planId)**
   - 전체 일정 삭제 (모든 날짜와 장소 삭제)
   - Plan은 유지되지만 내용이 모두 비워짐
   - ⚠️ 복구 불가능하므로 신중하게 사용
   - 예: "전체 일정 삭제해줘" → `FUNCTION_CALL: deletePlan(planId=343)`
   - 예: "다 지우고 처음부터 다시 만들래" → `FUNCTION_CALL: deletePlan(planId=343)`

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
### ⚠️ 핵심 규칙 (반드시 지켜야 함!)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

1. **장소 교체/추가 워크플로우** ⭐ 가장 중요!

   **통합 3단계 프로세스 (교체/추가 모두 동일)**

   **Step 1: 먼저 검색하여 결과 보여주기**
   - 사용자: "창경궁으로 바꿔줘" 또는 "짜장면 추가해줘"
   - AI: `FUNCTION_CALL: searchPlace(searchQuery="창경궁")` 또는 `searchPlace(searchQuery="짜장면")`
   - 결과: 5개 후보 목록 자동 표시
   ```
   🔍 '짜장면' 검색 결과 5개:
   1. **평양면옥 을지로점** (음식점>중식>짜장면) ⭐ 추천!
   2. **공화춘 본점** (음식점>중식)
   3. **차이나타운 짜장** (음식점>중식)
   4. **왕짜장** (음식점>중식)
   5. **중화반점** (음식점>중식)

   어떤 장소를 선택하시겠어요?
   ```
   - **중요**: 이 단계에서는 검색 결과만 보여주고 **즉시 교체/추가하지 않음**
   - 사용자는 검색이 완료될 때까지 대기 (동기 처리처럼 느껴짐)

   **Step 2: 사용자 선택**
   - 사용자: "1번이요" 또는 "첫 번째요"
   - AI: 선택 확인 + 다음 단계 진행

   **Step 3-A: 교체인 경우**
   - AI: `FUNCTION_CALL: replacePlaceWithSelection(planId=343, oldPlaceName="워터킹덤", newPlaceName="짜장면", selectedIndex=1)`
   - 결과: "✅ '워터킹덤'을 '평양면옥 을지로점'으로 변경했습니다"

   **Step 3-B: 추가인 경우 (적절한 위치 제안)**
   - AI가 일정을 분석하여 적절한 위치 제안:
   ```
   평양면옥 을지로점을 추가하시는군요! 일정을 분석해보니:

   📍 추천 위치:
   1. **2일차 3번째** (점심시간 12:00 근처)
      → 이후 일정 4개가 2시간씩 뒤로 밀립니다
   2. **2일차 끝에 추가** (11:40 이후)
      → 일정 변동 없음

   어떻게 추가할까요?
   ```
   - 사용자: "1번으로 해줘"
   - AI: `FUNCTION_CALL: addPlaceAtPosition(planId=343, dayIndex=2, position=3, placeName="짜장면", duration=120)`

   **위치 직접 지정 시:**
   - 사용자: "2일차 3번째에 짜장면 추가해줘"
   - AI: Step 1 (검색) → Step 2 (선택) → Step 3 (바로 해당 위치에 추가)

   **⚠️ 절대 규칙:**
   - ❌ searchPlace 없이 바로 추가/교체 금지
   - ❌ 자동으로 첫 번째 결과 선택 금지
   - ❌ 사용자 선택 없이 진행 금지
   - ✅ **반드시** Step 1(검색) → Step 2(선택) → Step 3(실행) 순서 준수

2. **JSON 데이터만 신뢰**: 제공된 JSON에 있는 정보만 사용하세요. 추측하지 마세요.

3. **사용자 선택 번호 감지** ⭐ 매우 중요!
   - 사용자가 "1번", "2번이요", "첫 번째", "세 번째요" 같은 선택 응답을 하면:
     * **반드시 "지금까지의 대화" 섹션에서 이전 검색 결과(🔍)를 찾아보기**
     * 검색 결과가 있다면:
       - **교체인 경우**: 바로 replacePlaceWithSelection 호출
       - **추가인 경우**: 먼저 적절한 위치를 제안하고 다시 확인받기
     * 예시 (교체):
       ```
       Assistant: 🔍 '창경궁' 검색 결과 5개:
       1. **창경궁** (여행,명소>궁궐)
       2. **창경궁초밥** (음식점>일식)
       ...
       User: 1번
       → replacePlaceWithSelection(..., selectedIndex=1)
       ```
     * 예시 (추가):
       ```
       Assistant: 🔍 '짜장면' 검색 결과 5개:
       1. **연경** (중식>중식당)
       ...
       User: 1번
       → AI: "연경을 추가하시는군요! 어디에 넣을까요?
            1. 2일차 3번째 (점심시간)
            2. 2일차 끝에 추가
            어떻게 할까요?"
       User: 1번으로
       → addPlaceAtPosition(..., position=3)
       ```
   - 검색 결과 없이 "1번"이라고 하면: "무엇의 1번을 말씀하시는 건가요?" 되물음

4. **모호하면 즉시 되물음** ⚠️ 매우 중요!
   - "우리 덕수궁 가나?" → "일정에 포함 여부를 묻는 건가요, 추가하고 싶은 건가요?"
   - "일정 바꿔줘" → "어떤 일정을 어떻게 바꾸고 싶으신가요?"
   - **Function Call 전에 반드시 확인**: 실행 후 되돌릴 수 없으므로 애매하면 무조건 물어보기!

5. **변경 작업은 신중하게**:
   - 삭제/교체 요청은 한 번 더 확인 (선택 사항)
   - 함수 호출 후에는 "✅ 완료했습니다" 형태로 결과 안내

6. **자연어로만 응답**: JSON, 분석 내용, 메타 정보를 출력하지 마세요.

7. **명확하고 구조화된 응답**:
   - 일정 조회 시 이모지와 번호로 구조화
   - 시간 정보 명확히 표시
   - 필요시 추가 질문 제안

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
### 💬 할 수 있는 작업
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

1. **일정 조회**
   - 전체 일정 보기
   - 특정 일차 일정 보기 (예: "2일차 일정 뭐야?")
   - 특정 장소 찾기 (예: "경복궁 언제 가?")
   - 특정 순서 일정 확인 (예: "2일차 첫번째 일정 뭐야?")

2. **일정 포함 여부 확인**
   - 특정 장소가 일정에 있는지 확인 (예: "덕수궁 있어?")

3. **일정 실제 변경** ⭐ 핵심!
   - 장소 추가: addPlace
   - 장소 삭제: deletePlaceByName
   - 장소 교체: **replacePlace** (가장 중요!)
   - 순서 교환: swapPlacesInDay, swapPlacesBetweenDays
   - 시간 변경: updatePlaceTime
   - 날짜 삭제/교환: deleteDay, swapDays
   - 일정 확장: extendPlan

4. **불명확한 요청 처리**
   - 요청이 모호하면 즉시 되물어봄
   - 필요한 정보를 명확히 질문 (Slot-Filling)

5. **자연스러운 대화**
   - 친근하고 도움이 되는 톤
   - 이모지 적절히 활용

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
### 📋 함수 호출 예시 (반드시 참고!)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

**예시 1: 장소 교체 (가장 흔한 패턴)**
```
사용자: "덕수궁을 창경궁으로 바꿔줘"
AI 판단: 이건 교체 요청이다!
→ replacePlace(343, "덕수궁", "창경궁") 호출 ✅
→ 절대로 deletePlaceByName + addPlace 하지 않음! ❌
```

**예시 2: 장소 삭제**
```
사용자: "창경궁 삭제해줘"
→ deletePlace(343, "창경궁")
```

**예시 3: 시간 조정 (시간이 너무 가까워)**
```
사용자: "추어탕 하고 짜장면 먹는 일정의 시간이 너무 가까워"
AI 분석:
1. 일정에서 추어탕 찾기 → "오계절 남원추어탕" 발견 (1일차 7번째, 10:40~11:40)
2. 일정에서 짜장면 찾기 → "무탄 코엑스점" 발견 (1일차 8번째, 11:50~13:50)
3. 시간 간격 확인 → 10분 간격 (너무 가까움!)
4. 해결 방법 제안:
   - 방법 1: 순서 바꾸기 (swapPlaces)
   - 방법 2: 시간 조정 (updatePlaceTime)
   - 방법 3: 하나 삭제하기

AI 응답:
"현재 1일차 일정에서 '오계절 남원추어탕'(10:40~11:40)과 '무탄 코엑스점'(11:50~13:50)의 시간이 10분 간격으로 너무 가까운 점을 확인했습니다.

📍 해결 방안:
1. **두 식사 순서를 바꾸기** - 무탄을 앞으로, 추어탕을 뒤로
2. **짜장면 시간을 늦추기** - 14:00으로 변경하여 2시간 간격 확보
3. **둘 중 하나 삭제** - 한 끼만 선택

어떻게 할까요?"

사용자: "1번으로 해줘"
→ swapPlaces(planId=343, dayIndex=1, index1=7, index2=8)
```

**예시 3: 장소 추가**
```
사용자: "2일차에 남산타워 추가해줘"
→ addPlace(343, 2, "남산타워", null)
```

**예시 4: 일정 확장**
```
사용자: "하루 더 늘려줘"
→ extendPlan(343, 1)
```

**예시 5: 조회만 (함수 호출 없음)**
```
사용자: "2일차 일정 뭐야?"
→ JSON을 파싱하여 자연어로 응답
→ 함수 호출 필요 없음
```

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
### 🎨 응답 형식 예시
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

**좋은 예시:**
```
📅 2일차 일정입니다!

1. 경복궁 — 09:00~11:00
2. 북촌한옥마을 — 11:30~13:00
3. 인사동 — 13:30~15:00

더 자세한 정보가 필요하신가요?
```

**나쁜 예시:**
```
JSON을 분석한 결과, day=2에 3개의 items가 있습니다...
```

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

**다시 한 번 강조:**
- "A를 B로 바꿔줘" → **replacePlace** 함수 사용!
- delete + add 조합은 절대 안 됨!
- 순서와 시간이 유지되어야 함!
""";
    }

    /**
     * 사용자 프롬프트 생성
     * - 전체 일정 JSON
     * - 대화 히스토리
     * - 현재 사용자 메시지
     */
    private String buildUserPrompt(String planJson, List<String> history, String userMessage) {
        StringBuilder historyStr = new StringBuilder();

        // 최근 10턴만 포함
        int startIndex = Math.max(0, history.size() - 20);
        for (int i = startIndex; i < history.size(); i++) {
            historyStr.append(history.get(i)).append("\n");
        }

        return """
### 전체 여행 일정 (JSON):
```json
%s
```

### 지금까지의 대화:
%s

### 사용자 요청:
"%s"

### 응답:
위 정보를 바탕으로 사용자 요청에 대한 자연스러운 한국어 응답을 생성하세요.
""".formatted(
            planJson,
            historyStr.length() > 0 ? historyStr.toString() : "(대화 시작)",
            userMessage
        );
    }

    /**
     * 여행 일정 컨텍스트 로드 (최적화)
     * 🔥 단일 JOIN 쿼리로 전체 일정 로드
     */
    /**
     * 사용자의 활성화된 여행 계획 컨텍스트 로드
     * 외부에서도 접근 가능하도록 public으로 변경 (테스트/디버깅용)
     */
    public PlanContext loadPlanContext(Long userId) {
        try {
            Plan plan = planService.findActiveByUserId(userId);
            if (plan == null) {
                return PlanContext.empty();
            }

            return PlanContext.builder()
                    .activePlan(plan)
                    .allDays(planService.queryAllDaysOptimized(plan.getId()))  // 🚀 최적화된 메서드 사용
                    .build();

        } catch (Exception e) {
            log.error("❌ 일정 로드 실패", e);
            return PlanContext.empty();
        }
    }

    /**
     * Function 실행 디스패처 (수동)
     * LLM이 호출한 function을 실제로 실행
     */
    /**
     * 대화 히스토리 초기화 (테스트용)
     */
    public void clearHistory(Long userId) {
        chatHistory.remove(userId);
        log.info("🗑️ 사용자 {}의 대화 히스토리 초기화", userId);
    }
}
