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
import com.example.demo.planner.plan.service.PlanCrudService;
import com.example.demo.planner.plan.service.PlanQueryService;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SmartPlanAgent implements AiAgent {

    private final ChatClient chatClient;
    private final PlanQueryService queryService;
    private final PlanCrudService crudService;

    // 멀티에이전트: 모든 여행 관련 에이전트들
    private final TravelPlanAgent travelPlanAgent;
    private final SearchPlaceAgent searchPlaceAgent;
    private final PlaceManagementAgent placeManagementAgent;
    private final ScheduleOptimizationAgent scheduleOptimizationAgent;

    private final Map<Long, List<String>> historyMap = new HashMap<>();
    private final ThreadLocal<Long> currentPlanId = new ThreadLocal<>();

    public SmartPlanAgent(ChatClient.Builder builder,
            PlanQueryService queryService,
            PlanCrudService crudService,
            TravelPlanAgent travelPlanAgent,
            SearchPlaceAgent searchPlaceAgent,
            PlaceManagementAgent placeManagementAgent,
            ScheduleOptimizationAgent scheduleOptimizationAgent) {
        this.chatClient = builder.build();
        this.queryService = queryService;
        this.crudService = crudService;
        this.travelPlanAgent = travelPlanAgent;
        this.searchPlaceAgent = searchPlaceAgent;
        this.placeManagementAgent = placeManagementAgent;
        this.scheduleOptimizationAgent = scheduleOptimizationAgent;
    }

    @Override
    public AiAgentResponse execute(IntentCommand command, Long userId) {

        String userMsg = command.getOriginalUserMessage();
        log.info("[SmartPlanAgent] User({}): {}", userId, userMsg);

        PlanContext ctx = loadContext(userId);

        // ✅ Plan이 없는 경우: 일정 생성 요청인지 확인
        if (!ctx.hasActivePlan()) {
            log.info("[SmartPlanAgent] 활성 Plan 없음 → TravelPlanAgent로 일정 생성 시도");

            // LLM으로 일정 생성 의도 확인 및 TravelPlanAgent 호출
            String response = chatClient.prompt()
                    .system("""
                            당신은 여행 일정 관리 전문가입니다.
                            사용자가 새로운 여행 일정 생성을 원하는지 확인하고, 필요한 정보를 수집합니다.

                            ## 사용 가능한 도구:
                            - createSeoulTravelPlanStructured: 서울 여행 일정 생성 (duration 필수)

                            ## 작업 절차:
                            1. 사용자 메시지에서 여행 기간, 스타일, 선호 지역 등 추출
                            2. 기간 정보가 없으면 사용자에게 질문
                            3. 기간 정보가 있으면 createSeoulTravelPlanStructured 도구 호출
                            4. 생성 결과를 자연스러운 한국어로 설명

                            ## 응답 형식:
                            - 정보 부족: "여행 기간은 며칠인가요? (예: 2박 3일, 3일)"
                            - 생성 완료: "✅ [N일] 여행 일정을 생성했습니다! [간단한 요약]"
                            """)
                    .user(userMsg)
                    .tools(travelPlanAgent)
                    .toolContext(Map.of("userId", userId))
                    .call()
                    .content();

            log.info("[SmartPlanAgent] 일정 생성 응답: {}", response);
            return AiAgentResponse.of(response);
        }

        // ✅ Plan이 있는 경우: 일정 관리 (장소 추가/삭제/교환 등)
        Long planId = ctx.getActivePlan().getId();
        currentPlanId.set(planId);

        try {
            String planJson = ctx.toJson();
            List<String> history = historyMap.computeIfAbsent(userId, k -> new ArrayList<>());
            history.add("User: " + userMsg);

            String systemPrompt = buildSystemPrompt();
            String userPrompt = buildUserPrompt(planJson, history, userMsg);

            log.info("[SmartPlanAgent] Tool-Calling 시작 (멀티에이전트)");

            // ✅ 모든 여행 에이전트를 Tool로 등록
            String llm = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .tools(
                        searchPlaceAgent,           // 장소 검색 에이전트
                        placeManagementAgent,       // 장소 관리 에이전트
                        scheduleOptimizationAgent   // 일정 최적화 에이전트
                    )
                    .toolContext(Map.of(
                        "userId", userId,
                        "planId", planId
                    ))
                    .call()
                    .content();

            log.info("[SmartPlanAgent] LLM 응답:\n{}", llm);

            saveHistory(userId, llm);

            return AiAgentResponse.of(llm);
        } finally {
            currentPlanId.remove();
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
