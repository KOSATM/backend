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
     * ✅ Guard: 특정 날짜 삭제 전 사용자 확인
     * 실제 삭제 전 확인 메시지 반환
     * ✨ planId는 PlanContextHolder에서만 읽음
     */
    @Tool(description = "여행 일정에서 특정 날짜를 삭제하기 전 사용자 확인을 구합니다.")
    public String deleteDayGuard(Integer dayIndex) {
        Long planId = com.example.demo.planner.plan.dto.context.PlanContextHolder.getPlanId();
        log.info("🔐 [DayManagementAgent Guard] 날짜 삭제 확인: planId={}, dayIndex={}", planId, dayIndex);

        return dayManagementTools.deleteDayGuard(planId, dayIndex);
    }

    /**
     * ✅ Confirm: 특정 날짜 실제 삭제 (NOT A @Tool - called by Java only)
     * 사용자 확인 이후 실제 삭제 수행
     * ✨ planId는 PlanContextHolder에서만 읽음
     */
    public String confirmDeleteDay(Integer dayIndex) {
        Long planId = com.example.demo.planner.plan.dto.context.PlanContextHolder.getPlanId();
        log.info("🛑 [DayManagementAgent Confirm] 날짜 실제 삭제: planId={}, dayIndex={}", planId, dayIndex);

        return dayManagementTools.confirmDeleteDay(planId, dayIndex);
    }

    /**
     * ✅ 기존 호환성: 단계 삭제 (Guard 없이 바로 실행)
     * SmartPlanAgent에서 호출되는 @Tool 메서드
     */
    @Tool(description = "여행 일정에서 특정 날짜를 완전히 삭제합니다 (dayIndex는 1부터 시작, 확인 없이 바로 삭제)")
    public String deleteDay(Long planId, Integer dayIndex) {
        log.info("🗑️ [DayManagementAgent @Tool] 날짜 삭제 (단계): planId={}, dayIndex={}", planId, dayIndex);

        return dayManagementTools.deleteDay(planId, dayIndex);
    }

    /**
     * ✅ Guard: 전체 일정 삭제 전 확인
     * 실제 삭제 전 사용자 확인
     * ✨ planId는 PlanContextHolder에서만 읽음
     */
    @Tool(description = "여행 일정 전체를 삭제하기 전 사용자 확인을 구합니다.")
    public String deleteEntirePlan() {
        Long planId = com.example.demo.planner.plan.dto.context.PlanContextHolder.getPlanId();
        log.info("🔐 [DayManagementAgent Guard] 전체 일정 삭제 확인: planId={}", planId);

        return dayManagementTools.deleteEntirePlanGuard(planId);
    }

    /**
     * ✅ Confirm: 전체 일정 실제 삭제 (NOT A @Tool - called by Java only)
     * 사용자 확인 이후 실제 삭제 수행
     * ✨ planId는 PlanContextHolder에서만 읽음
     */
    public String confirmDeletePlan() {
        Long planId = com.example.demo.planner.plan.dto.context.PlanContextHolder.getPlanId();
        log.info("🛑 [DayManagementAgent Confirm] 전체 일정 실제 삭제: planId={}", planId);

        return dayManagementTools.confirmDeletePlan(planId);
    }
}
