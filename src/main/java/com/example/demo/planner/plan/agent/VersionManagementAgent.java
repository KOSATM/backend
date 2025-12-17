package com.example.demo.planner.plan.agent;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.example.demo.planner.plan.agent.tools.VersionManagementTools;

import lombok.extern.slf4j.Slf4j;

/**
 * 버전 관리 전문 에이전트
 * - 일정 변경 이력 롤백
 * - SmartPlanAgent (LLM call) → VersionManagementAgent (순수 Java) → VersionManagementTools
 */
@Component
@Slf4j
public class VersionManagementAgent {

    private final VersionManagementTools versionManagementTools;

    public VersionManagementAgent(VersionManagementTools versionManagementTools) {
        this.versionManagementTools = versionManagementTools;
    }

    /**
     * ✅ SmartPlanAgent에서 호출되는 @Tool 메서드
     * 이전 버전으로 롤백 (직접 실행, LLM 호출 없음)
     */
    @Tool(description = "여행 일정을 바로 이전 버전으로 되돌립니다")
    public String rollBack(
            @ToolParam(description = "현재 planId") Long planId,
            @ToolParam(description = "Tool Context") ToolContext toolContext) {
        log.info("⏮️ [VersionManagementAgent @Tool] 이전 버전 롤백: planId={}", planId);
        return versionManagementTools.rollBack(planId, toolContext);
    }

    /**
     * ✅ SmartPlanAgent에서 호출되는 @Tool 메서드
     * 특정 버전으로 롤백 (직접 실행, LLM 호출 없음)
     */
    @Tool(description = "여행 일정을 특정 버전 번호로 되돌립니다")
    public String rollBackToSpecific(
            @ToolParam(description = "현재 planId") Long planId,
            @ToolParam(description = "돌아갈 버전 번호") Integer versionNo,
            @ToolParam(description = "Tool Context") ToolContext toolContext) {
        log.info("⏮️ [VersionManagementAgent @Tool] 특정 버전 롤백: planId={}, version={}",
                planId, versionNo);
        return versionManagementTools.rollBackToSpecific(planId, versionNo, toolContext);
    }
}
