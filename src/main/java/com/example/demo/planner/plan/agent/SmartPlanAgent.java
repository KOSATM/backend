package com.example.demo.planner.plan.agent;

import java.util.HashMap;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

import com.example.demo.common.chat.pipeline.AiAgentResponse;
import com.example.demo.common.chat.prompt.PromptBuilder;
import com.example.demo.planner.plan.agent.common.PlanToolSupport;
import com.example.demo.planner.plan.agent.tools.PlaceRecommendTools;
import com.example.demo.planner.plan.agent.tools.PlanAdvancedTools;
import com.example.demo.planner.plan.agent.tools.PlanBasicTools;
import com.example.demo.planner.plan.agent.tools.PlanCreateTools;
import com.example.demo.planner.plan.agent.tools.PlanVersionTools;
import com.example.demo.planner.plan.agent.tools.PlanViewTools;
import com.example.demo.planner.plan.dto.context.PlanContext;
import com.example.demo.planner.plan.dto.entity.Plan;
import com.example.demo.planner.plan.service.PlanCrudService;
import com.example.demo.planner.plan.service.PlanQueryService;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SmartPlanAgent {

    private final ChatClient chatClient;
    private final PlanQueryService queryService;
    private final PlanCrudService crudService;

    private final PlanToolSupport planSupport;
    private final PlanViewTools planViewTools;
    private final PlanBasicTools planBasicTools;
    private final PlanAdvancedTools planAdvancedTools;
    private final PlanCreateTools planCreateTools;
    private final PlaceRecommendTools placeRecommendTools;
    private final PlanVersionTools planVersionTools;

    public SmartPlanAgent(
            ChatClient.Builder builder,
            PlanQueryService queryService,
            PlanCrudService crudService,
            ChatMemory chatMemory,
            PlanToolSupport planSupport,
            PlanViewTools planViewTools,
            PlanBasicTools planBasicTools,
            PlanAdvancedTools planAdvancedTools,
            PlanCreateTools planCreateTools,
            PlaceRecommendTools placeRecommendTools,
            PlanVersionTools planVersionTools) {

        this.chatClient = builder.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build()).build();
        this.queryService = queryService;
        this.crudService = crudService;
        this.planSupport = planSupport;
        this.planViewTools = planViewTools;
        this.planBasicTools = planBasicTools;
        this.planAdvancedTools = planAdvancedTools;
        this.planCreateTools = planCreateTools;
        this.placeRecommendTools = placeRecommendTools;
        this.planVersionTools = planVersionTools;
    }

    public AiAgentResponse execute(String userMessage, Long userId) {

        log.info("[SmartPlanAgent] 사용자({}) 요청: {}", userId, userMessage);

        String conversationId = userId.toString();

        PlanContext ctx = loadContext(userId);

        // planId 매핑
        if (ctx.hasActivePlan()) {
            Long planId = ctx.getActivePlan().getId();
            planSupport.setPlanId(conversationId, planId);
            log.info("🧩 [컨텍스트] conversationId={}, planId={}", conversationId, planId);
        } else {
            log.info("🧩 [컨텍스트] conversationId={}, 활성 일정 없음", conversationId);
        }

        try {
            String systemPrompt = buildSystemPrompt();
            String stateContext = planSupport.buildStateContext(conversationId);
            String userPrompt = buildUserPrompt(ctx.toJson(), userMessage);

            Map<String, Object> toolCtx = new HashMap<>();
            toolCtx.put("userId", userId);
            toolCtx.put("conversationId", conversationId);
            toolCtx.put("userMessage", userMessage);

            String llm = chatClient.prompt()
                    .messages(
                            new SystemMessage(systemPrompt),
                            new SystemMessage(stateContext),
                            new UserMessage(userPrompt))
                    .tools(
                            planViewTools,
                            planBasicTools,
                            planAdvancedTools,
                            planCreateTools,
                            placeRecommendTools,
                            planVersionTools)
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .toolContext(toolCtx)
                    .call()
                    .content();

            return AiAgentResponse.of(llm);

        } finally {
            // 상태 유지 (초기화하지 않음)
        }
    }

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

    /**
     * 외부에서 PlanContext 조회용 (테스트/디버깅)
     */
    public PlanContext loadPlanContext(Long userId) {
        return loadContext(userId);
    }

    /**
     * 사용자의 활성 Plan Context 로드
     */
    private PlanContext loadContext(Long userId) {
        try {
            Plan plan = crudService.findActiveByUserId(userId);
            return (plan == null)
                    ? PlanContext.empty()
                    : PlanContext.builder()
                            .userId(userId)
                            .activePlan(plan)
                            .allDays(queryService.queryAllDaysOptimized(plan.getId()))
                            .build();
        } catch (Exception e) {
            return PlanContext.empty();
        }
    }


    private String buildSystemPrompt() {
        return """
                당신은 친근한 서울 여행 플래너 AI입니다.

                # 핵심 규칙

                1. 상태 확인
                   <context> 태그를 보고 현재 상태를 파악하세요:
                   - RECOMMENDATION → addRecommendedPlace
                   - ADD_CANDIDATE → addPlace
                   - 대기 없음 → 사용자 요청에 맞는 Tool 선택

                2. 한 턴에 한 번만 응답
                   - Tool 호출 → 결과 확인 → 응답
                   - Tool이 질문하면 사용자 답변 기다리기
                   - 사용자 답변 후 → 필요시 Tool 다시 호출

                3. 자연스럽게
                   - "버전 5", "STATE" 같은 용어 숨기기
                   - 이모지 적절히

                # Tool 선택 가이드

                사용자 요청에 맞는 Tool을 선택하세요:
                - "추천해줘" → recommendPlace
                - "추가해줘" → addPlace
                - "삭제해줘" → deletePlace
                - "바꿔줘" → replacePlace
                - "일정 보여줘" → viewPlan
                - "2일차 보여줘" → viewDay
                - "2일차 삭제" → deleteDay
                - "다시 짜줘" → regenerateDay

                # 예시

                - 일반적인 흐름:
                User: "경복궁 삭제해줘"
                You: [deletePlace("경복궁")]
                Tool: "삭제했습니다"
                You: "경복궁 삭제했어요! ✨"

                - 멀티턴 흐름:
                Turn 1:
                  User: "경복 추가해줘"
                  You: [addPlace("경복")]
                  Tool: "몇 일차에?"
                  You: "몇 일차에 추가할까요? 😊"

                Turn 2:
                  User: "1일차"
                  You: [addPlace("경복")] ← 다시 호출 OK

                - 상태 기반 흐름:
                <context>: RECOMMENDATION 상태
                User: "3번"
                You: [addRecommendedPlace] ← 상태에 맞는 Tool

                # 응답 스타일

                간결하고 친근하게.
                """;
    }

}
