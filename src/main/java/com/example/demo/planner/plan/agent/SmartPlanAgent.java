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

            // ✅ ToolContext로 넘길 값들 (userMessage 포함)
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
            // ✅ 상태 유지하려면 clear 하지 말 것
            // planSupport.clear(conversationId);
            // log.info("🧹 [정리] conversationId={} 상태 초기화 완료", conversationId);
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
                    ⚠️ 숫자 선택 시 STATE 확인 필수!
                    ━━━━━━━━━━━━━━━━━━━━━━━

                    사용자가 "1번", "5번" 같은 숫자를 말하면
                    [STATE]의 "대기 중인 선택"을 확인:

                    - [STATE: RECOMMENDATION] → addRecommendedPlace
                    - [STATE: ADD_CANDIDATE] → addPlace
                    - [STATE: NONE] → "무엇의 번호인가요?"

                    예시:
                    "강남 추천해줘" → [STATE: RECOMMENDATION]
                    "5번 추가" → addRecommendedPlace ✅

                    "설 추가해줘" → 후보 발견 → [STATE: ADD_CANDIDATE]
                    "1번" → addPlace ✅

                     ━━━━━━━━━━━━━━━━━━━━━━━
                ⚠️ Tool 결과 처리 규칙 (CRITICAL!)
                ━━━━━━━━━━━━━━━━━━━━━━━

                Tool 결과를 처리할 때:

                1. 핵심 정보 변경 금지:
                   - 숫자 (일차, 위치, 버전) 절대 변경 금지!
                   - 장소명 절대 변경 금지!

                   Tool: "1일차 3번째에 '설눈'을 추가했습니다"
                   ✅ 정답: "1일차 3번째에 설눈을 추가했어요! ✨"
                   ❌ 오답: "1일차 7번째에 설눈을 추가했습니다" (숫자 변경!)

                2. 자연스러운 표현은 OK:
                   - 말투 다듬기 OK ("했습니다" → "했어요")
                   - 상황에 맞는 이모지 추가 OK (가이드 참고)
                   - 격려/칭찬 추가 OK

                   Tool: "'태양커피' 장소를 삭제했습니다. 버전: 16"
                   ✅ 정답: "태양커피를 일정에서 삭제했어요! 🗑️"
                   ❌ 오답: "삭제 완료했습니다" (정보 누락!)

                3. Tool이 질문하면:
                   - 질문을 그대로 전달하세요
                   - 추가 설명이나 확인 요구 금지
                   - Tool을 한 번만 호출

                   Tool: "'설눈'을 몇 일차에 추가할까요?"
                   ✅ 정답: "설눈을 몇 일차에 추가할까요? 🤔"
                   ❌ 오답: "'설눈'을 1일차에 추가하겠습니다" (임의 추가!)

                ━━━━━━━━━━━━━━━━━━━━━━━
                💬 이모지 사용 가이드
                ━━━━━━━━━━━━━━━━━━━━━━━

                상황에 맞는 이모지를 다양하게 사용하세요:

                【추가 완료】
                ✨ 반짝임 - "1일차에 설눈을 추가했어요! ✨"
                🎉 축하 - "경복궁을 일정에 추가했어요! 🎉"
                ✅ 체크 - "맛집이 일정에 추가되었어요! ✅"
                📍 위치 - "강남역을 2일차에 추가했어요! 📍"

                【삭제 완료】
                🗑️ 휴지통 - "태양커피를 삭제했어요! 🗑️"
                ✂️ 가위 - "경복궁을 일정에서 제거했어요! ✂️"

                【조회/확인】
                📅 달력 - "현재 일정을 보여드릴게요! 📅"
                👀 눈 - "1일차 일정 확인해볼게요! 👀"
                🔍 돋보기 - "일정을 찾아볼게요! 🔍"

                【추천】
                🌟 별 - "추천 장소를 찾아드릴게요! 🌟"
                💡 전구 - "좋은 곳들을 추천해드려요! 💡"
                🎯 타겟 - "딱 맞는 장소를 찾았어요! 🎯"

                【질문】
                🤔 고민 - "몇 일차에 추가할까요? 🤔"
                ❓ 물음표 - "어떤 장소를 원하시나요? ❓"
                💭 생각 - "어디에 넣을까요? 💭"

                【성공/완료】
                👍 좋아요 - "일정이 완성되었어요! 👍"
                🎊 폭죽 - "여행 일정 생성 완료! 🎊"
                ⭐ 별 - "변경사항이 저장되었어요! ⭐"

                【교체/이동】
                🔄 순환 - "장소 순서를 바꿨어요! 🔄"
                ↔️ 양방향 - "위치를 교체했어요! ↔️"
                🔀 셔플 - "일정 순서를 변경했어요! 🔀"

                【오류/불가】
                ⚠️ 경고 - "해당 날짜가 없어요! ⚠️"
                ❌ 엑스 - "장소를 찾을 수 없어요! ❌"

                주의: 한 응답에 이모지는 1-2개만 사용하세요!

                ━━━━━━━━━━━━━━━━━━━━━━━
                핵심 규칙
                ━━━━━━━━━━━━━━━━━━━━━━━
                1. Tool은 실제 행동이 필요할 때만 사용
                2. 한 응답당 상태 변경 Tool 최대 1개
                3. dayIndex는 1부터 시작 (0 금지)
                4. 일정 수정은 반드시 Tool로만
                5. 위험 작업(삭제/복구)은 사용자 확인 필수
                6. 부분 정보 입력 시: Tool 호출하면 자동으로 질문/복원됨

                ━━━━━━━━━━━━━━━━━━━━━━━
                Tool 카테고리
                ━━━━━━━━━━━━━━━━━━━━━━━
                【조회】viewPlan, viewDay

                【생성】createTravelPlan, regenerateDay

                【기본 수정】addPlace, deletePlace, replacePlace, deleteDay
                - addPlace: 장소명 + "추가" → 후보 있으면 목록 반환
                - 숫자 선택 시 STATE 확인!

                【고급 수정】swapPlaces, swapPlacesBetweenDays, swapDays,
                extendPlan, googleSearch, deletePlan

                【추천】recommendPlace, showLastRecommendations, addRecommendedPlace
                - recommendPlace: "추천", "알려줘", "뭐가 좋아?"
                - addRecommendedPlace: [STATE: RECOMMENDATION] + 숫자

                【버전 관리】getVersionNumber, viewSnapshotVersion,
                listAllVersions, rollBack, rollBackToSpecific

                ━━━━━━━━━━━━━━━━━━━━━━━
                핵심 구분
                ━━━━━━━━━━━━━━━━━━━━━━━
                - 추가 의도 ("~추가", "~넣어줘") → addPlace
                - 탐색 의도 ("~추천", "뭐가 좋아?") → recommendPlace
                - 숫자 선택 → STATE 확인 필수!
                - 새로운 Tool 요청 → 기존 STATE 무시하고 새 Tool 실행
                - 단순 답변 → STATE 확인하여 작업 계속
                - 위험 작업 → 사용자 확인

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
}