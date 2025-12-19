package com.example.demo.planner.plan.agent.tools;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.planner.plan.agent.common.PlanToolSupport;
import com.example.demo.planner.plan.service.action.PlanAddAction;
import com.example.demo.planner.plan.service.action.PlanDeleteAction;
import com.example.demo.planner.plan.service.action.PlanModifyAction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component("planBasicTools")
@RequiredArgsConstructor
@Slf4j
public class PlanBasicTools {

    private final PlanToolSupport support;
    private final PlanDeleteAction deleteAction;
    private final PlanAddAction addAction;
    private final PlanModifyAction modifyAction;

    // ===============================
    // 장소 삭제
    // ===============================
    @Transactional
    @Tool(description = """
            이 Tool은 사용자의 여행 일정(plan)에서 특정 장소를 삭제할 때 사용합니다.

            사용 조건:
            - 사용자가 '삭제', '지워', '빼줘', '제거' 등의 표현으로
              특정 장소를 일정에서 없애달라고 요청했을 때만 사용하세요.
            - 장소명이 명확하지 않거나 여러 후보가 있을 경우,
              이 Tool을 호출하지 말고 먼저 사용자에게 확인 질문을 하세요.
            - 사용자가 삭제 의사를 명확히 확인한 경우에만 실행하세요.

            입력:
            - placeName: 일정에 등록된 장소의 이름 (사용자 발화에서 그대로 추출)

            주의:
            - 이 Tool은 실제로 데이터를 삭제합니다.
            - 실행 후에는 되돌릴 수 없으므로 반드시 사용자 확인이 필요합니다.
        """)
    public String deletePlace(
            String placeName,
            ToolContext toolContext) {

        String conversationId = getConversationId(toolContext);
        Long planId = support.getPlanId(conversationId);

        log.info("🔧 [Tool] deletePlace: planId={}, placeName={}", planId, placeName);

        try {
            deleteAction.deletePlaceByName(planId, placeName);
            Integer version = support.saveSnapshot(planId);

            return String.format(
                    "✅ '%s' 장소를 일정에서 삭제했습니다. 버전: %d",
                    placeName, version);

        } catch (IllegalArgumentException e) {
            return String.format("❌ '%s' 장소를 찾을 수 없습니다.", placeName);
        } catch (Exception e) {
            log.error("장소 삭제 실패", e);
            return String.format("❌ 장소 삭제 중 오류 발생: %s", e.getMessage());
        }
    }

    // ===============================
    // 장소 교체
    // ===============================
    @Transactional
    @Tool(description = "기존 장소를 다른 장소로 교체합니다")
    public String replacePlace(
            String oldPlaceName,
            String newPlaceName,
            ToolContext toolContext) {

        String conversationId = getConversationId(toolContext);
        Long planId = support.getPlanId(conversationId);

        log.info("🔧 [Tool] replacePlace: planId={}, old={}, new={}",
                planId, oldPlaceName, newPlaceName);

        try {
            String newName = modifyAction.replacePlaceWithSearch(
                    planId, oldPlaceName, newPlaceName);

            Integer version = support.saveSnapshot(planId);

            return String.format(
                    "✅ '%s'를 '%s'(으)로 변경했습니다. 버전: %d",
                    oldPlaceName, newName, version);

        } catch (Exception e) {
            log.error("장소 교체 실패", e);
            return String.format("❌ 장소 교체 중 오류 발생: %s", e.getMessage());
        }
    }

    // ===============================
    // 날짜 삭제
    // ===============================
    @Transactional
    @Tool(description = "특정 날짜 전체를 삭제합니다 (dayIndex는 1부터 시작)")
    public String deleteDay(
            int dayIndex,
            ToolContext toolContext) {

        String conversationId = getConversationId(toolContext);
        Long planId = support.getPlanId(conversationId);

        log.info("🔧 [Tool] deleteDay: planId={}, dayIndex={}", planId, dayIndex);

        try {
            deleteAction.deleteDay(planId, dayIndex);
            Integer version = support.saveSnapshot(planId);

            return String.format(
                    "✅ %d일차 일정을 삭제했습니다. 버전: %d",
                    dayIndex, version);

        } catch (Exception e) {
            log.error("날짜 삭제 실패", e);
            return String.format("❌ 날짜 삭제 중 오류 발생: %s", e.getMessage());
        }
    }

    // ===============================
    // 공통: conversationId
    // ===============================
    private String getConversationId(
            ToolContext toolContext) {

        return (String) toolContext
                .getContext()
                .get("conversationId");
    }
}
