package com.example.demo.planner.plan.service.action;

import org.springframework.stereotype.Component;

import com.example.demo.planner.plan.service.PlanDayService;
import com.example.demo.planner.plan.service.PlanPlaceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AI 전용 - 장소/날짜 교환 서비스
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PlanSwapAction {

    private final PlanPlaceService placeService;
    private final PlanDayService dayService;

    /**
     * 같은 날 장소 순서 교환
     */
    public void swapPlacesInSameDay(Long planId, int dayIndex, int index1, int index2) {
        placeService.swapPlaceOrdersInner(planId, dayIndex, index1, index2);
    }

    /**
     * 다른 날 장소 교환
     */
    public void swapPlacesBetweenDays(Long planId, int day1, int index1, int day2, int index2) {
        placeService.swapPlacesBetweenDays(planId, day1, index1, day2, index2);
    }

    /**
     * 날짜 전체 교환
     */
    public void swapDays(Long planId, int day1, int day2) {
        dayService.swapDaySchedules(planId, day1, day2);
    }
}
