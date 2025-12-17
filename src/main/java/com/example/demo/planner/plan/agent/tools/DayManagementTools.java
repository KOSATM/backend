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
import com.example.demo.planner.plan.dto.entity.PlanSnapshot;
import com.example.demo.planner.plan.service.PlanSnapshotService;
import com.example.demo.planner.plan.service.action.PlanDeleteAction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 날짜 관리 도구
 * - 실제 서비스 함수 호출
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DayManagementTools {

    private final PlanDeleteAction deleteAction;
    private final PlanDao planDao;
    private final PlanDayDao planDayDao;
    private final PlanPlaceDao planPlaceDao;
    private final PlanSnapshotService planSnapshotService;

    @Tool(description = "특정 날짜를 완전히 삭제합니다")
    public String deleteDay(Long planId, Integer dayIndex) {
        log.info("🔧 [DayManagementTools] deleteDay: planId={}, dayIndex={}", planId, dayIndex);

        try {
            deleteAction.deleteDay(planId, dayIndex);

            // 스냅샷 저장
            Plan plan = planDao.selectPlanById(planId);
            List<PlanDay> planDays = planDayDao.selectPlanDaysByPlanId(planId);
            List<PlanPlace> planPlaces = planPlaceDao.selectPlanPlacesByPlanId(planId);
            PlanSnapshot newSnapshot = planSnapshotService.savePlanSnapshot(plan, planDays, planPlaces);
            Integer versionNo = newSnapshot.getVersionNo();

            return String.format("✅ %d일차 일정을 삭제했습니다. 버전: %d", dayIndex, versionNo);
        } catch (Exception e) {
            log.error("날짜 삭제 실패", e);
            return String.format("❌ 날짜 삭제 중 오류 발생: %s", e.getMessage());
        }
    }
}
