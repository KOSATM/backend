package com.example.demo.planner.plan.agent;

import java.util.*;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import com.example.demo.common.chat.intent.dto.IntentCommand;
import com.example.demo.common.chat.pipeline.AiAgentResponse;
import com.example.demo.common.global.agent.AiAgent;
import com.example.demo.planner.plan.dto.context.PlanContext;
import com.example.demo.planner.plan.dto.entity.Plan;
import com.example.demo.planner.plan.service.PlanCrudService;
import com.example.demo.planner.plan.service.PlanQueryService;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SmartPlanAgent implements AiAgent {

    private final ChatClient chatClient;
    private final PlanQueryService queryService;
    private final PlanCrudService crudService;
    private final PlanTools planTools;
    private final Map<Long, List<String>> historyMap = new HashMap<>();

    public SmartPlanAgent(ChatClient.Builder builder,
                          PlanQueryService queryService,
                          PlanCrudService crudService,
                          PlanTools planTools) {
        this.chatClient = builder.build();
        this.queryService = queryService;
        this.crudService = crudService;
        this.planTools = planTools;
    }

    @Override
    public AiAgentResponse execute(IntentCommand command, Long userId) {

        String userMsg = command.getOriginalUserMessage();
        log.info("[SmartPlanAgent] User({}): {}", userId, userMsg);

        PlanContext ctx = loadContext(userId);
        if (!ctx.hasActivePlan()) {
            return AiAgentResponse.of("현재 활성화된 여행 일정이 없습니다. 새로운 여행 계획을 만들어주세요!");
        }

        // PlanTools에 planId 설정
        Long planId = ctx.getActivePlan().getId();
        planTools.setPlanId(planId);

        try {
            String planJson = ctx.toJson();
            List<String> history = historyMap.computeIfAbsent(userId, k -> new ArrayList<>());
            history.add("User: " + userMsg);

            String systemPrompt = buildSystemPrompt();
            String userPrompt = buildUserPrompt(planJson, history, userMsg);

            log.info("[Tool Calling] LLM 호출 with 13 functions");

            // Tool Calling 방식으로 LLM 호출
            String llm = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .tools(planTools)  // PlanTools의 모든 @Description 메서드가 자동 등록됨
                    .toolContext(Map.of("userId", userId))
                    .call()
                    .content();

            log.info("[LLM Response]\n{}", llm);

            saveHistory(userId, llm);

            // 최신 일정 다시 로드 (삭제된 경우 빈 컨텍스트 반환)
            PlanContext updatedCtx = loadContext(userId);

            // 응답 반환
            return AiAgentResponse.of(llm);
        } finally {
            // planId 정리
            planTools.clearPlanId();
        }
    }

    /* ─────────────────────────────────────────────
     * Prompt Builder
     * ───────────────────────────────────────────── */
    private String buildUserPrompt(String json, List<String> history, String userMsg) {
        String hist = history.size() > 20
                ? String.join("\n", history.subList(history.size() - 20, history.size()))
                : String.join("\n", history);

        return """
                ### 전체 여행 일정 (JSON):
                ```json
                %s
                ```

                ### 지금까지의 대화:
                %s

                ### 사용자 요청:
                "%s"
                """.formatted(json, hist, userMsg);
    }

    private void saveHistory(Long userId, String answer) {
        historyMap.get(userId).add("Assistant: " + answer);
    }

    public PlanContext loadPlanContext(Long userId) {
        return loadContext(userId);
    }

    private PlanContext loadContext(Long userId) {
        try {
            Plan plan = crudService.findActiveByUserId(userId);
            return (plan == null)
                    ? PlanContext.empty()
                    : PlanContext.builder()
                            .activePlan(plan)
                            .allDays(queryService.queryAllDaysOptimized(plan.getId()))
                            .build();
        } catch (Exception e) {
            return PlanContext.empty();
        }
    }

    private String buildSystemPrompt() {
        return """
        당신은 여행 일정 관리 AI 어시스턴트입니다.

        ## 중요한 규칙

        ### 🔢 dayIndex는 1부터 시작
        - 1일차 = dayIndex: 1
        - 2일차 = dayIndex: 2
        - **0이 아닙니다!**

        ### 🍽️ 음식/식당 요청 처리
        - "짜장면 먹고 싶어", "피자 추가해줘" 같은 음식 이름 언급 시:
          1. searchPlace("음식명") 먼저 호출
          2. 검색 결과에서 적절한 음식점 찾음
          3. addPlace() 또는 addPlaceAtPosition()으로 추가

        ### ⚠️ 전체 일정 삭제 시 반드시 확인 필수!
        - "일정 삭제", "전체 삭제", "다 지워줘" 등 **전체 일정 삭제** 요청 시:
          1. **절대 바로 deletePlan() 호출하지 마세요**
          2. 먼저 "정말로 전체 일정을 삭제하시겠습니까? 삭제하면 복구할 수 없습니다." 확인 요청
          3. 사용자가 "네", "응", "삭제해", "확인" 등으로 명확히 확인한 경우에만 deletePlan() 호출

        ### ✅ 일반 작업 (확인 불필요)
        - 특정 장소 삭제, 장소 추가/수정/교환, 시간 변경, 날짜 삭제: 바로 실행

        함수 호출 후에는 친절하게 결과를 설명하세요.
        """;
    }
}
