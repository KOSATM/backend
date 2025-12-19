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
                특정 일차(Day)의 일정만 조회합니다.

                이 Tool은 "조회" 전용입니다.
                일정을 수정하거나 추가하지 않습니다.

                사용 예:
                - "1일차 일정 보여줘"
                - "2일차 뭐야?"
                - "3일차 계획 알려줘"

                사용 조건:
                - dayIndex는 1부터 시작합니다. (1일차 = 1)
                - 여행 일정이 이미 존재해야 합니다.

                ❌ 사용하면 안 되는 경우:
                - "1일차에 장소 추가해줘" → addPlace 사용
                - "2일차 일정 바꿔줘" → 수정 Tool 사용
                - "오늘 뭐하지?" → recommendPlace 사용

                반환 내용:
                - 해당 일차의 날짜
                - 해당 일차에 등록된 장소 목록
                - 장소가 없을 경우 안내 메시지 반환
            """)
    public String viewDay(
            @ToolParam(description = "조회할 일차 번호 (1부터 시작)") int dayIndex,
            ToolContext toolContext) {

        String conversationId = getConversationId(toolContext);
        Long planId = support.getPlanId(conversationId);
        log.info("🧭 [일차 조회] conversationId={}, planId={}, dayIndex={}",
                conversationId, planId, dayIndex);

        support.clearAddCandidateState(conversationId);
        log.info("🧹 [viewPlan] 장소 후보 상태 클리어 (추천 유지)");

        // 일정이 없는 경우
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
            return e.getMessage(); // "n일차를 찾을 수 없습니다."
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
