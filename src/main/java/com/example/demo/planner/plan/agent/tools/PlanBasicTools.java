package com.example.demo.planner.plan.agent.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.common.util.UserInputParser;
import com.example.demo.planner.plan.agent.common.PlanToolSupport;
import com.example.demo.planner.plan.dto.entity.PlanDay;
import com.example.demo.planner.plan.dto.entity.PlanPlace;
import com.example.demo.planner.plan.dto.entity.TravelPlaces;
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
            일정에서 특정 장소를 삭제합니다.
            ...
            """)
    public String deletePlace(
            @ToolParam(description = "삭제할 장소 이름") String placeName,

            @ToolParam(description = "몇 일차인지 (1부터). 없으면 null", required = false) Integer dayIndex,

            @ToolParam(description = "몇 번째인지 (1부터). 없으면 null", required = false) Integer position,

            ToolContext toolContext) {

        String conversationId = getConversationId(toolContext);
        Long planId = support.getPlanId(conversationId);

        support.clearPendingSelection(conversationId);
        log.info("🧹 [deletePlace] 모든 선택 상태 클리어");

        // ✅ 사용자 발화 파싱
        String userMessage = (String) toolContext.getContext().get("userMessage");

        Integer dayFromText = UserInputParser.parseDayIndex(userMessage);
        Integer posFromText = UserInputParser.parsePosition(userMessage);

        // ✅ final 변수로 복사 (람다식용)
        final Integer finalDayIndex = (dayFromText != null) ? dayFromText : dayIndex;
        final Integer finalPosition = (posFromText != null) ? posFromText : position;

        log.info("🔧 [Tool] deletePlace: placeName={}, dayIndex={}, position={}",
                placeName, finalDayIndex, finalPosition);

        try {
            // ✅ Case 1: dayIndex + position 둘 다 있음
            if (finalDayIndex != null && finalPosition != null) {
                deleteAction.deletePlace(planId, finalDayIndex, finalPosition);
                Integer version = support.saveSnapshot(planId);

                return String.format(
                        "%d일차 %d번째 장소를 삭제했습니다. (버전 %d)",
                        finalDayIndex, finalPosition, version);
            }

            // ✅ Case 2: dayIndex + placeName
            if (finalDayIndex != null) {
                List<PlanDay> days = support.loadDays(planId);
                PlanDay targetDay = days.stream()
                        .filter(d -> d.getDayIndex() == finalDayIndex) // ✅ final 변수 사용
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(finalDayIndex + "일차를 찾을 수 없습니다."));

                List<PlanPlace> places = support.loadPlacesByDayId(targetDay.getId());

                // placeName으로 찾기
                int foundIndex = -1;
                for (int i = 0; i < places.size(); i++) {
                    if (places.get(i).getTitle().equals(placeName) ||
                            places.get(i).getPlaceName().equals(placeName)) {
                        foundIndex = i + 1;
                        break;
                    }
                }

                if (foundIndex == -1) {
                    return String.format("%d일차에 '%s' 장소를 찾을 수 없습니다.",
                            finalDayIndex, placeName);
                }

                deleteAction.deletePlace(planId, finalDayIndex, foundIndex);
                Integer version = support.saveSnapshot(planId);

                return String.format(
                        "'%s'을(를) %d일차에서 삭제했습니다. (버전 %d)",
                        placeName, finalDayIndex, version);
            }

            // ✅ Case 3: placeName만 있음
            List<PlanDay> days = support.loadDays(planId);
            Map<Long, List<PlanPlace>> placesByDayId = support.loadPlacesByDayId(days);

            List<PlaceLocation> found = new ArrayList<>();
            for (PlanDay day : days) {
                List<PlanPlace> places = placesByDayId.get(day.getId());
                if (places == null)
                    continue;

                for (int i = 0; i < places.size(); i++) {
                    PlanPlace p = places.get(i);
                    if (p.getTitle().equals(placeName) ||
                            p.getPlaceName().equals(placeName)) {
                        found.add(new PlaceLocation(day.getDayIndex(), i + 1, p.getTitle()));
                    }
                }
            }

            if (found.isEmpty()) {
                return String.format("'%s' 장소를 찾을 수 없습니다.", placeName);
            }

            if (found.size() > 1) {
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("'%s' 장소가 여러 개 있습니다:\n\n", placeName));

                for (int i = 0; i < found.size(); i++) {
                    PlaceLocation loc = found.get(i);
                    sb.append(String.format("%d. %d일차 %d번째 - %s\n",
                            i + 1, loc.dayIndex, loc.position, loc.name));
                }

                sb.append("\n몇 일차의 장소를 삭제할까요?");
                return sb.toString();
            }

            PlaceLocation loc = found.get(0);
            deleteAction.deletePlace(planId, loc.dayIndex, loc.position);
            Integer version = support.saveSnapshot(planId);

            return String.format(
                    "'%s' 장소를 일정에서 삭제했습니다. (버전 %d)",
                    loc.name, version);

        } catch (IllegalArgumentException e) {
            return e.getMessage();
        } catch (Exception e) {
            log.error("❌ 장소 삭제 실패", e);
            return String.format("장소 삭제 중 오류 발생: %s", e.getMessage());
        }
    }

    // ===============================
    // Helper 클래스
    // ===============================

    private static class PlaceLocation {
        final int dayIndex;
        final int position;
        final String name;

        PlaceLocation(int dayIndex, int position, String name) {
            this.dayIndex = dayIndex;
            this.position = position;
            this.name = name;
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

        support.clearPendingSelection(conversationId);
        log.info("🧹 [replacePlace] 모든 선택 상태 클리어");

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

        support.clearPendingSelection(conversationId);
        log.info("🧹 [deleteDay] 모든 선택 상태 클리어");

        try {
            deleteAction.deleteDay(planId, dayIndex);
            Integer version = support.saveSnapshot(planId);

            return String.format(
                    " %d일차 일정을 삭제했습니다. 버전: %d",
                    dayIndex, version);

        } catch (Exception e) {
            log.error("날짜 삭제 실패", e);
            return String.format("❌ 날짜 삭제 중 오류 발생: %s", e.getMessage());
        }
    }

    @Transactional
    @Tool(description = """
            장소 추가 Tool

            사용 케이스:
            1. 장소명 + "추가" → 후보 있으면 목록 반환, 없으면 바로 추가
            2. [STATE: ADD_CANDIDATE] + 번호 선택 → 후보에서 선택
            3. 부분 정보 입력 → 자동으로 질문하고 컨텍스트 복원

            중요:
            - [STATE: RECOMMENDATION]일 때는 addRecommendedPlace 사용 권장
            - 부족한 정보는 자동으로 질문
            - 컨텍스트는 자동으로 복원
            """)
    public String addPlace(
            @ToolParam(description = "사용자 발화에서 언급한 장소 이름 또는 번호") String placeName,

            @ToolParam(description = "몇 일차인지 (1부터). 없으면 null", required = false) Integer dayIndex,

            @ToolParam(description = "몇 번째 위치인지 (1부터). 없으면 null", required = false) Integer position,

            ToolContext toolContext) {

        String conversationId = getConversationId(toolContext);
        Long planId = support.getPlanId(conversationId);

        PlanToolSupport.PendingSelectionType selectionType = support.getPendingSelectionType(conversationId);

        log.info("🔧 [Tool] addPlace: placeName={}, state={}", placeName, selectionType);

        /*
         * =====================================================
         * 🆕 RECOMMENDATION 상태 처리 (잘못된 Tool 호출)
         * =====================================================
         */
        if (selectionType == PlanToolSupport.PendingSelectionType.RECOMMENDATION) {
            log.warn("⚠️ [추천 상태에서 addPlace 호출] placeName={}", placeName);
            support.clearPendingSelection(conversationId);

            return String.format("""
                    ⚠️ 추천 목록에서 선택하려면 번호를 말씀해주세요.

                    아니면 '%s'을(를) 직접 추가하시겠어요?
                    그렇다면 몇 일차에 추가할까요?
                    """, placeName);
        }

        /*
         * =====================================================
         * 1. ADD_CANDIDATE 상태 처리 (후보 번호 선택)
         * =====================================================
         */
        if (selectionType == PlanToolSupport.PendingSelectionType.ADD_CANDIDATE) {

            List<TravelPlaces> candidates = support.getAddPlaceCandidates(conversationId);
            TravelPlaces selected = null;

            boolean isNumber = placeName.matches("\\d+");

            // 방법 1: 숫자로 시도
            if (isNumber) {
                try {
                    int index = Integer.parseInt(placeName);
                    if (index >= 1 && index <= candidates.size()) {
                        selected = candidates.get(index - 1);
                    }
                } catch (NumberFormatException ignore) {
                }
            }

            // 방법 2: 장소명으로 시도 (LLM이 변환한 경우)
            if (selected == null) {
                selected = candidates.stream()
                        .filter(c -> c.getTitle().equals(placeName))
                        .findFirst()
                        .orElse(null);
            }

            // 둘 다 실패 → 후보 상태 종료
            if (selected == null) {
                log.info("🔍 [후보 목록에 없음] placeName={}", placeName);
                support.clearPendingSelection(conversationId);
                // 아래 일반 추가로 진행
            } else {
                // 후보 선택 처리
                log.info("[후보 선택] {}", selected.getTitle());

                String userMessage = (String) toolContext.getContext().get("userMessage");

                // 컨텍스트 복원
                PlanToolSupport.SelectionContext context = support.getSelectionContext(conversationId);
                if (context != null) {
                    dayIndex = context.getDayIndex();
                    position = context.getPosition();
                }

                // 사용자 발화에서 추출
                Integer dayFromText = UserInputParser.parseDayIndex(userMessage);
                Integer posFromText = UserInputParser.parsePosition(userMessage);

                if (dayFromText != null) {
                    support.updateDayIndex(conversationId, dayFromText);
                    dayIndex = dayFromText;
                }
                if (posFromText != null) {
                    support.updatePosition(conversationId, posFromText);
                    position = posFromText;
                }

                // 부족한 값 질문
                if (dayIndex == null) {
                    return String.format("'%s'을(를) 몇 일차에 추가할까요?", selected.getTitle());
                }
                if (position == null) {
                    return String.format("%d일차에 '%s'을(를) 몇 번째에 추가할까요?",
                            dayIndex, selected.getTitle());
                }

                // 실행
                try {
                    String addedName = addAction.addPlaceFromCandidate(planId, dayIndex, position, selected);
                    support.clearPendingSelection(conversationId);
                    Integer version = support.saveSnapshot(planId);

                    return String.format("%d일차 %d번째에 '%s'을(를) 추가했습니다. (버전 %d)",
                            dayIndex, position, addedName, version);

                } catch (Exception e) {
                    log.error("❌ 후보 장소 추가 실패", e);
                    return "장소 추가 중 오류가 발생했습니다.";
                }
            }
        }

        /*
         * =====================================================
         * 2. 일반 장소 추가
         * =====================================================
         */

        // 상태 클리어 (새 작업 시작)
        support.clearPendingSelection(conversationId);
        log.info("🧹 [addPlace] 모든 선택 상태 클리어");

        // ToolParam 무시하고 사용자 발화에서 파싱
        dayIndex = null;
        position = null;

        String userMessage = (String) toolContext.getContext().get("userMessage");

        Integer dayFromText = UserInputParser.parseDayIndex(userMessage);
        Integer posFromText = UserInputParser.parsePosition(userMessage);

        if (dayFromText != null) {
            dayIndex = dayFromText;
        }
        if (posFromText != null) {
            position = posFromText;
        }

        // position 기본값 설정 (검색용)
        Integer searchPosition = (position != null) ? position : Integer.MAX_VALUE;

        // dayIndex 임시값 설정 (검색용)
        Integer searchDayIndex = (dayIndex != null) ? dayIndex : 1;

        // 먼저 검색 (후보 찾기)
        try {
            PlanAddAction.AddPlaceResult result = addAction.addPlace(
                    planId,
                    searchDayIndex,
                    placeName,
                    searchPosition);

            // 후보 목록인 경우
            if (result.hasCandidates()) {
                log.info("📋 [후보 목록 발견] 개수={}", result.getCandidates().size());
                support.setAddPlaceCandidates(conversationId,
                        result.getCandidates(),
                        dayIndex,
                        null); // position은 null로 저장 (나중에 다시 물어봄)
                return result.getMessage();
            }

            // 에러인 경우
            if (result.isError()) {
                return result.getMessage();
            }

            // 성공인 경우 - dayIndex 체크
            if (dayIndex == null) {
                return String.format("'%s'을(를) 몇 일차에 추가할까요?", placeName);
            }

            Integer versionNo = support.saveSnapshot(planId);
            log.info("[장소 추가 완료] {}일차에 '{}' 추가, 버전: {}",
                    searchDayIndex, result.getAddedPlaceName(), versionNo);

            return String.format("%d일차 %d번째에 '%s'을(를) 추가했습니다. (버전 %d)",
                    searchDayIndex, searchPosition, result.getAddedPlaceName(), versionNo);

        } catch (Exception e) {
            log.error("❌ [장소 추가 실패]", e);
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
