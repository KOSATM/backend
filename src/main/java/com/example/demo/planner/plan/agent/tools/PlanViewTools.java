package com.example.demo.planner.plan.agent.tools;

import java.util.List;
import java.util.Map;

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

    @Tool(description = "현재 여행 일정 전체를 조회합니다")
    public String viewPlan() {
        Long planId = support.getPlanId();
        log.info("🔧 [Tool] viewPlan: planId={}", planId);

        try {
            Plan plan = support.loadPlan(planId);
            List<PlanDay> days = support.loadDays(planId);
            Map<Long, List<PlanPlace>> placesByDayId = support.loadPlacesByDayId(days);

            return support.renderPlan(plan, days, placesByDayId);

        } catch (Exception e) {
            log.error("일정 조회 실패", e);
            return "일정 조회 중 오류 발생: " + e.getMessage();
        }
    }

    @Tool(description = "특정 일차의 일정만 조회합니다. dayIndex는 1부터 시작 (1일차=1)")
    public String viewDay(
            @ToolParam(description = "조회할 일차 번호 (1부터 시작)") int dayIndex) {
        Long planId = support.getPlanId();
        log.info("🔧 [Tool] viewDay: planId={}, dayIndex={}", planId, dayIndex);

        try {
            PlanDay day = support.loadDayByIndex(planId, dayIndex);
            List<PlanPlace> places = support.loadPlacesByDayId(day.getId());

            return support.renderDay(day, places);

        } catch (Exception e) {
            log.error("일차 조회 실패", e);
            return "일차 조회 중 오류 발생: " + e.getMessage();
        }
    }

    @Tool(description = "현재 여행 일정 전체를 조회합니다")
    public String viewSnapShot() {
        Long planId = support.getPlanId();
        log.info("🔧 [Tool] viewPlan: planId={}", planId);

        try {
            Plan plan = support.loadPlan(planId);
            List<PlanDay> days = support.loadDays(planId);
            Map<Long, List<PlanPlace>> placesByDayId = support.loadPlacesByDayId(days);

            return support.renderPlan(plan, days, placesByDayId);

        } catch (Exception e) {
            log.error("일정 조회 실패", e);
            return "일정 조회 중 오류 발생: " + e.getMessage();
        }
    }
}