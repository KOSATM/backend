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
import com.example.demo.planner.plan.service.PlanSnapshotService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PlanToolSupport {

    private final PlanDao planDao;
    private final PlanDayDao planDayDao;
    private final PlanPlaceDao planPlaceDao;
    private final PlanSnapshotService planSnapshotService;

    // =========================
    // conversationId 기반 상태 저장
    // =========================
    private final Map<String, Long> planIdByConversation = new ConcurrentHashMap<>();
    private final Map<String, List<Map<String, Object>>> lastRecommendationsByConversation = new ConcurrentHashMap<>();
    private final Map<String, Set<Long>> recommendedPlaceIdsByConversation = new ConcurrentHashMap<>();

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
    }

    /**
     * 일정이 없을 수도 있으므로 null 반환 가능
     */
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
    }

    // =========================
    // 추천 여행지 관리
    // =========================
    public void setLastRecommendations(String conversationId, List<Map<String, Object>> list) {
        if (conversationId == null || conversationId.isBlank()) {
            return;
        }
        if (list == null || list.isEmpty()) {
            lastRecommendationsByConversation.remove(conversationId);
            return;
        }
        lastRecommendationsByConversation.put(conversationId, new ArrayList<>(list));
    }

    public List<Map<String, Object>> getLastRecommendations(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return Collections.emptyList();
        }
        return lastRecommendationsByConversation.getOrDefault(conversationId, Collections.emptyList());
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
    }

    // =========================
    // 전체 상태 초기화
    // =========================
    public void clear(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return;
        }
        planIdByConversation.remove(conversationId);
        lastRecommendationsByConversation.remove(conversationId);
        recommendedPlaceIdsByConversation.remove(conversationId);
    }

    // =========================
    // 스냅샷 저장
    // =========================
    public Integer saveSnapshot(Long planId) throws Exception {
        if (planId == null) {
            throw new IllegalStateException("저장할 일정이 없습니다.");
        }

        Plan plan = planDao.selectPlanById(planId);
        List<PlanDay> planDays = planDayDao.selectPlanDaysByPlanId(planId);
        List<PlanPlace> planPlaces = planPlaceDao.selectPlanPlacesByPlanId(planId);

        PlanSnapshot snapshot = planSnapshotService.savePlanSnapshot(plan, planDays, planPlaces);
        return snapshot.getVersionNo();
    }

    // =========================
    // 스냅샷 전체 삭제
    // =========================
    public void deleteAllSnapshot(Long userId) throws Exception {
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
        return plan;
    }

    public List<PlanDay> loadDays(Long planId) {
        if (planId == null) {
            return Collections.emptyList();
        }
        return planDayDao.selectPlanDaysByPlanId(planId);
    }

    public PlanDay loadDayByIndex(Long planId, Integer dayIndex) {
        if (planId == null) {
            throw new IllegalStateException("일정이 없습니다.");
        }
        PlanDay day = planDayDao.selectPlanDayByPlanIdAndDayIndex(planId, dayIndex);
        if (day == null) {
            throw new IllegalArgumentException(dayIndex + "일차를 찾을 수 없습니다.");
        }
        return day;
    }

    public Map<Long, List<PlanPlace>> loadPlacesByDayId(List<PlanDay> days) {
        Map<Long, List<PlanPlace>> map = new HashMap<>();
        for (PlanDay day : days) {
            map.put(day.getId(),
                    planPlaceDao.selectPlanPlacesByPlanDayId(day.getId()));
        }
        return map;
    }

    public List<PlanPlace> loadPlacesByDayId(Long dayId) {
        if (dayId == null) {
            return Collections.emptyList();
        }
        return planPlaceDao.selectPlanPlacesByPlanDayId(dayId);
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
        return sb.toString();
    }

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

    // 추천 장소 상태
    List<Map<String, Object>> recs = getLastRecommendations(conversationId);
    if (recs != null && !recs.isEmpty()) {
        sb.append("- 추천 장소 목록: EXISTS\n");
        sb.append("- 추천 장소 개수: ").append(recs.size()).append("\n");
        sb.append("- 사용자는 추천 번호(1번, 2번 등)로 ")
          .append("추천 장소를 일정에 추가할 수 있음\n");
    } else {
        sb.append("- 추천 장소 목록: NONE\n");
    }

    return sb.toString();
}

}
