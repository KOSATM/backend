package com.example.demo.planner.plan.agent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import com.example.demo.common.chat.intent.dto.IntentCommand;
import com.example.demo.common.chat.prompt.PromptBuilder;
import com.example.demo.common.chat.prompt.PromptContext;
import com.example.demo.common.chat.pipeline.AiAgentResponse;
import com.example.demo.common.global.agent.AiAgent;
import com.example.demo.planner.plan.dto.context.PlanContext;
import com.example.demo.planner.plan.dto.entity.Plan;
import com.example.demo.planner.plan.guard.WriteToolGuard;
import com.example.demo.planner.plan.service.PlanCrudService;
import com.example.demo.planner.plan.service.PlanQueryService;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SmartPlanAgent implements AiAgent {

    private final ChatClient chatClient;
    private final PromptBuilder promptBuilder;
    private final PlanQueryService queryService;
    private final PlanCrudService crudService;
    private final WriteToolGuard writeToolGuard;

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
            PromptBuilder promptBuilder,
            PlanQueryService queryService,
            PlanCrudService crudService,
            WriteToolGuard writeToolGuard,
            TravelPlanAgent travelPlanAgent,
            SearchPlaceAgent searchPlaceAgent,
            PlaceManagementAgent placeManagementAgent,
            ScheduleOptimizationAgent scheduleOptimizationAgent,
            DayManagementAgent dayManagementAgent,
            PlaceTimeAgent placeTimeAgent,
            VersionManagementAgent versionManagementAgent
    ) {
        this.chatClient = builder.build();
        this.promptBuilder = promptBuilder;
        this.queryService = queryService;
        this.crudService = crudService;
        this.writeToolGuard = writeToolGuard;
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

    /**
     * 🎯 SmartPlanAgent 핵심 실행 로직
     * 
     * 설계 원칙:
     * 1. chatMemory가 상태의 중심 (PendingAction 불필요)
     * 2. hasToolCalls()로 턴 유형 구분
     * 3. 탐색 → 질문 → 확정 → 쓰기 순서 유도
     * 
     * PendingAction을 사용하지 않는 이유:
     * - chatMemory에 충분한 컨텍스트 포함
     * - 사용자 확인은 LLM의 자연스러운 질문으로 처리
     * - 대부분의 작업이 2턴 이내 완료
     * - 명시적 상태 관리보다 대화 흐름 우선
     */
    private AiAgentResponse executeInternal(String userMsg, Long userId) {
        log.info("[SmartPlanAgent] User({}): {}", userId, userMsg);

        PlanContext ctx = loadContext(userId);

        // ✅ Plan이 없는 경우 → 일정 생성
        if (!ctx.hasActivePlan()) {
            log.info("[SmartPlanAgent] 활성 Plan 없음 → TravelPlanAgent 호출");
            return handlePlanCreation(userMsg, userId);
        }

        // ✅ Plan이 있는 경우 → 일정 관리
        return handlePlanManagement(userMsg, userId, ctx);
    }

    /**
     * Plan 생성 처리
     */
    private AiAgentResponse handlePlanCreation(String userMsg, Long userId) {
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

    /**
     * <p><strong>턴 유형:</strong>
     * <ul>
     *   <li>탐색 턴: hasToolCalls() = true, 읽기 tool (search*, query*)</li>
     *   <li>질문 턴: hasToolCalls() = false, tool 없음</li>
     *   <li>확정 턴: hasToolCalls() = true, 쓰기 tool (add*, update*, delete*, create*)</li>
     * </ul>
     * 
     * <p><strong>WriteToolGuard 통합 철학:</strong>
     * <ul>
     *   <li>LLM은 자유롭게 tool call 생성 (Agent는 제약 없음)</li>
     *   <li>실제 Tool 메서드 내부에서 Guard가 실행 관문 역할</li>
     *   <li>Guard가 막으면 → 확인 질문 반환 + chatMemory 저장</li>
     *   <li>다음 턴에서 isConfirmed=true로 재실행</li>
     * </ul>
     * 
     * <p><strong>중요:</strong>
     * Spring AI의 .call()은 tool을 자동 실행하므로,
     * Guard는 각 Tool Agent의 메서드 내부에서 체크한다.
     * SmartPlanAgent는 Guard를 소유만 하고, 실제 검증은 위임.
     * - 질문 턴: hasToolCalls() = false, tool 없음
     * - 확정 턴: hasToolCalls() = true, 쓰기 tool (add*, update*, delete*, create*)
     */
    private AiAgentResponse handlePlanManagement(String userMsg, Long userId, PlanContext ctx) {
        Long planId = ctx.getActivePlan().getId();

        // 1. ChatMemory 업데이트
        List<String> history = historyMap.computeIfAbsent(userId, k -> new ArrayList<>());
        history.add("User: " + userMsg);

        // 2. PromptContext 생성
        PromptContext promptContext = PromptContext.builder()
                .userId(userId)
                .userMessage(userMsg)
                .planContext(ctx)
                .build();

        // 3. Prompt 생성
        Prompt prompt = promptBuilder.build(promptContext);

        // 4. LLM 호출 (모든 tool 등록)
        var response = chatClient.prompt(prompt)
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
                .call();

        // 5. 응답 처리
        String content = response.content();
        saveHistory(userId, content);
        
        // 6. Tool 호출 로깅
        // 💡 LLM이 tool을 불렀는지는 로그로만 확인
        // 강제 검증은 하지 않음 (LLM의 자율성 보장)
        log.debug("[SmartPlanAgent] Response generated for user {}", userId);

        return AiAgentResponse.of(content);
    }

    /* ================= Helper Methods ================= */

    /**
     * ChatMemory에 Assistant 응답 저장
     */
    private void saveHistory(Long userId, String answer) {
        historyMap.get(userId).add("Assistant: " + answer);
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
                            .activePlan(plan)
                            .allDays(queryService.queryAllDaysOptimized(plan.getId()))
                            .build();
        } catch (Exception e) {
            return PlanContext.empty();
        }
    }
}
