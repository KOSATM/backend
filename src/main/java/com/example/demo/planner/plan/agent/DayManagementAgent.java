package com.example.demo.planner.plan.agent;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import com.example.demo.planner.plan.agent.tools.DayManagementTools;

import lombok.extern.slf4j.Slf4j;

/**
 * 날짜 관리 전문 에이전트
 * - 날짜 삭제 등 Day 레벨 작업 수행
 * - SmartPlanAgent → DayManagementAgent → DayManagementTools
 */
@Component
@Slf4j
public class DayManagementAgent {

    private final DayManagementTools dayManagementTools;

    public DayManagementAgent(DayManagementTools dayManagementTools) {
        this.dayManagementTools = dayManagementTools;
    }

    /**
     * ✅ SmartPlanAgent에서 호출되는 @Tool 메서드
     * 특정 날짜 삭제
     */
    @Tool(description = "여행 일정에서 특정 날짜를 완전히 삭제합니다 (dayIndex는 1부터 시작)")
    public String deleteDay(Long planId, Integer dayIndex) {
        log.info("🗑️ [DayManagementAgent @Tool] 날짜 삭제: planId={}, dayIndex={}", planId, dayIndex);

        return dayManagementTools.deleteDay(planId, dayIndex);
    }
}
