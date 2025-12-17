package com.example.demo.planner.plan.agent;

import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Component;

import com.example.demo.common.chat.pipeline.AiAgentResponse;
import com.example.demo.planner.plan.agent.common.PlanToolSupport;
import com.example.demo.planner.plan.agent.tools.PlanAdvancedTools;
import com.example.demo.planner.plan.agent.tools.PlanBasicTools;
import com.example.demo.planner.plan.agent.tools.PlanCreateTools;
import com.example.demo.planner.plan.agent.tools.PlanViewTools;
import com.example.demo.planner.plan.dto.context.PlanContext;
import com.example.demo.planner.plan.dto.entity.Plan;
import com.example.demo.planner.plan.service.PlanCrudService;
import com.example.demo.planner.plan.service.PlanQueryService;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SmartPlanAgent{

    private final ChatClient chatClient;
    private final PlanQueryService queryService;
    private final PlanCrudService crudService;

    private final PlanToolSupport planSupport;
    private final PlanViewTools planViewTools;
    private final PlanBasicTools planBasicTools;
    private final PlanAdvancedTools planAdvancedTools;
    private final PlanCreateTools planCreateTools;


    public SmartPlanAgent(
            ChatClient.Builder builder,
            PlanQueryService queryService,
            PlanCrudService crudService,
            ChatMemory chatMemory,
            PlanToolSupport planSupport,
            PlanViewTools planViewTools,
            PlanBasicTools planBasicTools,
            PlanAdvancedTools planAdvancedTools,
            PlanCreateTools planCreateTools) {

        this.chatClient = builder.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build()).build();
        this.queryService = queryService;
        this.crudService = crudService;
        this.planSupport = planSupport;
        this.planViewTools = planViewTools;
        this.planBasicTools = planBasicTools;
        this.planAdvancedTools = planAdvancedTools;
        this.planCreateTools = planCreateTools;
    }

    public AiAgentResponse execute(String userMessage, Long userId) {

        log.info("[SmartPlanAgent] User({}): {}", userId, userMessage);

        PlanContext ctx = loadContext(userId);

        if (ctx.hasActivePlan()) {
            planSupport.setPlanId(ctx.getActivePlan().getId());
        }

        try {
            String systemPrompt = buildSystemPrompt();
            String userPrompt = buildUserPrompt(ctx.toJson(), userMessage);

            String llm = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .tools(
                            planViewTools,
                            planBasicTools,
                            planAdvancedTools,
                            planCreateTools)
                    .advisors(a -> a.param(
                            ChatMemory.CONVERSATION_ID,
                            userId.toString()))
                    .toolContext(Map.of("userId", userId))
                    .call()
                    .content();

            return AiAgentResponse.of(llm);

        } finally {
            planSupport.clearPlanId();
        }
    }

    /*
     * ─────────────────────────────────────────────
     * Prompt Builder
     * ─────────────────────────────────────────────
     */
    private String buildUserPrompt(String planJson, String userMsg) {
        return """
                ### 현재 여행 일정 (JSON)
                ```json
                %s
                ```

                ### 사용자 요청
                "%s"
                """.formatted(planJson, userMsg);
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

                [사용 가능한 Tool 카테고리]

                **조회 (View)**
                - viewPlan: 전체 일정 조회
                - viewDay: 특정 일차 조회

                **생성 (Create)**
                - createSeoulTravelPlan: 새로운 여행 일정 생성
                  (사용자가 "N박 N일 여행", "서울 당일치기" 등 요청 시)

                **기본 수정 (Basic)**
                - deletePlace: 장소 삭제
                - addPlace: 장소 추가
                - replacePlace: 장소 교체
                - updatePlaceTime: 시간 변경
                - deleteDay: 날짜 삭제

                **고급 기능 (Advanced)**
                - swapPlaces: 같은 날짜 내 순서 변경
                - swapPlacesBetweenDays: 다른 날짜 간 장소 교환
                - swapDays: 날짜 전체 교환
                - addPlaceAtPosition: 특정 위치에 삽입
                - extendPlan: 여행 기간 연장
                - searchPlace: 네이버 검색
                - replacePlaceWithSelection: 검색 결과로 교체
                - rollBack: 이전 버전으로 복구
                - rollBackToSpecific: 특정 버전으로 복구
                - deletePlan: 전체 일정 삭제

                [중요 규칙]
                - dayIndex는 반드시 1부터 시작합니다 (0 사용 금지)
                - 여행 지역은 서울로 한정합니다
                - 한 번의 응답에서는 상태를 변경하는 Tool을 최대 한 개만 호출하세요
                - Tool 실행 후에는 변경된 내용과 버전 번호를 사용자에게 설명하세요

                [전체 일정 삭제 주의]
                - "전체 삭제", "일정 삭제", "다 지워줘" 요청 시
                  즉시 deletePlan을 호출하지 마세요
                - 사용자의 명확한 확인이 있을 때만 deletePlan을 호출하세요

                [Tool 선택 가이드]
                - 조회만 필요 → viewPlan, viewDay
                - 간단한 수정 → Basic Tools
                - 복잡한 작업 → Advanced Tools
                - 새로운 여행 계획 → createSeoulTravelPlan
                        """;
    }
}