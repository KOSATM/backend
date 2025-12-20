package com.example.demo.planner.plan.agent.tools;

import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.example.demo.planner.plan.agent.common.PlanToolSupport;
import com.example.demo.planner.plan.dto.entity.Plan;
import com.example.demo.planner.plan.dto.entity.PlanDay;
import com.example.demo.planner.plan.dto.entity.PlanPlace;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component("planViewTools")
@RequiredArgsConstructor
@Slf4j
public class PlanViewTools {

    private final PlanToolSupport support;

    @Tool(description = """
                현재 여행 일정 전체를 조회합니다.

                사용 예:
                - 전체 일정 알려줘
                - 내 여행일정에 대해서 알려줘
            """)
    public String viewPlan(ToolContext toolContext) {

        String conversationId = getConversationId(toolContext);
        Long planId = support.getPlanId(conversationId);
        log.info("🧭 [일정 조회] conversationId={}, planId={}", conversationId, planId);
        support.clearAddCandidateState(conversationId);
        log.info("🧹 [viewPlan] 장소 후보 상태 클리어 (추천 유지)");

        // 일정이 없는 경우
        if (planId == null) {
            return """
                    📭 아직 생성된 여행 일정이 없습니다.

                    원하시면
                    - "서울 2박 3일 여행 만들어줘"
                    - "당일치기 일정 만들어줘"

                    처럼 말씀해 주세요!
                    """;
        }

        try {
            Plan plan = support.loadPlan(planId);
            List<PlanDay> days = support.loadDays(planId);
            Map<Long, List<PlanPlace>> placesByDayId = support.loadPlacesByDayId(days);

            return support.renderPlan(plan, days, placesByDayId)
                    + "\n필요하시면 수정이나 추천도 도와드릴게요 😊";

        } catch (Exception e) {
            log.error("❌ 전체 일정 조회 실패", e);
            return "일정을 불러오는 중 오류가 발생했습니다.";
        }
    }

    @Tool(description = """
    특정 날짜의 일정을 조회합니다.

    ⚠️ 사용 조건:
    - 반드시 '보여줘', '확인', '일정' 같은 조회 의도가 포함되어야 합니다.

    ❌ 사용 금지:
    - 장소 추가/추천 중 사용자가 '2일차', '3일차'만 답한 경우
                        """)
    public String viewDay(
            @ToolParam(description = "조회할 일차 번호 (1부터 시작)") int dayIndex,
            ToolContext toolContext) {

        String conversationId = getConversationId(toolContext);
        Long planId = support.getPlanId(conversationId);

        PlanToolSupport.PendingSelectionType state = support.getPendingSelectionType(conversationId);

        log.info("🧭 [일차 조회 시도] conversationId={}, planId={}, dayIndex={}, state={}",
                conversationId, planId, dayIndex, state);

        /*
         * 추가/선택 흐름 중이면 조회 금지
         */
        if (state == PlanToolSupport.PendingSelectionType.ADD_CANDIDATE ||
                state == PlanToolSupport.PendingSelectionType.RECOMMENDATION) {

            log.info("⛔ viewDay 차단: 선택 흐름 중(state={})", state);

            // ❗️여기서 상태를 날리지 말고 그대로 둔다
            // → addPlace / addRecommendedPlace가 다시 호출되게 유도
            return "알겠습니다! 계속 진행할게요.";
            // 또는 null / 빈 문자열도 가능 (에이전트가 이전 Tool 재호출)
        }

        // ⬇️ 여기부터는 '진짜 조회'만 도달
        if (planId == null) {
            return """
                    📭 아직 여행 일정이 없습니다.

                    먼저 여행 일정을 만들어주세요!
                    예: "서울 2박 3일 여행 만들어줘"
                    """;
        }

        try {
            PlanDay day = support.loadDayByIndex(planId, dayIndex);
            List<PlanPlace> places = support.loadPlacesByDayId(day.getId());

            return support.renderDay(day, places);

        } catch (IllegalArgumentException e) {
            return e.getMessage();
        } catch (Exception e) {
            log.error("❌ 일차 조회 실패", e);
            return "해당 일차를 불러오는 중 오류가 발생했습니다.";
        }
    }

    // ===============================
    // Helper
    // ===============================
    private String getConversationId(ToolContext toolContext) {
        Object v = toolContext.getContext().get("conversationId");
        if (v == null) {
            throw new IllegalStateException("conversationId가 ToolContext에 없습니다.");
        }
        return String.valueOf(v);
    }
}
