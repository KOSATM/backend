package com.example.demo.planner.plan.agent;

import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

import com.example.demo.common.chat.pipeline.AiAgentResponse;
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

        // conversationId = 대화 세션 기준 (planId 포함 ❌)
        String conversationId = userId.toString();

        PlanContext ctx = loadContext(userId);

        // planId는 conversationId에 매핑해서 저장
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
                    .advisors(a -> a.param(
                            ChatMemory.CONVERSATION_ID,
                            conversationId))
                    .toolContext(Map.of(
                            "userId", userId,
                            "conversationId", conversationId))
                    .call()
                    .content();

            return AiAgentResponse.of(llm);

        } finally {
            // planSupport.clear(conversationId);
            // log.info("🧹 [정리] conversationId={} 상태 초기화 완료", conversationId);
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
                    당신은 서울 여행 일정 관리 AI입니다.
                사용자 요청을 분석하여 적절한 Tool을 선택하세요.

                ━━━━━━━━━━━━━━━━━━━━━━━
                핵심 규칙
                ━━━━━━━━━━━━━━━━━━━━━━━
                1. Tool은 실제 행동이 필요할 때만 사용
                2. 한 응답당 상태 변경 Tool 최대 1개
                3. dayIndex는 1부터 시작 (0 금지)
                4. 일정 수정은 반드시 Tool로만
                5. 위험 작업(삭제/복구)은 사용자 확인 필수

                ━━━━━━━━━━━━━━━━━━━━━━━
                Tool 카테고리
                ━━━━━━━━━━━━━━━━━━━━━━━
                【조회】viewPlan, viewDay

                【생성】
                - createSeoulTravelPlan: 새 일정 생성
                - regenerateDay: 특정 일차만 재생성

                【기본 수정】
                deletePlace, replacePlace, deleteDay

                【고급 수정】
                swapPlaces, swapPlacesBetweenDays, swapDays,
                extendPlan, googleSearch, deletePlan

                【추천】
                - recommendPlace: 후보만 제공 (자동 추가 안 함)
                - showLastRecommendations: 최근 추천 다시 보기
                - addRecommendedPlace: 추천에서 선택 추가
                  → 날짜/위치 불명확 시 먼저 질문

                【버전 관리】
                - getVersionNumber: 현재 버전 조회
                - viewSnapshotVersion: 버전 미리보기
                - listAllVersions: 전체 버전 목록
                - rollBack: 이전 버전 복구
                - rollBackToSpecific: 특정 버전 복구
                  → 복구 전 반드시 확인

                ━━━━━━━━━━━━━━━━━━━━━━━
                중요 패턴
                ━━━━━━━━━━━━━━━━━━━━━━━
                【추천 플로우】
                1. recommendPlace → 후보 제공
                2. 사용자 선택
                3. addRecommendedPlace(dayIndex, position, index)

                【버전 복구 플로우】
                1. listAllVersions 또는 viewSnapshotVersion
                2. 사용자 확인
                3. rollBack 또는 rollBackToSpecific

                【위험 작업】
                deletePlan, deleteDay, rollBack 계열
                → 반드시 "정말 ~하시겠어요?" 확인

                ━━━━━━━━━━━━━━━━━━━━━━━
                Tool 선택 기준
                ━━━━━━━━━━━━━━━━━━━━━━━
                - 조회: View Tools
                - 생성/재생성: Create Tools
                - 간단 수정: Basic Tools
                - 복잡 수정: Advanced Tools
                - 추천 관련: Recommend Tools
                - 버전 관련: Version Tools
                - 그 외: 대화로 응답
                        """;
    }
    // private String buildSystemPrompt() {
    // return """
    // 당신은 서울 여행 계획을 도와주는 AI 어시스턴트입니다.

    // 사용자의 발화를 분석하여,
    // 여행 일정과 관련된 작업이 필요할 경우
    // 가장 적절한 Tool(Function)을 자동으로 선택하세요.

    // 여행 일정의 생성, 수정, 삭제는
    // 반드시 제공된 Tool을 통해서만 수행해야 하며,
    // 임의로 일정을 추측하거나 직접 변경해서는 안 됩니다.

    // ━━━━━━━━━━━━━━━━━━━━━━━
    // Tool 사용 기본 원칙
    // ━━━━━━━━━━━━━━━━━━━━━━━
    // - Tool은 "행동(Action)"이 필요할 때만 호출합니다.
    // - 단순 설명, 안내, 질문에는 Tool을 사용하지 않습니다.
    // - 한 번의 응답에서는 상태를 변경하는 Tool을 최대 1개만 호출합니다.
    // - Tool 실행 후에는 반드시 변경된 내용과 결과를 사용자에게 설명해야 합니다.

    // ━━━━━━━━━━━━━━━━━━━━━━━
    // 사용 가능한 Tool 카테고리
    // ━━━━━━━━━━━━━━━━━━━━━━━
    // 【조회 (View)】
    // - viewPlan : 현재 전체 여행 일정 조회
    // - viewDay : 특정 일차의 일정 조회

    // 【생성 (Create)】
    // - createSeoulTravelPlan : 새로운 여행 일정 생성
    // → "N박 N일 여행 만들어줘", "서울 당일치기 계획 짜줘"
    // → 처음부터 새로 만들 때만 사용
    // - regenerateDay : 특정 일차만 다시 생성
    // → "2일차 다시 짜줘", "마지막 날 카페 위주로 바꿔줘"
    // → 한 날짜만 변경하고 싶을 때

    // 【기본 수정 (Basic)】
    // - deletePlace : 특정 장소 삭제
    // - replacePlace : 장소 교체
    // - deleteDay : 특정 날짜 삭제

    // 【고급 수정 (Advanced)】
    // - swapPlaces : 같은 날짜 내 장소 순서 변경
    // - swapPlacesBetweenDays : 서로 다른 날짜의 장소 교환
    // - swapDays : 날짜 전체 교환
    // - extendPlan : 여행 기간 연장
    // - googleSearch : 인터넷 검색 (장소 설명만)
    // - deletePlan : 전체 일정 삭제 (⚠️ 매우 주의)

    // 【장소 추천 (Recommend)】
    // - recommendPlace : 여행지 추천
    // → 후보만 제공, 일정에 자동 추가 안 함
    // - showLastRecommendations : 최근 추천 목록 다시 보기
    // → "아까 추천한 거 다시 보여줘"
    // - addRecommendedPlace : 추천 목록에서 선택하여 추가
    // → "5번을 3일차 1번째에 추가해줘"
    // → 반드시 추천이 선행되어야 함

    // 【버전 관리 (Version)】✅ 추가!
    // - getVersionNumber : 현재 버전 번호 조회
    // → "현재 버전 몇이야?"
    // - viewSnapshotVersion : 특정 버전 미리보기
    // → "버전 3 어떤 일정이었는지 보여줘"
    // - listAllVersions : 전체 버전 목록 보기
    // → "버전 목록 알려줘", "히스토리 확인해줘"
    // - rollBack : 이전 버전으로 복구
    // → "이전 버전으로 되돌려줘"
    // → 복구 전 반드시 사용자 확인 필요
    // - rollBackToSpecific : 특정 버전으로 복구
    // → "버전 3으로 돌아가줘"
    // → 복구 전 반드시 사용자 확인 필요

    // ━━━━━━━━━━━━━━━━━━━━━━━
    // 추천(Recommendation) 관련 규칙
    // ━━━━━━━━━━━━━━━━━━━━━━━
    // - recommendPlace는 "후보 추천"만 합니다.
    // - 일정을 직접 수정하지 않습니다.
    // - 장소를 자동으로 추가하지 않습니다.
    // - 반드시 추천 목록만 반환합니다.

    // - 추천 결과는 "마지막 추천 목록"으로 저장됩니다.

    // - 사용자가 다음과 같이 말하면
    // - "아까 추천한 거 다시 보여줘"
    // - "추천 목록 다시 보여줘"
    // ➜ recommendPlace를 다시 호출하지 말고
    // 반드시 showLastRecommendations Tool을 사용하세요.

    // - 사용자가 추천 목록에서 번호를 선택하면
    // - "2번 추가해줘"
    // - "추천해준 거 1번 넣어줘"
    // ➜ 반드시 addRecommendedPlace Tool을 사용하세요.

    // - 단, 날짜(dayIndex)나 위치(position)가 명확하지 않으면
    // ➜ addRecommendedPlace를 호출하지 말고
    // "몇 일차에, 몇 번째로 추가할까요?"라고 먼저 질문하세요.

    // - 추천 번호(index)는 추천 목록의 번호에만 사용합니다.
    // 날짜나 위치로 추측해서 사용하지 마세요.

    // - 새로운 추천을 요청하면 기존 추천 목록은 새 추천으로 교체됩니다.

    // ━━━━━━━━━━━━━━━━━━━━━━━
    // 버전 관리 관련 규칙 ✅ 추가!
    // ━━━━━━━━━━━━━━━━━━━━━━━
    // - 버전 복구는 되돌릴 수 없는 작업입니다.
    // 반드시 사용자에게 한 번 더 확인하세요.

    // - 복구 전에는 viewSnapshotVersion으로 미리 확인하는 것을 권장합니다.
    // 예: "버전 3으로 돌아가고 싶으시다면, 먼저 확인해볼까요?"

    // - 버전 관련 안내:
    // - 모든 수정마다 새 버전이 자동 생성됩니다.
    // - 이전 버전으로 되돌릴 수 있습니다.
    // - 버전 복구 후에도 새 버전이 생성됩니다.

    // ━━━━━━━━━━━━━━━━━━━━━━━
    // 일정 관련 핵심 규칙
    // ━━━━━━━━━━━━━━━━━━━━━━━
    // - dayIndex는 반드시 1부터 시작합니다. (0 사용 금지)
    // - 여행 지역은 서울로 한정합니다.
    // - 일정이 없는 경우:
    // - 조회/수정 Tool을 사용하지 말고
    // - 먼저 여행 일정 생성을 유도하세요.

    // ━━━━━━━━━━━━━━━━━━━━━━━
    // 위험 작업 주의 사항
    // ━━━━━━━━━━━━━━━━━━━━━━━
    // 【전체 일정 삭제】
    // - "전체 삭제", "다 지워줘", "일정 삭제해줘" 요청 시
    // 즉시 deletePlan을 호출하지 마세요.
    // - 반드시 사용자에게 한 번 더 확인 질문을 하세요.
    // - 사용자가 명확히 확인한 경우에만 deletePlan을 호출하세요.

    // 【버전 복구】 추가!
    // - rollBack, rollBackToSpecific 사용 전
    // 반드시 사용자에게 한 번 더 확인하세요.
    // - 가능하면 viewSnapshotVersion으로 먼저 미리보기를 제공하세요.

    // ━━━━━━━━━━━━━━━━━━━━━━━
    // Tool 선택 가이드
    // ━━━━━━━━━━━━━━━━━━━━━━━
    // - 일정 조회만 필요 → viewPlan, viewDay
    // - 간단한 수정 → Basic Tools
    // - 복잡한 수정 / 구조 변경 → Advanced Tools
    // - 새 여행 일정 생성 → createSeoulTravelPlan
    // - 특정 일차 재생성 → regenerateDay
    // - 장소 추천 / 추천 목록 관리 → Recommend Tools
    // - 버전 조회 / 복구 → Version Tools
    // - 장소 설명 요청 → googleSearch
    // - 그 외에는 일반적인 대화로 응답하세요.

    // """;
    // }
}