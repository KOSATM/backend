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
import com.example.demo.planner.plan.service.action.PlanModifyAction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 장소 시간 관리 도구
 * - 실제 서비스 함수 호출
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PlaceTimeTools {

    private final PlanModifyAction modifyAction;
    private final PlanDao planDao;
    private final PlanDayDao planDayDao;
    private final PlanPlaceDao planPlaceDao;
    private final PlanSnapshotService planSnapshotService;

    @Tool(description = "특정 장소의 방문 시간을 변경합니다")
    public String updatePlaceTime(Long planId, String placeName, String newTime) {
        log.info("🔧 [PlaceTimeTools] updatePlaceTime: planId={}, place={}, time={}",
                planId, placeName, newTime);

        try {
            modifyAction.updatePlaceTime(planId, placeName, newTime);

            // 스냅샷 저장
            Plan plan = planDao.selectPlanById(planId);
            List<PlanDay> planDays = planDayDao.selectPlanDaysByPlanId(planId);
            List<PlanPlace> planPlaces = planPlaceDao.selectPlanPlacesByPlanId(planId);
            PlanSnapshot newSnapshot = planSnapshotService.savePlanSnapshot(plan, planDays, planPlaces);
            Integer versionNo = newSnapshot.getVersionNo();

            return String.format("✅ '%s'의 방문 시간을 %s로 변경했습니다. 버전: %d",
                    placeName, newTime, versionNo);
        } catch (Exception e) {
            log.error("시간 변경 실패", e);
            return String.format("❌ 시간 변경 중 오류 발생: %s", e.getMessage());
        }
    }
}
