package com.example.demo.planner.plan.service.action;

import org.springframework.stereotype.Component;

import com.example.demo.planner.plan.service.PlanCrudService;
import com.example.demo.planner.plan.service.PlanDayService;
import com.example.demo.planner.plan.service.PlanPlaceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AI 전용 - 장소/날짜 삭제 서비스
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
     * 장소 삭제 (특정 날, 장소 삭제)
     */
    public void deletePlace(Long planId, int dayIndex, int placeIndex) {
        placeService.deletePlace(planId, dayIndex, placeIndex);
    }

    /**
     * 날짜 삭제
     */
    public void deleteDay(Long planId, int dayIndex) {
        dayService.deleteDay(planId, dayIndex);
    }

    /**
     * 전체 일정 삭제 (Plan 포함)
     */
    public void deleteAllDaysAndPlaces(Long planId) {
        log.info("🗑️ 전체 일정 삭제 요청: planId={}", planId);
        crudService.deletePlan(planId);
        log.info("✅ Plan 완전 삭제 완료: planId={}", planId);
    }
}
