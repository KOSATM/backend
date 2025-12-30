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

            ⚠️ 이 Tool은 "삭제" 의도가 명확할 때만 사용하세요.

            ━━━━━━━━━━━━━━━━━━━━━━━
            사용해야 할 때
            ━━━━━━━━━━━━━━━━━━━━━━━
            - "경복궁 삭제해줘"
            - "2일차 첫 번째 장소 지워줘"
            - "3일차 스타벅스 삭제"
            - "카페 드롭탑 제거해줘"

            ━━━━━━━━━━━━━━━━━━━━━━━
            사용하지 말아야 할 때
            ━━━━━━━━━━━━━━━━━━━━━━━
            - 장소 추가 흐름 중 사용자의 위치 응답
              예: "2일차에", "세 번째에"
            - 추천 장소 선택 중 번호 입력
              예: "3번", "5번 추가"

            위 경우에는 ❌ deletePlace 사용 금지

            ━━━━━━━━━━━━━━━━━━━━━━━
            입력 규칙 (중요)
            ━━━━━━━━━━━━━━━━━━━━━━━
            - dayIndex / position ToolParam은 신뢰하지 않습니다.
            - 실제 값은 사용자 발화(userMessage)에서 직접 추출합니다.
            - 없는 정보는 절대 추측하지 않습니다.

            ━━━━━━━━━━━━━━━━━━━━━━━
            내부 처리 로직
            ━━━━━━━━━━━━━━━━━━━━━━━
            1. dayIndex + position 이 있으면
               → 해당 위치의 장소를 바로 삭제

            2. dayIndex + placeName 이 있으면
               → 해당 날짜에서 장소 이름으로 검색 후 삭제

            3. placeName만 있으면
               → 전체 일정에서 검색
                 - 하나면 바로 삭제
                 - 여러 개면 사용자에게 선택 요청

            4. 삭제가 성공하면
               → 스냅샷을 저장하고 완료 메시지 반환

            ━━━━━━━━━━━━━━━━━━━━━━━
            주의 사항
            ━━━━━━━━━━━━━━━━━━━━━━━
            - 삭제는 되돌릴 수 없는 작업이므로 항상 사용자 의도를 명확히 확인하세요.
            - 모호한 경우 반드시 사용자에게 다시 질문해야 합니다.
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
            @ToolParam(description = "몇 일차인지 (1부터)", required = false) Integer dayIndex,
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
            1. 장소명 + "추가" → 후보 있으면 목록 반환, 정보 부족하면 질문
            2. [STATE: ADD_CANDIDATE] + 번호 → 후보에서 선택
            3. 부분 정보 입력 → 자동으로 컨텍스트 복원 후 이어서 처리

            ⚠️ 중요:
            - RECOMMENDATION 상태에서는 addRecommendedPlace 사용
            - dayIndex / position 없으면 절대 실제 추가하지 않음
            - 사용자가 명시하지 않은 정보는 추론하지 말고 물어보세요
            - Tool 실행 결과가 질문이면 그대로 전달하고 재시도하지 마세요
                """)
    public String addPlace(
            @ToolParam(description = "장소 이름 또는 후보 번호") String placeName,
            @ToolParam(description = "몇 일차인지 (1부터). 현재 일정에서 추론 가능", required = false) Integer dayIndex,
            @ToolParam(description = "몇 번째 위치인지 (1부터). 현재 일정에서 추론 가능", required = false) Integer position,
            ToolContext toolContext) {

        String conversationId = getConversationId(toolContext);
        Long planId = support.getPlanId(conversationId);
        String userMessage = (String) toolContext.getContext().get("userMessage");

        PlanToolSupport.PendingSelectionType state = support.getPendingSelectionType(conversationId);

        log.info("🔧 [addPlace] placeName={}, dayIndex={}, position={}, state={}",
                placeName, dayIndex, position, state);

        /*
         * =====================================================
         * 0️⃣ 추천 상태 차단
         * =====================================================
         */
        if (state == PlanToolSupport.PendingSelectionType.RECOMMENDATION) {
            return "추천 장소를 추가 중입니다. 번호로 선택해주세요. (예: \"3번 추가\")";
        }

        /*
         * =====================================================
         * 1️⃣ ADD_CANDIDATE 상태 (후보 번호 선택)
         * =====================================================
         */
        if (state == PlanToolSupport.PendingSelectionType.ADD_CANDIDATE) {

            List<TravelPlaces> candidates = support.getAddPlaceCandidates(conversationId);
            TravelPlaces selected = null;

            // 번호 선택
            if (placeName.matches("\\d+")) {
                int idx = Integer.parseInt(placeName);
                if (idx >= 1 && idx <= candidates.size()) {
                    selected = candidates.get(idx - 1);
                }
            }

            // 장소명 직접 선택 (LLM 보정 대비)
            if (selected == null) {
                selected = candidates.stream()
                        .filter(c -> c.getTitle().equals(placeName))
                        .findFirst()
                        .orElse(null);
            }

            // 후보 선택 실패 → 상태 종료 후 일반 흐름
            if (selected == null) {
                support.clearPendingSelection(conversationId);
            } else {

                // 컨텍스트 복원
                PlanToolSupport.SelectionContext ctx = support.getSelectionContext(conversationId);
                if (ctx != null) {
                    dayIndex = ctx.getDayIndex();
                    position = ctx.getPosition();
                }

                // 사용자 발화에서 보충
                Integer d = UserInputParser.parseDayIndex(userMessage);
                Integer p = UserInputParser.parsePosition(userMessage);

                if (d != null) {
                    dayIndex = d;
                    support.updateDayIndex(conversationId, d);
                }
                if (p != null) {
                    position = p;
                    support.updatePosition(conversationId, p);
                }

                // 질문
                if (dayIndex == null) {
                    return String.format("'%s'을(를) 몇 일차에 추가할까요?",
                            selected.getTitle());
                }
                if (position == null) {
                    return String.format("%d일차에 '%s'을(를) 몇 번째에 추가할까요?",
                            dayIndex, selected.getTitle());
                }

                // 실행
                String added = addAction.addPlaceFromCandidate(
                        planId, dayIndex, position, selected);

                support.clearPendingSelection(conversationId);
                Integer version;
                try {
                    version = support.saveSnapshot(planId);
                } catch (Exception e) {
                    log.error("장소 추가 실패", e);
                    return String.format("❌ 장소 추가 중 오류 발생: %s", e.getMessage());
                }

                return String.format(
                        "%d일차 %d번째에 '%s'을(를) 추가했습니다. (버전 %d)",
                        dayIndex, position, added, version);
            }
        }

        /*
         * =====================================================
         * 2️⃣ 일반 장소 추가 (새 작업)
         * =====================================================
         */
        support.clearPendingSelection(conversationId);

        // ✅ 사용자 발화 파싱
        Integer dayFromText = UserInputParser.parseDayIndex(userMessage);
        Integer posFromText = UserInputParser.parsePosition(userMessage);

        // ✅ 최종값 결정 (우선순위: 사용자 발화 > ToolParam)
        Integer finalDayIndex = (dayFromText != null) ? dayFromText : dayIndex;
        Integer finalPosition = (posFromText != null) ? posFromText : position;

        // ✅ 일차 정보가 아예 없으면 물어보기
        if (finalDayIndex == null) {
            return "몇 일차에 추가할까요? 😊";
        }

        // ✅ position만 없으면 마지막에 추가
        if (finalPosition == null) {
            finalPosition = calculateLastPosition(planId, finalDayIndex);
        }

        log.info("📍 [최종 파라미터] dayIndex={}, position={}", finalDayIndex, finalPosition);

        PlanAddAction.AddPlaceResult result = addAction.addPlace(
                planId, finalDayIndex, placeName, finalPosition);

        switch (result.getType()) {

            case CANDIDATES:
                support.setAddPlaceCandidates(
                        conversationId,
                        result.getCandidates(),
                        finalDayIndex,
                        finalPosition);
                return result.getMessage();

            case NEED_DAY:
            case NEED_POSITION:
                return result.getMessage();

            case ERROR:
                return result.getMessage();

            case SUCCESS:
                Integer version;
                try {
                    version = support.saveSnapshot(planId);
                    return String.format(
                            "%d일차 %d번째에 '%s'을(를) 추가했습니다. (버전 %d)",
                            finalDayIndex, finalPosition,
                            result.getAddedPlaceName(),
                            version);
                } catch (Exception e) {
                    log.error("장소 추가 실패", e);
                    return String.format("❌ 장소 추가 중 오류 발생: %s", e.getMessage());
                }
        }

        return "알 수 없는 오류가 발생했습니다.";
    }

    // ===============================
    // Helper 메서드
    // ===============================
    private Integer calculateLastPosition(Long planId, Integer dayIndex) {
        try {
            List<PlanDay> days = support.loadDays(planId);
            final Integer dayIndexFinal = dayIndex;

            PlanDay targetDay = days.stream()
                    .filter(d -> d.getDayIndex().equals(dayIndexFinal))
                    .findFirst()
                    .orElse(null);

            if (targetDay != null) {
                List<PlanPlace> places = support.loadPlacesByDayId(targetDay.getId());
                return places.size() + 1;
            }
            return 1;
        } catch (Exception e) {
            log.warn("위치 계산 실패, 기본값 사용: {}", e.getMessage());
            return 1;
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
