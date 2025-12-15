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
                    .tools(planTools) // PlanTools의 모든 @Description 메서드가 자동 등록됨
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

    /*
     * ─────────────────────────────────────────────
     * Prompt Builder
     * ─────────────────────────────────────────────
     */
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
                당신은 서울 여행 계획을 도와주는 AI 어시스턴트입니다.

                사용자의 질문을 분석하여,
                여행 일정과 관련된 작업이 필요한 경우
                가장 적절한 Tool(Function)를 자동으로 선택하세요.

                여행 일정의 생성, 수정, 삭제는
                반드시 제공된 Tool을 통해서만 수행해야 하며,
                임의로 일정을 추측하거나 변경해서는 안 됩니다.

                [여행 일정 생성]
                - 사용자가 "여행 일정 만들어줘", "N박 N일 여행",
                  "서울 당일치기", "3일 kpop 여행" 등
                  **전체 여행 계획 생성을 요청하면**
                  → `master_createSeoulTravelPlan` Tool을 사용하세요.

                [기존 일정 처리]
                - 기존 여행 일정의 장소 추가, 삭제, 변경, 순서 변경,
                  날짜 삭제, 기간 연장, 버전 되돌리기 등은
                  각 작업에 맞는 개별 Tool을 사용하세요.

                [중요 규칙]
                - dayIndex는 반드시 1부터 시작합니다 (0 사용 금지)
                - 여행 지역은 서울로 한정합니다
                - 한 번의 응답에서는
                  상태를 변경하는 Tool을 최대 한 개만 호출하세요
                - Tool 실행 후에는
                  변경된 내용과 버전 번호를 사용자에게 설명하세요

                [전체 일정 삭제 주의]
                - "전체 삭제", "일정 삭제", "다 지워줘" 요청 시
                  즉시 deletePlan을 호출하지 마세요
                - 사용자의 명확한 확인이 있을 때만 deletePlan을 호출하세요

                        """;
    }
}
