package com.example.demo.planner.plan.agent.tools;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.example.demo.planner.plan.agent.TravelPlanAgent;
import com.example.demo.planner.plan.agent.common.PlanToolSupport;
import com.example.demo.planner.plan.dto.entity.GeneratedTravelPlan;
import com.example.demo.planner.plan.service.PlanSnapshotService;
import com.example.demo.planner.plan.service.TravelPlanSaveService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component("planCreateTools")
@RequiredArgsConstructor
@Slf4j
public class PlanCreateTools {

    private final TravelPlanAgent travelPlanAgent; // ← 실제 생성 로직
    private final TravelPlanSaveService travelPlanSaveService;
    private final PlanSnapshotService planSnapshotService;

    private final PlanToolSupport support;

    @Tool(name = "createSeoulTravelPlan", description = """
            서울 여행 일정을 자동으로 생성합니다.
            사용자가 "N박N일 계획 짜줘", "여행 일정 만들어줘"라고 요청할 때 사용하세요.
            사용자의 요청에서 다음 정보를 추출하여 전달하세요:
            - duration: 여행 기간 (필수)
            - style: 여행 스타일 (선택)
            - location: 선호 지역 (선택)
            - pace: 일정 강도 (선택)
            - startDateText: 여행 시작 시점 (선택)

            예시:
            "3일 kpop 강남 여행"
            → duration=3, style="kpop", location="강남", pace=null

            "서울 당일치기"
            → duration=1, style=null, location=null, pace=null

            "5일 힐링 여행 빡빡하게"
            → duration=5, style="힐링", location=null, pace="빡빡"

            주의:
            - 새로운 여행 계획을 생성할 때만 사용하세요.
            - 여행 기간이 없다면 사용자에게 다시 물어보세요.
            """)
    public String createTravelPlan(
            @ToolParam(description = "여행 기간(일). 반드시 사용자 발화에 명시되어야 함", required = true) Integer duration,
            @ToolParam(description = "여행 스타일. 예: 'kpop', '힐링'. 없으면 null", required = false) String style,
            @ToolParam(description = "선호 지역. 예: '강남', '강남, 홍대'. 없으면 null", required = false) String location,
            @ToolParam(description = "일정 강도. '빡빡', '널널'. 없으면 null", required = false) String pace,
            @ToolParam(description = "사용자가 말한 여행 시작 시점의 원문 표현 그대로. 예: '3일뒤', '다음주 월요일'", required = false) String startDateText,
            ToolContext toolContext) {

        log.info("🗼 [Tool] 서울 여행 일정 생성 시작");
        log.info("   파라미터: duration={}, style={}, location={}, pace={}",
                duration, style, location, pace);

        try {
            // 1. Validation
            if (duration == null || duration <= 0) {
                return "여행 기간을 지정해주세요. 며칠 동안 여행하실 예정인가요?";
            }

            Long userId = (Long) toolContext.getContext().get("userId");
            String conversationId = String.valueOf(
                    toolContext.getContext().get("conversationId"));

            // 2. 새 일정 생성 = conversation 상태 리셋
            support.clear(conversationId);
            log.info("🧹 [초기화] 새 일정 생성으로 conversation 상태 초기화");

            // 3. Agent 호출
            GeneratedTravelPlan plan = travelPlanAgent.createSeoulTravelPlanStructured(
                    duration, style, location, pace, startDateText);

            if (plan.days().isEmpty()) {
                return "일정 생성에 실패했습니다. 다시 시도해주세요.";
            }

            log.info("일정 생성 완료 - {}일", plan.days().size());

            // 4. DB 저장
            Long planId = travelPlanSaveService.save(userId, plan, new ObjectMapper());

            // 5. conversationId ↔ planId 연결 (가장 중요)
            support.setPlanId(conversationId, planId);
            log.info("conversationId={} → planId={} 연결 완료", conversationId, planId);

            // 6. 스냅샷 정리 & 저장
            support.deleteAllSnapshot(userId);
            Integer versionNo = support.saveSnapshot(planId);

            // 7. 렌더링
            return renderPlan(plan, versionNo);

        } catch (Exception e) {
            log.error("서울 여행 일정 생성 실패", e);
            return "일정 생성 중 오류가 발생했습니다: " + e.getMessage();
        }
    }

    /**
     * GeneratedTravelPlan 렌더링
     */
    private String renderPlan(GeneratedTravelPlan plan, Integer vesionNo) {
        StringBuilder sb = new StringBuilder();
        sb.append("version: " + vesionNo);
        sb.append(" \n");
        sb.append("=== Seoul Travel Itinerary ===\n");
        sb.append("📅 ")
                .append(plan.startDate())
                .append(" ~ ")
                .append(plan.endDate())
                .append("\n");
        sb.append("⏱️ Pace: ").append(plan.pace()).append("\n\n");

        for (var day : plan.days()) {
            sb.append("Day ").append(day.dayIndex()).append("\n");

            for (var p : day.places()) {
                sb.append("  ")
                        // .append(p.startAt().toLocalTime())
                        // .append("-")
                        // .append(p.endAt().toLocalTime())
                        // .append(" ")
                        .append(p.title())
                        .append("\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }
}