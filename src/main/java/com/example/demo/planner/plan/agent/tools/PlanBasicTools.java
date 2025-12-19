package com.example.demo.planner.plan.agent.tools;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
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
    @Tool(description = """
            이 Tool은 사용자의 여행 일정(plan)에 이미 등록된 장소를
            다른 장소로 교체할 때 사용합니다.

            사용 조건:
            - 사용자가 "A를 B로 바꿔줘", "A 대신 B로 교체해줘",
              "A 말고 B로 해줘" 등과 같이
              기존 장소를 다른 장소로 변경하길 명확히 요청했을 때만 사용하세요.
            - 삭제(delete)나 추가(add)가 아니라,
              "교체(replace)" 의도가 분명한 경우에만 사용합니다.
            - 기존 장소(oldPlaceName)가 일정에 존재하지 않으면
              이 Tool을 호출하지 말고 먼저 사용자에게 확인 질문을 하세요.
            - 여러 장소가 후보로 모호할 경우에도
              이 Tool을 호출하지 말고 명확히 어떤 장소인지 다시 물어보세요.

            입력:
            - oldPlaceName: 현재 일정에 등록된 기존 장소 이름
            - newPlaceName: 새로 교체할 장소 이름

            주의:
            - 이 Tool은 기존 장소를 제거하고 새 장소로 대체하는 실제 수정 작업입니다.
            - 실행 후에는 일정 구조가 변경되므로,
              Tool 실행 결과를 반드시 사용자에게 명확히 설명하세요.
            """)
    public String replacePlace(
            String oldPlaceName,
            String newPlaceName,
            ToolContext toolContext) {

        String conversationId = getConversationId(toolContext);
        Long planId = support.getPlanId(conversationId);

        log.info("🔧 [Tool] replacePlace: planId={}, old={}, new={}",
                planId, oldPlaceName, newPlaceName);

        // 1. Validation
        if (oldPlaceName == null || oldPlaceName.isBlank()) {
            return "교체할 장소 이름을 알려주세요.";
        }
        if (newPlaceName == null || newPlaceName.isBlank()) {
            return "새로운 장소 이름을 알려주세요.";
        }

        try {
            String newName = modifyAction.replacePlaceWithSearch(
                    planId, oldPlaceName, newPlaceName);

            Integer version = support.saveSnapshot(planId);

            return String.format(
                    "'%s'를 '%s'(으)로 변경했습니다. 버전: %d",
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
    @Tool(description = """
            이 Tool은 사용자의 여행 일정(plan)에서
            특정 날짜(dayIndex)에 해당하는 일정 전체를 삭제할 때 사용합니다.

            사용 조건:
            - 사용자가 “N일차 삭제”, “N번째 날 일정 지워줘”,
              “N일차 일정 전부 없애줘” 등과 같이
              특정 날짜 전체를 삭제하길 명확히 요청했을 때만 사용하세요.
            - dayIndex는 반드시 1부터 시작합니다.
            - 삭제 대상 날짜가 존재하지 않거나,
              사용자의 요청이 모호한 경우에는
              이 Tool을 호출하지 말고 먼저 사용자에게 확인 질문을 하세요.

            입력:
            - dayIndex: 삭제할 날짜 번호 (1부터 시작)

            주의:
            - 이 Tool은 해당 날짜의 모든 장소를 포함하여
              일정 데이터를 실제로 삭제합니다.
            - 실행 후에는 되돌릴 수 없으므로,
              사용자에게 삭제 결과를 반드시 명확히 안내하세요.
                    """)
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

    @Tool(description = """
            ⚠️ 우선순위 Tool: 사용자가 장소명을 언급하고 추가하려 할 때 사용
        
            ✅ 반드시 이 Tool을 사용해야 하는 경우:
            - "설 추가해줘" → 장소명 "설"
            - "경복궁 넣어줘" → 장소명 "경복궁"
            - "명동 1일차에 추가" → 장소명 "명동"
            → 사용자가 장소 이름을 언급한 경우
            
            ❌ 사용하지 않는 경우:
            - "5번 추가해줘" → addRecommendedPlace 사용!
            - "추천 2번 넣어줘" → addRecommendedPlace 사용!
            → 숫자만 언급한 경우 (추천 번호)
            
            핵심 구분:
            - 장소 이름 언급 ("설", "경복궁", "명동") → 이 Tool ✅
            - 숫자만 언급 ("5번", "2번") → addRecommendedPlace
            
            동작:
            1. 장소명으로 DB 검색
            2. 정확히 일치 → 바로 추가
            3. 여러 후보 → 목록 반환
            4. 없음 → "찾을 수 없음"
            
            파라미터:
            - placeName: 장소 이름 (필수)
            - dayIndex: 몇 일차 (선택, 없으면 질문)
            - position: 몇 번째 (선택, 없으면 맨 뒤)
                    """)
    public String addPlace(
            @ToolParam(description = "몇 일차인지 (1부터 시작)", required = false) Integer dayIndex,

            @ToolParam(description = "몇 번째 위치인지 (1부터). 없으면 맨 뒤", required = false) Integer position,

            @ToolParam(description = "추가할 장소 이름 (사용자가 이미 알고 있는 장소)", required = true) String placeName,

            ToolContext toolContext) {
        String conversationId = getConversationId(toolContext);
        // 위치 없으면 무조건 되묻기
        if (dayIndex == null || position == null) {
            return """
                    어느 날짜에, 몇 번째로 추가할까요?

                    예시:
                    - "경복궁을 3일차 1번째에 추가해줘"
                    - "롯데월드를 2일차 맨 뒤에 넣어줘"
                    """;
        }

        Long planId = support.getPlanId(conversationId);
        log.info("🔧 [Tool] addPlace: planId={}, dayIndex={}, placeName={}", planId, dayIndex, placeName);
        try {
            String result = addAction.addPlace(planId, dayIndex, placeName, position);
            Integer versionNo = support.saveSnapshot(planId);
            return String.format("%d일차에 '%s'을(를) 추가했습니다. 버전: %d", dayIndex, result, versionNo);
        } catch (Exception e) {
            log.error("장소 추가 실패", e);
            return String.format("장소 추가 중 오류 발생: %s", e.getMessage());
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
