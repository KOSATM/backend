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
import com.example.demo.planner.plan.service.create.PlanService;

import lombok.extern.slf4j.Slf4j;

/**
 * 🧠 SmartPlanAgent - LLM Full-Reasoning Architecture
 * 
 * 완전히 새로운 아키텍처:
 * - LLM이 전체 일정 JSON을 보고 직접 reasoning
 * - action 분류, slot-filling, 응답 생성 모두 LLM이 처리
 * - PlanActionExecutor 불필요 (LLM이 직접 응답 생성)
 * - 유지보수 최소화 (프롬프트만 수정)
 * 
 * 핵심 흐름:
 * 1. 전체 일정 JSON 준비
 * 2. 대화 히스토리 로드
 * 3. LLM에게 모든 정보 전달
 * 4. LLM이 생성한 자연어 응답 반환
 */
@Component
@Slf4j
public class SmartPlanAgent implements AiAgent {

    private final ChatClient chatClient;
    private final PlanService planService;

    // 유저별 multi-turn 대화 기록 (임시 저장소 → 실서비스에서는 Redis 권장)
    private final Map<Long, List<String>> chatHistory = new HashMap<>();

    public SmartPlanAgent(
            ChatClient.Builder chatClientBuilder,
            PlanService planService
    ) {
        this.chatClient = chatClientBuilder.build();
        this.planService = planService;
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

        // 4단계: LLM 호출
        String answer = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();

        log.info("✅ LLM 응답 생성 완료");
        log.info("💬 응답 내용: {}", answer);

        // 5단계: 대화 기록 저장
        history.add("Assistant: " + answer);

        // 히스토리 관리 (최근 10턴만 유지)
        if (history.size() > 20) {
            history.subList(0, history.size() - 20).clear();
        }

        return AiAgentResponse.of(answer);
    }

    /**
     * LLM 시스템 프롬프트 생성
     * - LLM의 역할 정의
     * - 할 수 있는 작업 나열
     * - 응답 규칙 명시
     */
    private String buildSystemPrompt() {
        return """
당신은 여행 일정 관리 AI 어시스턴트입니다.

사용자의 여행 일정이 JSON 형식으로 제공됩니다.
사용자의 요청을 정확히 파악하여 가장 자연스럽고 유용한 한국어 응답을 생성하세요.

### 할 수 있는 작업

1. **일정 조회**
   - 전체 일정 보기
   - 특정 일차 일정 보기 (예: "2일차 일정 뭐야?")
   - 특정 장소 찾기 (예: "경복궁 언제 가?")
   - 특정 순서 일정 확인 (예: "2일차 첫번째 일정 뭐야?")

2. **일정 포함 여부 확인**
   - 특정 장소가 일정에 있는지 확인 (예: "덕수궁 있어?", "우리 도쿄 타워 가?")

3. **일정 변경 제안** (실제 변경은 하지 않음, 확인만)
   - 일정 추가 제안 (예: "경복궁 추가할까요?")
   - 일정 삭제 확인 (예: "정말 삭제할까요?")
   - 장소 교체 확인 (예: "롯데리아를 버거킹으로 바꿀까요?")

4. **불명확한 요청 처리**
   - 요청이 모호하면 즉시 되물어봄
   - 필요한 정보를 명확히 질문 (Slot-Filling)

5. **자연스러운 대화**
   - 친근하고 도움이 되는 톤
   - 이모지 적절히 활용

### 중요한 규칙

1. **JSON 데이터만 신뢰**: 제공된 JSON에 있는 정보만 사용하세요. 추측하지 마세요.

2. **모호하면 즉시 되물음**: 
   - "우리 덕수궁 가나?" → "일정에 포함 여부를 묻는 건가요, 추가하고 싶은 건가요?"
   - "일정 바꿔줘" → "어떤 일정을 어떻게 바꾸고 싶으신가요?"

3. **변경 작업은 확인만**:
   - 실제로 데이터를 변경할 수 없으므로, 변경이 필요하면 "~할까요?" 형태로 확인 질문
   - 예: "2일차에 경복궁을 추가할까요?"

4. **자연어로만 응답**: JSON, 분석 내용, 메타 정보를 출력하지 마세요.

5. **명확하고 구조화된 응답**:
   - 일정 조회 시 이모지와 번호로 구조화
   - 시간 정보 명확히 표시
   - 필요시 추가 질문 제안

### 응답 예시

좋은 예시:
"📅 2일차 일정입니다!

1. 경복궁 — 09:00~11:00
2. 북촌한옥마을 — 11:30~13:00
3. 인사동 — 13:30~15:00

더 자세한 정보가 필요하신가요?"

나쁜 예시:
"JSON을 분석한 결과, day=2에 3개의 items가 있습니다..."

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
     * 여행 일정 컨텍스트 로드
     */
    private PlanContext loadPlanContext(Long userId) {
        try {
            Plan plan = planService.findActiveByUserId(userId);
            if (plan == null) {
                return PlanContext.empty();
            }

            return PlanContext.builder()
                    .activePlan(plan)
                    .allDays(planService.queryAllDays(plan.getId()))
                    .build();

        } catch (Exception e) {
            log.error("❌ 일정 로드 실패", e);
            return PlanContext.empty();
        }
    }

    /**
     * 대화 히스토리 초기화 (테스트용)
     */
    public void clearHistory(Long userId) {
        chatHistory.remove(userId);
        log.info("🗑️ 사용자 {}의 대화 히스토리 초기화", userId);
    }
}
