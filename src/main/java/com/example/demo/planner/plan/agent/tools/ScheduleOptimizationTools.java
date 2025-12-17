package com.example.demo.planner.plan.agent.tools;

import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import com.example.demo.planner.plan.dao.PlanDao;
import com.example.demo.planner.plan.dao.PlanDayDao;
import com.example.demo.planner.plan.dao.PlanPlaceDao;
import com.example.demo.planner.plan.dto.entity.Plan;
import com.example.demo.planner.plan.dto.entity.PlanDay;
import com.example.demo.planner.plan.dto.entity.PlanPlace;
import com.example.demo.planner.plan.service.PlanSnapshotService;
import com.example.demo.planner.plan.service.action.PlanAddAction;
import com.example.demo.planner.plan.service.action.PlanSwapAction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 일정 최적화 도구 모음
 * - ScheduleOptimizationAgent가 사용
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduleOptimizationTools {

    private final PlanSwapAction swapAction;
    private final PlanAddAction addAction;
    private final PlanDao planDao;
    private final PlanDayDao planDayDao;
    private final PlanPlaceDao planPlaceDao;
    private final PlanSnapshotService planSnapshotService;

    @Tool(description = "같은 날짜 내에서 두 장소의 순서를 교환합니다 (dayIndex는 1부터 시작)")
    public String swapPlacesInSameDay(Long planId, Integer dayIndex, Integer index1, Integer index2) {
        log.info("🔧 [Tool] swapPlacesInSameDay: planId={}, day={}, idx1={}, idx2={}",
                planId, dayIndex, index1, index2);
        try {
            swapAction.swapPlacesInSameDay(planId, dayIndex, index1, index2);

            // 스냅샷 저장
            saveSnapshot(planId);

            return String.format("✅ %d일차의 %d번째와 %d번째 장소 순서를 교환했습니다.", dayIndex, index1, index2);
        } catch (Exception e) {
            log.error("❌ 장소 순서 교환 실패", e);
            return String.format("❌ 순서 교환 실패: %s", e.getMessage());
        }
    }

    @Tool(description = "서로 다른 날짜 간 장소를 교환합니다 (dayIndex는 1부터 시작)")
    public String swapPlacesBetweenDifferentDays(Long planId, Integer day1, Integer index1, Integer day2, Integer index2) {
        log.info("🔧 [Tool] swapPlacesBetweenDifferentDays: planId={}, day1={}, idx1={}, day2={}, idx2={}",
                planId, day1, index1, day2, index2);
        try {
            swapAction.swapPlacesBetweenDays(planId, day1, index1, day2, index2);

            // 스냅샷 저장
            saveSnapshot(planId);

            return String.format("✅ %d일차의 %d번째 장소와 %d일차의 %d번째 장소를 교환했습니다.",
                    day1, index1, day2, index2);
        } catch (Exception e) {
            log.error("❌ 날짜 간 장소 교환 실패", e);
            return String.format("❌ 교환 실패: %s", e.getMessage());
        }
    }

    @Tool(description = "여행 기간을 연장합니다 (일수 추가)")
    public String extendTravelPlan(Long planId, Integer extraDays) {
        log.info("🔧 [Tool] extendTravelPlan: planId={}, extraDays={}", planId, extraDays);
        try {
            addAction.extendPlan(planId, extraDays);

            // 스냅샷 저장
            saveSnapshot(planId);

            Plan plan = planDao.selectPlanById(planId);
            long totalDays = java.time.temporal.ChronoUnit.DAYS.between(
                plan.getStartDate(), plan.getEndDate()) + 1;

            return String.format("✅ 여행 기간을 %d일 연장했습니다. 현재 %d일 여행입니다.", extraDays, totalDays);
        } catch (Exception e) {
            log.error("❌ 여행 기간 확장 실패", e);
            return String.format("❌ 기간 확장 실패: %s", e.getMessage());
        }
    }

    private void saveSnapshot(Long planId) {
        try {
            Plan plan = planDao.selectPlanById(planId);
            List<PlanDay> planDays = planDayDao.selectPlanDaysByPlanId(planId);
            List<PlanPlace> planPlaces = planPlaceDao.selectPlanPlacesByPlanId(planId);
            planSnapshotService.savePlanSnapshot(plan, planDays, planPlaces);
        } catch (Exception e) {
            log.error("❌ 스냅샷 저장 실패", e);
        }
    }
}
