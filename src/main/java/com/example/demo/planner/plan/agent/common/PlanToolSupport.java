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

        // ✅ 상호 배타적: 장소 후보 클리어
        lastAddPlaceCandidatesByConversation.remove(conversationId);
        selectionContextByConversation.remove(conversationId); // ✅ 컨텍스트도 클리어!

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

        // ✅ 상호 배타적: 추천 목록 클리어
        lastRecommendationsByConversation.remove(conversationId);

        lastAddPlaceCandidatesByConversation.put(conversationId, new ArrayList<>(candidates));

        // ✅ 컨텍스트 새로 설정
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
     * ✅ 추천 선택 컨텍스트 저장
     * - ❗️덮어쓰기 금지: index만 업데이트하고 day/position은 보존
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
     * ✅ 선택 컨텍스트 조회 (후보/추천 공통)
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
        StringBuilder sb = new StringBuilder();

        sb.append("[STATE]\n");

        // 여행 일정 존재 여부
        Long planId = getPlanId(conversationId);
        if (planId != null) {
            sb.append("- 여행 일정: EXISTS\n");
        } else {
            sb.append("- 여행 일정: NONE\n");
        }

        // 선택 대기 상태
        PendingSelectionType selectionType = getPendingSelectionType(conversationId);

        switch (selectionType) {
            case RECOMMENDATION:
                List<Map<String, Object>> recs = getLastRecommendations(conversationId);
                sb.append("- 대기 중인 선택: 추천 목록\n");
                sb.append("- 추천 장소 개수: ").append(recs.size()).append("\n");
                sb.append("⚠️ 사용자가 번호를 선택하면 addRecommendedPlace 재호출\n");
                sb.append("⚠️ 다른 Tool을 호출하지 마세요!\n");
                break;

            case ADD_CANDIDATE:
                List<TravelPlaces> candidates = getAddPlaceCandidates(conversationId);
                SelectionContext context = getSelectionContext(conversationId);
                sb.append("- 대기 중인 선택: 장소 후보\n");
                sb.append("- 후보 개수: ").append(candidates.size()).append("\n");

                if (context != null) {
                    if (context.getDayIndex() != null) {
                        sb.append("- 이미 입력된 일차: ").append(context.getDayIndex()).append("일차\n");
                    }
                    if (context.getPosition() != null) {
                        sb.append("- 이미 입력된 위치: ").append(context.getPosition()).append("번째\n");
                    }
                }

                sb.append("⚠️ 사용자 답변을 받으면 addPlace 재호출\n");
                sb.append("⚠️ viewDay, viewPlan 등 다른 Tool을 호출하지 마세요!\n");
                break;

            case NONE:
                sb.append("- 대기 중인 선택: NONE\n");
                break;
        }

        return sb.toString();
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
