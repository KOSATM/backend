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
import com.example.demo.planner.plan.service.action.PlanDeleteAction;
import com.example.demo.planner.plan.service.action.PlanModifyAction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 장소 관리 도구 모음
 * - PlaceManagementAgent가 사용
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PlaceManagementTools {

    private final PlanAddAction addAction;
    private final PlanDeleteAction deleteAction;
    private final PlanModifyAction modifyAction;
    private final PlanDao planDao;
    private final PlanDayDao planDayDao;
    private final PlanPlaceDao planPlaceDao;
    private final PlanSnapshotService planSnapshotService;

    @Tool(description = "네이버에서 장소를 검색하고 첫 번째 결과를 일정에 자동으로 추가합니다")
    public String searchAndAddPlace(Long planId, String placeName, Integer dayIndex, String startTime) {
        log.info("🔧 [Tool] searchAndAddPlace: planId={}, place={}, day={}, time={}",
                planId, placeName, dayIndex, startTime);
        try {
            // PlanAddAction.addPlace는 이미 네이버 검색 + 첫 번째 결과 추가를 수행함
            String actualName = addAction.addPlace(planId, dayIndex, placeName, startTime);

            // 스냅샷 저장
            saveSnapshot(planId);

            return String.format("✅ '%s'을(를) %d일차 일정에 추가했습니다.", actualName, dayIndex);
        } catch (Exception e) {
            log.error("❌ 장소 추가 실패", e);
            return String.format("❌ 장소 추가 실패: %s", e.getMessage());
        }
    }

    @Tool(description = "이미 선택된 장소를 일정에 추가합니다 (검색 없이 바로 추가)")
    public String confirmAddPlace(Long planId, String placeName, Integer dayIndex, String startTime) {
        log.info("🔧 [Tool] confirmAddPlace: planId={}, place={}, day={}, time={}",
                planId, placeName, dayIndex, startTime);
        try {
            // 검색 없이 바로 추가 (이미 사용자가 선택한 장소)
            addAction.addPlace(planId, dayIndex, placeName, startTime);

            // 스냅샷 저장
            saveSnapshot(planId);

            return String.format("✅ '%s'을(를) %d일차 일정에 추가했습니다.", placeName, dayIndex);
        } catch (Exception e) {
            log.error("❌ 장소 추가 실패", e);
            return String.format("❌ 장소 추가 실패: %s", e.getMessage());
        }
    }

    @Tool(description = "일정에서 장소를 삭제합니다")
    public String deletePlaceFromPlan(Long planId, String placeName) {
        log.info("🔧 [Tool] deletePlaceFromPlan: planId={}, place={}", planId, placeName);
        try {
            deleteAction.deletePlaceByName(planId, placeName);

            // 스냅샷 저장
            saveSnapshot(planId);

            return String.format("✅ '%s'을(를) 일정에서 삭제했습니다.", placeName);
        } catch (IllegalArgumentException e) {
            return String.format("❌ '%s'을(를) 찾을 수 없습니다.", placeName);
        } catch (Exception e) {
            log.error("❌ 장소 삭제 실패", e);
            return String.format("❌ 장소 삭제 실패: %s", e.getMessage());
        }
    }

    @Tool(description = "기존 장소를 새로운 장소로 교체합니다 (검색 후 첫 번째 결과 사용)")
    public String replacePlaceInPlan(Long planId, String oldPlaceName, String newPlaceName) {
        log.info("🔧 [Tool] replacePlaceInPlan: planId={}, old={}, new={}", planId, oldPlaceName, newPlaceName);
        try {
            String actualName = modifyAction.replacePlaceWithSearch(planId, oldPlaceName, newPlaceName);

            // 스냅샷 저장
            saveSnapshot(planId);

            return String.format("✅ '%s'를 '%s'로 변경했습니다.", oldPlaceName, actualName);
        } catch (Exception e) {
            log.error("❌ 장소 교체 실패", e);
            return String.format("❌ 장소 교체 실패: %s", e.getMessage());
        }
    }

    @Tool(description = "검색 결과에서 선택한 장소로 교체합니다")
    public String replacePlaceWithSelection(Long planId, String oldPlaceName, String newPlaceName, Integer selectedIndex) {
        log.info("🔧 [Tool] replacePlaceWithSelection: planId={}, old={}, new={}, index={}",
                planId, oldPlaceName, newPlaceName, selectedIndex);
        try {
            String actualName = modifyAction.replacePlaceWithSelection(planId, oldPlaceName, newPlaceName, selectedIndex);

            // 스냅샷 저장
            saveSnapshot(planId);

            return String.format("✅ '%s'를 '%s'로 변경했습니다.", oldPlaceName, actualName);
        } catch (Exception e) {
            log.error("❌ 장소 교체 실패", e);
            return String.format("❌ 장소 교체 실패: %s", e.getMessage());
        }
    }

    @Tool(description = "특정 위치에 장소를 삽입하고 이후 일정을 자동 조정합니다")
    public String addPlaceAtPosition(Long planId, Integer dayIndex, Integer position, String placeName, Integer duration) {
        log.info("🔧 [Tool] addPlaceAtPosition: planId={}, day={}, pos={}, place={}, duration={}",
                planId, dayIndex, position, placeName, duration);
        try {
            String actualName = addAction.addPlaceAtPosition(planId, dayIndex, position, placeName, duration);

            // 스냅샷 저장
            saveSnapshot(planId);

            return String.format("✅ %d일차 %d번째에 '%s'을(를) 추가했습니다.", dayIndex, position, actualName);
        } catch (Exception e) {
            log.error("❌ 장소 삽입 실패", e);
            return String.format("❌ 장소 삽입 실패: %s", e.getMessage());
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
