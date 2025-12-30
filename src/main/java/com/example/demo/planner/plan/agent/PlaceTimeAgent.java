package com.example.demo.planner.plan.agent;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import com.example.demo.planner.plan.agent.tools.PlaceTimeTools;

import lombok.extern.slf4j.Slf4j;

/**
 * 장소 시간 관리 전문 에이전트
 * - 장소의 방문 시간 변경
 * - SmartPlanAgent → PlaceTimeAgent → PlaceTimeTools
 */
@Component
@Slf4j
public class PlaceTimeAgent {

    private final PlaceTimeTools placeTimeTools;

    public PlaceTimeAgent(PlaceTimeTools placeTimeTools) {
        this.placeTimeTools = placeTimeTools;
    }

    /**
     * ✅ SmartPlanAgent에서 호출되는 @Tool 메서드
     * 장소의 방문 시간 변경
     */
    @Tool(description = "여행 일정에서 특정 장소의 방문 시간을 변경합니다")
    public String updatePlaceTime(Long planId, String placeName, String newTime) {
        log.info("⏰ [PlaceTimeAgent @Tool] 시간 변경: planId={}, place={}, time={}",
                planId, placeName, newTime);

        return placeTimeTools.updatePlaceTime(planId, placeName, newTime);
    }
}
