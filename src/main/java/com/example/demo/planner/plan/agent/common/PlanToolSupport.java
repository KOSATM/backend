package com.example.demo.planner.plan.agent.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.example.demo.planner.plan.dao.PlanDao;
import com.example.demo.planner.plan.dao.PlanDayDao;
import com.example.demo.planner.plan.dao.PlanPlaceDao;
import com.example.demo.planner.plan.dto.entity.Plan;
import com.example.demo.planner.plan.dto.entity.PlanDay;
import com.example.demo.planner.plan.dto.entity.PlanPlace;
import com.example.demo.planner.plan.dto.entity.PlanSnapshot;
import com.example.demo.planner.plan.dto.entity.TravelPlaces;
import com.example.demo.planner.plan.service.PlanSnapshotService;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlanToolSupport {

    private final PlanDao planDao;
    private final PlanDayDao planDayDao;
    private final PlanPlaceDao planPlaceDao;
    private final PlanSnapshotService planSnapshotService;

    // =========================
    // 선택 대기 상태 타입
    // =========================
    public enum PendingSelectionType {
        NONE, // 대기 중인 선택 없음
        RECOMMENDATION, // 추천 목록에서 선택 대기
        ADD_CANDIDATE // 장소 후보에서 선택 대기
    }

    // =========================
    // conversationId 기반 상태 저장
    // =========================
    private final Map<String, Long> planIdByConversation = new ConcurrentHashMap<>();
    private final Map<String, List<Map<String, Object>>> lastRecommendationsByConversation = new ConcurrentHashMap<>();
    private final Map<String, Set<Long>> recommendedPlaceIdsByConversation = new ConcurrentHashMap<>();
    private final Map<String, List<TravelPlaces>> lastAddPlaceCandidatesByConversation = new ConcurrentHashMap<>();
    private final Map<String, SelectionContext> selectionContextByConversation = new ConcurrentHashMap<>();
    private final Map<String, PendingSelectionType> pendingSelectionByConversation = new ConcurrentHashMap<>();

    // =========================
    // planId 관리
    // =========================
    public void setPlanId(String conversationId, Long planId) {
        if (conversationId == null || conversationId.isBlank()) {
            return;
        }
        if (planId == null) {
            return;
        }
        planIdByConversation.put(conversationId, planId);
        log.debug("🗺️ [PlanId 설정] conversationId={}, planId={}", conversationId, planId);
    }

    public Long getPlanId(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return null;
        }
        return planIdByConversation.get(conversationId);
    }

    public boolean hasPlan(String conversationId) {
        return getPlanId(conversationId) != null;
    }

    public void clearPlanId(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return;
        }
        planIdByConversation.remove(conversationId);
        log.debug("🗑️ [PlanId 제거] conversationId={}", conversationId);
    }

    // =========================
    // 추천 여행지 관리
    // =========================
    public void setLastRecommendations(String conversationId, List<Map<String, Object>> list) {
        if (conversationId == null || conversationId.isBlank()) {
            return;
        }
        if (list == null || list.isEmpty()) {
            clearPendingSelection(conversationId);
            return;
        }

        // 상호 배타적: 장소 후보 클리어
        lastAddPlaceCandidatesByConversation.remove(conversationId);
        selectionContextByConversation.remove(conversationId);

        lastRecommendationsByConversation.put(conversationId, new ArrayList<>(list));
        pendingSelectionByConversation.put(conversationId, PendingSelectionType.RECOMMENDATION);

        log.info("📋 [추천 목록 설정] conversationId={}, 개수={}", conversationId, list.size());
    }

    public List<Map<String, Object>> getLastRecommendations(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return Collections.emptyList();
        }
        return lastRecommendationsByConversation.getOrDefault(conversationId, Collections.emptyList());
    }

    // =========================
    // addPlace 후보 목록 관리
    // =========================

    public void setAddPlaceCandidates(String conversationId,
            List<TravelPlaces> candidates,
            Integer dayIndex,
            Integer position) {
        if (conversationId == null || conversationId.isBlank()) {
            return;
        }
        if (candidates == null || candidates.isEmpty()) {
            clearPendingSelection(conversationId);
            return;
        }

        //상호 배타적: 추천 목록 클리어
        lastRecommendationsByConversation.remove(conversationId);

        lastAddPlaceCandidatesByConversation.put(conversationId, new ArrayList<>(candidates));

        //컨텍스트 새로 설정
        SelectionContext context = new SelectionContext(dayIndex, position, null);
        selectionContextByConversation.put(conversationId, context);
        pendingSelectionByConversation.put(conversationId, PendingSelectionType.ADD_CANDIDATE);

        log.info("📋 [장소 후보 설정] conversationId={}, 개수={}, dayIndex={}",
                conversationId, candidates.size(), dayIndex);
    }

    public List<TravelPlaces> getAddPlaceCandidates(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return Collections.emptyList();
        }
        return lastAddPlaceCandidatesByConversation.getOrDefault(conversationId, Collections.emptyList());
    }

    public boolean hasAddPlaceCandidates(String conversationId) {
        return !getAddPlaceCandidates(conversationId).isEmpty();
    }

    // =========================
    // 선택 컨텍스트 관리 (통합)
    // =========================

    /**
     * 추천 선택 컨텍스트 저장
     * - 덮어쓰기 금지: index만 업데이트하고 day/position은 보존
     */
    public void setRecommendContext(String conversationId,
            Integer index,
            Integer dayIndex,
            Integer position) {
        if (conversationId == null || conversationId.isBlank()) {
            return;
        }

        SelectionContext ctx = selectionContextByConversation
                .computeIfAbsent(conversationId, k -> new SelectionContext());

        // index만 저장 (dayIndex/position은 여기서 절대 덮어쓰지 않음)
        ctx.setIndex(index);

        log.info("💾 [추천 컨텍스트 저장] conversationId={}, index={}, (dayIndex/position 유지)",
                conversationId, index);
    }

    /**
     * 선택 컨텍스트 조회 (후보/추천 공통)
     */
    public SelectionContext getSelectionContext(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return null;
        }
        return selectionContextByConversation.get(conversationId);
    }

    // =========================
    // SelectionContext 부분 업데이트 (추가)
    // =========================

    public void updateDayIndex(String conversationId, Integer dayIndex) {
        if (conversationId == null || conversationId.isBlank() || dayIndex == null) {
            return;
        }
        SelectionContext ctx = selectionContextByConversation
                .computeIfAbsent(conversationId, k -> new SelectionContext());
        ctx.setDayIndex(dayIndex);
    }

    public void updatePosition(String conversationId, Integer position) {
        if (conversationId == null || conversationId.isBlank() || position == null) {
            return;
        }
        SelectionContext ctx = selectionContextByConversation
                .computeIfAbsent(conversationId, k -> new SelectionContext());
        ctx.setPosition(position);
    }

    public void updateIndex(String conversationId, Integer index) {
        if (conversationId == null || conversationId.isBlank() || index == null) {
            return;
        }
        SelectionContext ctx = selectionContextByConversation
                .computeIfAbsent(conversationId, k -> new SelectionContext());
        ctx.setIndex(index);
    }

    // =========================
    // 선택 대기 상태 관리
    // =========================

    public PendingSelectionType getPendingSelectionType(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return PendingSelectionType.NONE;
        }
        return pendingSelectionByConversation.getOrDefault(conversationId, PendingSelectionType.NONE);
    }

    /**
     * 장소 후보 상태만 클리어 (추천 목록 유지)
     * - 조회 Tool에서 사용 (viewPlan, viewDay)
     */
    public void clearAddCandidateState(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return;
        }

        // 장소 후보만 제거
        lastAddPlaceCandidatesByConversation.remove(conversationId);

        // ADD_CANDIDATE 상태인 경우만 컨텍스트 제거
        PendingSelectionType currentType = pendingSelectionByConversation.get(conversationId);
        if (currentType == PendingSelectionType.ADD_CANDIDATE) {
            selectionContextByConversation.remove(conversationId);
            pendingSelectionByConversation.remove(conversationId);
        }

        log.info("🧹 [장소 후보 상태 클리어] conversationId={} (추천 목록 유지)", conversationId);
    }

    /**
     * 추천 상태만 클리어 (장소 후보 유지)
     * - 거의 사용 안 함 (참고용)
     */
    public void clearRecommendationState(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return;
        }

        // 추천 목록만 제거
        lastRecommendationsByConversation.remove(conversationId);

        // RECOMMENDATION 상태인 경우만 컨텍스트 제거
        PendingSelectionType currentType = pendingSelectionByConversation.get(conversationId);
        if (currentType == PendingSelectionType.RECOMMENDATION) {
            selectionContextByConversation.remove(conversationId);
            pendingSelectionByConversation.remove(conversationId);
        }

        log.info("🧹 [추천 상태 클리어] conversationId={} (장소 후보 유지)", conversationId);
    }

    /**
     * 모든 선택 상태 클리어
     * - 수정/삭제 Tool에서 사용
     */
    public void clearPendingSelection(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return;
        }
        lastRecommendationsByConversation.remove(conversationId);
        lastAddPlaceCandidatesByConversation.remove(conversationId);
        selectionContextByConversation.remove(conversationId);
        pendingSelectionByConversation.remove(conversationId);

        log.info("🧹 [모든 선택 상태 클리어] conversationId={}", conversationId);
    }

    // =========================
    // 제외할 추천 여행지(id) 관리
    // =========================
    public void addRecommendedIds(String conversationId, List<Map<String, Object>> recs) {
        if (conversationId == null || conversationId.isBlank() || recs == null) {
            return;
        }

        Set<Long> set = recommendedPlaceIdsByConversation
                .computeIfAbsent(conversationId, k -> new HashSet<>());

        for (Map<String, Object> r : recs) {
            Object idObj = r.get("id");
            try {
                if (idObj instanceof Number) {
                    set.add(((Number) idObj).longValue());
                } else if (idObj instanceof String) {
                    set.add(Long.parseLong((String) idObj));
                }
            } catch (Exception ignored) {
            }
        }

        log.debug("🔖 [추천 ID 추가] conversationId={}, 총 개수={}", conversationId, set.size());
    }

    public Set<Long> getRecommendedIds(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return Collections.emptySet();
        }
        return recommendedPlaceIdsByConversation.getOrDefault(conversationId, Collections.emptySet());
    }

    public void clearRecommendedIds(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return;
        }
        recommendedPlaceIdsByConversation.remove(conversationId);
        log.debug("🗑️ [추천 ID 제거] conversationId={}", conversationId);
    }

    // =========================
    // 전체 상태 초기화
    // =========================
    public void clear(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return;
        }
        planIdByConversation.remove(conversationId);
        recommendedPlaceIdsByConversation.remove(conversationId);
        clearPendingSelection(conversationId);

        log.info("🧹 [전체 상태 초기화] conversationId={}", conversationId);
    }

    // =========================
    // 스냅샷 저장
    // =========================
    public Integer saveSnapshot(Long planId) throws Exception {
        if (planId == null) {
            throw new IllegalStateException("저장할 일정이 없습니다.");
        }

        log.debug("💾 [스냅샷 저장 시작] planId={}", planId);

        Plan plan = planDao.selectPlanById(planId);
        List<PlanDay> planDays = planDayDao.selectPlanDaysByPlanId(planId);
        List<PlanPlace> planPlaces = planPlaceDao.selectPlanPlacesByPlanId(planId);

        PlanSnapshot snapshot = planSnapshotService.savePlanSnapshot(plan, planDays, planPlaces);

        log.info("💾 [스냅샷 저장 완료] planId={}, versionNo={}", planId, snapshot.getVersionNo());

        return snapshot.getVersionNo();
    }

    // =========================
    // 스냅샷 전체 삭제
    // =========================
    public void deleteAllSnapshot(Long userId) throws Exception {
        log.info("🗑️ [스냅샷 전체 삭제] userId={}", userId);
        planSnapshotService.deletePlanSnapshotsByUserId(userId);
    }

    // =========================
    // Plan 조회
    // =========================
    public Plan loadPlan(Long planId) {
        if (planId == null) {
            throw new IllegalStateException("조회할 일정이 없습니다.");
        }
        Plan plan = planDao.selectPlanById(planId);
        if (plan == null) {
            throw new IllegalArgumentException("일정을 찾을 수 없습니다.");
        }
        log.debug("📖 [Plan 조회] planId={}", planId);
        return plan;
    }

    public List<PlanDay> loadDays(Long planId) {
        if (planId == null) {
            return Collections.emptyList();
        }
        List<PlanDay> days = planDayDao.selectPlanDaysByPlanId(planId);
        log.debug("📖 [Days 조회] planId={}, 개수={}", planId, days.size());
        return days;
    }

    public PlanDay loadDayByIndex(Long planId, Integer dayIndex) {
        if (planId == null) {
            throw new IllegalStateException("일정이 없습니다.");
        }
        PlanDay day = planDayDao.selectPlanDayByPlanIdAndDayIndex(planId, dayIndex);
        if (day == null) {
            throw new IllegalArgumentException(dayIndex + "일차를 찾을 수 없습니다.");
        }
        log.debug("📖 [Day 조회] planId={}, dayIndex={}", planId, dayIndex);
        return day;
    }

    public Map<Long, List<PlanPlace>> loadPlacesByDayId(List<PlanDay> days) {
        Map<Long, List<PlanPlace>> map = new HashMap<>();
        for (PlanDay day : days) {
            map.put(day.getId(),
                    planPlaceDao.selectPlanPlacesByPlanDayId(day.getId()));
        }
        log.debug("📖 [Places 조회] 일차 개수={}", days.size());
        return map;
    }

    public List<PlanPlace> loadPlacesByDayId(Long dayId) {
        if (dayId == null) {
            return Collections.emptyList();
        }
        List<PlanPlace> places = planPlaceDao.selectPlanPlacesByPlanDayId(dayId);
        log.debug("📖 [Places 조회] dayId={}, 개수={}", dayId, places.size());
        return places;
    }

    // =========================
    // 렌더링
    // =========================
    public String renderPlan(Plan plan, List<PlanDay> planDays, Map<Long, List<PlanPlace>> placesByDayId) {
        if (plan == null) {
            return "📭 아직 생성된 여행 일정이 없습니다.\n원하시면 새 일정을 만들어드릴게요!";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("📅 ").append(plan.getTitle() != null ? plan.getTitle() : "여행 일정").append("\n");
        sb.append("기간: ").append(plan.getStartDate()).append(" ~ ").append(plan.getEndDate()).append("\n\n");

        for (PlanDay day : planDays) {
            sb.append("=== Day ").append(day.getDayIndex()).append(" ===\n");
            sb.append(day.getPlanDate()).append("\n");

            List<PlanPlace> places = placesByDayId.get(day.getId());
            if (places == null || places.isEmpty()) {
                sb.append("등록된 장소가 없습니다.\n\n");
                continue;
            }

            for (PlanPlace place : places) {
                sb.append("• ").append(place.getTitle()).append("\n");
            }
            sb.append("\n");
        }

        log.debug("🖼️ [Plan 렌더링] 일차={}", planDays.size());
        return sb.toString();
    }

    public String renderDay(PlanDay day, List<PlanPlace> places) {
        if (day == null) {
            return "해당 일차 정보를 찾을 수 없습니다.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("📅 Day ").append(day.getDayIndex()).append("\n");
        sb.append("🗓 날짜: ").append(day.getPlanDate()).append("\n\n");

        if (places == null || places.isEmpty()) {
            sb.append("⚠️ 등록된 장소가 없습니다.");
            return sb.toString();
        }

        for (PlanPlace place : places) {
            sb.append("• ").append(place.getTitle()).append("\n");
        }

        log.debug("🖼️ [Day 렌더링] dayIndex={}, 장소={}", day.getDayIndex(), places.size());
        return sb.toString();
    }

    // =========================
    // 상태 컨텍스트 생성
    // =========================
    public String buildStateContext(String conversationId) {
        PendingSelectionType selectionType = getPendingSelectionType(conversationId);

        // 대기 중인 작업이 없는 경우
        if (selectionType == PendingSelectionType.NONE) {
            return """
                    <context>
                    현재 특별히 대기 중인 작업이 없습니다.
                    사용자의 요청에 맞는 Tool을 자유롭게 선택하세요.
                    </context>
                    """;
        }

        // 추천 목록 선택 대기
        if (selectionType == PendingSelectionType.RECOMMENDATION) {
            List<Map<String, Object>> recs = getLastRecommendations(conversationId);
            SelectionContext context = getSelectionContext(conversationId);

            StringBuilder sb = new StringBuilder();
            sb.append("<context>\n");
            sb.append(String.format("방금 사용자에게 %d개의 여행지를 추천했습니다.\n", recs.size()));
            sb.append("사용자가 선택을 기다리고 있습니다.\n\n");

            // 이미 입력된 정보 표시
            if (context != null) {
                if (context.getIndex() != null) {
                    String placeName = (String) recs.get(context.getIndex() - 1).get("title");
                    sb.append(String.format("- 선택한 장소: %d번 (%s)\n", context.getIndex(), placeName));
                }
                if (context.getDayIndex() != null) {
                    sb.append(String.format("- 추가할 일차: %d일차\n", context.getDayIndex()));
                }
                if (context.getPosition() != null) {
                    sb.append(String.format("- 추가할 위치: %d번째\n", context.getPosition()));
                }
            }

            sb.append("\n다음 동작:\n");
            sb.append("- 사용자가 '3번', '5번 추가' 같은 숫자를 말하면 → addRecommendedPlace 사용\n");
            sb.append("- 사용자가 '2일차에', '맨 뒤에' 같은 위치를 말하면 → 정보를 저장하고 부족한 부분 물어보기\n");
            sb.append("- 사용자가 완전히 새로운 요청을 하면 → 해당 요청에 맞는 Tool 사용\n");
            sb.append("</context>");

            return sb.toString();
        }

        // 장소 후보 선택 대기
        if (selectionType == PendingSelectionType.ADD_CANDIDATE) {
            List<TravelPlaces> candidates = getAddPlaceCandidates(conversationId);
            SelectionContext context = getSelectionContext(conversationId);

            StringBuilder sb = new StringBuilder();
            sb.append("<context>\n");
            sb.append(String.format("사용자가 입력한 장소명으로 %d개의 후보를 찾았습니다.\n", candidates.size()));
            sb.append("사용자가 어느 것을 추가할지 선택을 기다리고 있습니다.\n\n");

            // 후보 목록 표시
            sb.append("후보 목록:\n");
            for (int i = 0; i < Math.min(3, candidates.size()); i++) {
                TravelPlaces place = candidates.get(i);
                sb.append(String.format("%d. %s\n", i + 1, place.getTitle()));
            }
            if (candidates.size() > 3) {
                sb.append(String.format("... 외 %d개\n", candidates.size() - 3));
            }
            sb.append("\n");

            // 이미 입력된 정보 표시
            if (context != null) {
                if (context.getIndex() != null) {
                    String placeName = candidates.get(context.getIndex() - 1).getTitle();
                    sb.append(String.format("- 선택한 장소: %d번 (%s)\n", context.getIndex(), placeName));
                }
                if (context.getDayIndex() != null) {
                    sb.append(String.format("- 추가할 일차: %d일차\n", context.getDayIndex()));
                }
                if (context.getPosition() != null) {
                    sb.append(String.format("- 추가할 위치: %d번째\n", context.getPosition()));
                }
            }

            sb.append("\n다음 동작:\n");
            sb.append("- 사용자가 '1번', '2번' 같은 숫자를 말하면 → addPlace 사용\n");
            sb.append("- 사용자가 '3일차에', '첫 번째에' 같은 정보를 말하면 → 정보 저장 후 부족한 부분 물어보기\n");
            sb.append("- 사용자가 완전히 새로운 요청을 하면 → 해당 요청에 맞는 Tool 사용 (기존 후보는 무시)\n");
            sb.append("</context>");

            return sb.toString();
        }

        return "<context>현재 상태를 확인할 수 없습니다.</context>";
    }

    // =========================
    // 내부 클래스: SelectionContext
    // =========================
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SelectionContext {
        private Integer dayIndex;
        private Integer position;
        private Integer index; // 추천 번호 or 후보 번호 (선택적)
    }
}
