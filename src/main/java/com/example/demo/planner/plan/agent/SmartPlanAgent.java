package com.example.demo.planner.plan.agent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import com.example.demo.common.chat.intent.dto.IntentCommand;
import com.example.demo.common.chat.memory.service.MemoryRetrievalService;
import com.example.demo.common.chat.prompt.PromptBuilder;
import com.example.demo.common.chat.prompt.PromptContext;
import com.example.demo.common.chat.pipeline.AiAgentResponse;
import com.example.demo.common.global.agent.AiAgent;
import com.example.demo.planner.plan.dto.context.PlanContext;
import com.example.demo.planner.plan.dto.entity.Plan;
import com.example.demo.planner.plan.dto.entity.PendingAction;
import com.example.demo.planner.plan.guard.WriteToolGuard;
import com.example.demo.planner.plan.service.PlanCrudService;
import com.example.demo.planner.plan.service.PlanQueryService;
import com.example.demo.planner.plan.service.PendingActionService;
import com.example.demo.planner.plan.service.action.PlanDeleteAction;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SmartPlanAgent implements AiAgent {

    private final ChatClient chatClient;
    private final PromptBuilder promptBuilder;
    private final PlanQueryService queryService;
    private final PlanCrudService crudService;
    private final WriteToolGuard writeToolGuard;
    private final MemoryRetrievalService memoryRetrievalService;
    private final PendingActionService pendingActionService;
    private final PlanDeleteAction planDeleteAction;

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
            MemoryRetrievalService memoryRetrievalService,
            PendingActionService pendingActionService,
            PlanDeleteAction planDeleteAction,
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
        this.memoryRetrievalService = memoryRetrievalService;
        this.pendingActionService = pendingActionService;
        this.planDeleteAction = planDeleteAction;
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

        // ✅ PlanContextHolder 설정 (모든 Tool이 동일 planId 사용하도록)
        com.example.demo.planner.plan.dto.context.PlanContextHolder.set(ctx);
        
        try {
            // ✅ STEP 1: PendingAction 있으면 LLM이 확인 여부 판단
            PendingAction pending = pendingActionService.getLatest(userId);
            if (pending != null && !pending.isExpired()) {
                log.info("[SmartPlanAgent] PendingAction 감지: type={}, userId={}", pending.getType(), userId);
                
                // LLM에게 판단 위임: "이게 확인 응답인가요?"
                AiAgentResponse confirmationJudgment = judgeConfirmationByLLM(userMsg, pending, ctx);
                
                // LLM이 확인이라고 판단했으면 실행
                if (confirmationJudgment.getMessage().contains("YES") || 
                    confirmationJudgment.getMessage().contains("네") ||
                    confirmationJudgment.getMessage().contains("맞")) {
                    
                    log.info("[SmartPlanAgent] LLM 판단: 확인 응답 → handleConfirmation 실행");
                    AiAgentResponse result = handleConfirmation(pending, userId);
                    saveHistory(userId, result.getMessage());
                    return result;
                } else {
                    // LLM: 이건 확인이 아님 → PendingAction 초기화 + 새 명령 처리
                    log.info("[SmartPlanAgent] LLM 판단: 확인 아님 → PendingAction 초기화 후 새 명령 처리");
                    pendingActionService.clearAll(userId);
                    // 아래로 계속 진행 (새 명령 처리)
                }
            }

            // 1. ChatMemory 업데이트
            List<String> history = historyMap.computeIfAbsent(userId, k -> new ArrayList<>());
            history.add("User: " + userMsg);

            // 2. ✅ MemoryBundle 생성 (대화 맥락 포함)
            var memoryBundle = memoryRetrievalService.retrieveAll(userId, userMsg, null);

            // 3. PromptContext 생성 (memoryBundle 포함)
            PromptContext promptContext = PromptContext.builder()
                    .userId(userId)
                    .userMessage(userMsg)
                    .planContext(ctx)
                    .memoryBundle(memoryBundle)
                    .build();

            // 4. Prompt 생성
            Prompt prompt = promptBuilder.build(promptContext);

            // 5. LLM 호출 (모든 tool 등록)
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

            // 6. 응답 처리
            String content = response.content();
            saveHistory(userId, content);
            
            log.debug("[SmartPlanAgent] Response generated for user {}", userId);

            return AiAgentResponse.of(content);
        } finally {
            // ✅ PlanContextHolder 정리 (요청 완료 후)
            com.example.demo.planner.plan.dto.context.PlanContextHolder.clear();
        }
    }
    
    /**
     * ✅ LLM이 사용자의 응답이 PendingAction 확인인지 판단
     */
    private AiAgentResponse judgeConfirmationByLLM(String userMsg, PendingAction pending, PlanContext ctx) {
        String confirmationPrompt = String.format(
            """
            사용자가 '%s'라고 말했습니다.
            
            현재 대기 중인 작업:
            - 타입: %s
            - 내용: %s
            
            사용자의 응답이 이 작업에 대한 "확인(YES)"인지, 아니면 다른 말인지 판단하세요.
            
            답변 형식: YES (확인) 또는 NO (확인 아님)
            """,
            userMsg,
            pending.getType(),
            getPendingActionDescription(pending)
        );
        
        log.debug("[SmartPlanAgent] LLM 확인 판단 프롬프트: {}", confirmationPrompt);
        
        String judgment = chatClient.prompt(confirmationPrompt)
                .call()
                .content();
        
        log.info("[SmartPlanAgent] LLM 판단 결과: {}", judgment);
        
        return AiAgentResponse.of(judgment);
    }
    
    /**
     * PendingAction을 사람이 읽을 수 있는 형태로 변환
     */
    private String getPendingActionDescription(PendingAction pending) {
        return switch (pending.getType()) {
            case DELETE_PLACE -> "장소 '" + pending.getPlaceName() + "' 삭제";
            case DELETE_DAY -> pending.getDayIndex() + "일차 삭제";
            case DELETE_PLAN -> "전체 여행 일정 삭제";
            default -> "알 수 없는 작업";
        };
    }

    /* ================= Helper Methods ================= */

    // ============ 규칙 기반 감지 제거 - LLM 기반 처리로 이동 ============
    
    /**
     * ✅ 확인 응답 처리 (Java switch - LLM 아님)
     * PendingAction의 타입에 따라 실제 삭제 실행
     * 삭제 후 관련 일정 정보 반환
     */
    private AiAgentResponse handleConfirmation(PendingAction action, Long userId) {
        try {
            PlanContext ctx = loadContext(userId);
            
            return switch (action.getType()) {
                case DELETE_PLACE -> {
                    String placeName = action.getPlaceName();
                    Integer dayIndex = action.getDayIndex();
                    log.info("[SmartPlanAgent] Confirm 장소 삭제: {} ({}일차)", placeName, dayIndex);
                    
                    placeManagementAgent.confirmDeletePlace(placeName);
                    pendingActionService.complete(userId);
                    
                    // 삭제된 place가 포함된 day의 남은 일정 조회
                    String daySchedule = getFormattedDaySchedule(ctx, dayIndex);
                    String response = "✅ '" + placeName + "'을(를) 삭제했습니다.\n\n📅 " + dayIndex + "일차 일정:\n" + daySchedule;
                    yield AiAgentResponse.of(response);
                }
                
                case DELETE_DAY -> {
                    Integer dayIndex = action.getDayIndex();
                    log.info("[SmartPlanAgent] Confirm Day 삭제: {}", dayIndex);
                    
                    // 삭제 전 day 정보 캡처
                    String deletedDayInfo = getFormattedDaySchedule(ctx, dayIndex);
                    
                    dayManagementAgent.confirmDeleteDay(dayIndex);
                    pendingActionService.complete(userId);
                    
                    String response = "✅ " + dayIndex + "일차를 삭제했습니다.\n\n❌ 삭제된 " + dayIndex + "일차 일정:\n" + deletedDayInfo;
                    yield AiAgentResponse.of(response);
                }
                
                case DELETE_PLAN -> {
                    log.info("[SmartPlanAgent] Confirm 전체 일정 삭제: {}", userId);
                    dayManagementAgent.confirmDeletePlan();
                    pendingActionService.complete(userId);
                    yield AiAgentResponse.of("✅ 전체 여행 일정을 삭제했습니다.");
                }
                
                default -> AiAgentResponse.of("❌ 알 수 없는 작업입니다.");
            };
        } catch (Exception e) {
            log.error("[SmartPlanAgent] Confirm 처리 중 오류: {}", e.getMessage(), e);
            return AiAgentResponse.of("❌ 작업 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 특정 day의 일정을 포맷된 문자열로 반환
     */
    private String getFormattedDaySchedule(PlanContext ctx, Integer dayIndex) {
        try {
            if (ctx == null || ctx.getAllDays() == null) {
                return "(일정 정보 없음)";
            }
            
            var day = ctx.getAllDays().stream()
                    .filter(d -> d.getDay() != null && d.getDay().getDayIndex() != null && d.getDay().getDayIndex().equals(dayIndex))
                    .findFirst()
                    .orElse(null);
            
            if (day == null) {
                log.warn("[SmartPlanAgent] Day {} 를 찾을 수 없음", dayIndex);
                return String.format("(%d일차 일정 없음)", dayIndex);
            }
            
            StringBuilder sb = new StringBuilder();
            var places = day.getPlaces();
            if (places == null || places.isEmpty()) {
                sb.append("(비어있음)");
            } else {
                for (var place : places) {
                    sb.append("• ").append(place.getPlaceName());
                    if (place.getStartAt() != null && place.getEndAt() != null) {
                        sb.append(" (").append(place.getStartAt()).append(" ~ ").append(place.getEndAt()).append(")");
                    }
                    sb.append("\n");
                }
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("[SmartPlanAgent] Day 일정 조회 실패 dayIndex={}: {}", dayIndex, e.getMessage());
            return String.format("(%d일차 일정 조회 실패)", dayIndex);
        }
    }

    /**
     * ChatMemory에 Assistant 응답 저장 (computeIfAbsent 방식 - 가장 안전)
     */
    private void saveHistory(Long userId, String answer) {
        historyMap.computeIfAbsent(userId, k -> new ArrayList<>())
                  .add("Assistant: " + answer);
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
}
