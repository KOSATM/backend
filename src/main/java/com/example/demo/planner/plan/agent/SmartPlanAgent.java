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
    private final DayManagementAgent dayManagementAgent;
    private final PlaceTimeAgent placeTimeAgent;
    private final VersionManagementAgent versionManagementAgent;

    private final Map<Long, List<String>> historyMap = new HashMap<>();

    public SmartPlanAgent(
            ChatClient.Builder builder,
            PlanQueryService queryService,
            PlanCrudService crudService,
            TravelPlanAgent travelPlanAgent,
            SearchPlaceAgent searchPlaceAgent,
            PlaceManagementAgent placeManagementAgent,
            ScheduleOptimizationAgent scheduleOptimizationAgent,
            DayManagementAgent dayManagementAgent,
            PlaceTimeAgent placeTimeAgent,
            VersionManagementAgent versionManagementAgent
    ) {
        this.chatClient = builder.build();
        this.queryService = queryService;
        this.crudService = crudService;
        this.travelPlanAgent = travelPlanAgent;
        this.searchPlaceAgent = searchPlaceAgent;
        this.placeManagementAgent = placeManagementAgent;
        this.scheduleOptimizationAgent = scheduleOptimizationAgent;
        this.dayManagementAgent = dayManagementAgent;
        this.placeTimeAgent = placeTimeAgent;
        this.versionManagementAgent = versionManagementAgent;
    }

    @Override
    public AiAgentResponse execute(IntentCommand command, Long userId) {
        return executeInternal(command.getOriginalUserMessage(), userId);
    }

    /**
     * ChatController에서 직접 호출되는 메서드
     */
    public AiAgentResponse execute(String userMessage, Long userId) {
        return executeInternal(userMessage, userId);
    }

    private AiAgentResponse executeInternal(String userMsg, Long userId) {
        log.info("[SmartPlanAgent] User({}): {}", userId, userMsg);

        PlanContext ctx = loadContext(userId);

        // ✅ Plan이 없는 경우 → 일정 생성
        if (!ctx.hasActivePlan()) {
            log.info("[SmartPlanAgent] 활성 Plan 없음 → TravelPlanAgent 호출");

            String response = chatClient.prompt()
                    .system("""
                            당신은 여행 일정 관리 전문가입니다.
                            사용자가 새로운 여행 일정 생성을 원하는지 확인하세요.
                            """)
                    .user(userMsg)
                    .tools(travelPlanAgent)
                    .toolContext(Map.of("userId", userId))
                    .call()
                    .content();

            return AiAgentResponse.of(response);
        }

        // ✅ Plan이 있는 경우 → 일정 관리
        Long planId = ctx.getActivePlan().getId();

        String planJson = ctx.toJson();
        List<String> history = historyMap.computeIfAbsent(userId, k -> new ArrayList<>());
        history.add("User: " + userMsg);

        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(planJson, history, userMsg);

        String llm = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .tools(
                        searchPlaceAgent,
                        placeManagementAgent,
                        scheduleOptimizationAgent,
                        dayManagementAgent,
                        placeTimeAgent,
                        versionManagementAgent
                )
                .toolContext(Map.of(
                        "userId", userId,
                        "planId", planId
                ))
                .call()
                .content();

        saveHistory(userId, llm);
        return AiAgentResponse.of(llm);
    }

    /* ================= Prompt Builder ================= */

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
                여행 일정의 생성, 수정, 삭제는 반드시 Tool을 통해 수행하세요.
                """;
    }
}
