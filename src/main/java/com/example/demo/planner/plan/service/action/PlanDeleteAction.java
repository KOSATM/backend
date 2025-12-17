package com.example.demo.planner.plan.service.action;

import org.springframework.stereotype.Component;

import com.example.demo.planner.plan.service.PlanCrudService;
import com.example.demo.planner.plan.service.PlanDayService;
import com.example.demo.planner.plan.service.PlanPlaceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AI 전용 - 장소/날짜 삭제 서비스
 * 순수하게 삭제 로직만 담당
 * 스냅샷은 Agent 단에서 처리
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PlanDeleteAction {

    private final PlanPlaceService placeService;
    private final PlanDayService dayService;
    private final PlanCrudService crudService;

    /**
     * 장소 삭제 (이름으로)
     */
    public void deletePlaceByName(Long planId, String placeName) {
        placeService.deletePlaceByName(planId, placeName);
    }

    /**
     * 특정 위치의 장소 삭제 (day + position)
     */
    public void deletePlaceByPosition(Long planId, Integer dayIndex, Integer position) {
        log.info("🗑️ [PlanDeleteAction] deletePlaceByPosition: planId={}, dayIndex={}, position={}", 
            planId, dayIndex, position);
        placeService.deletePlaceByPosition(planId, dayIndex, position);
    }

    /**
     * 날짜 삭제 (dayIndex로)
     */
    public void deleteDay(Long planId, int dayIndex) {
        log.info("🗑️ [PlanDeleteAction] deleteDay: planId={}, dayIndex={}", planId, dayIndex);
        dayService.deleteDay(planId, dayIndex);
    }

    /**
     * 전체 일정 삭제 (Plan 포함)
     */
    public void deleteAllDaysAndPlaces(Long planId) {
        log.info("🗑️ [PlanDeleteAction] deleteAllDaysAndPlaces: planId={}", planId);
        crudService.deletePlan(planId);
        log.info("✅ Plan 완전 삭제 완료: planId={}", planId);
    }
}


