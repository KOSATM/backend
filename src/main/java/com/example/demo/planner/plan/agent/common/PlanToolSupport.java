package com.example.demo.planner.plan.agent.common;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.demo.planner.plan.dao.PlanDao;
import com.example.demo.planner.plan.dao.PlanDayDao;
import com.example.demo.planner.plan.dao.PlanPlaceDao;
import com.example.demo.planner.plan.dto.entity.Plan;
import com.example.demo.planner.plan.dto.entity.PlanDay;
import com.example.demo.planner.plan.dto.entity.PlanPlace;
import com.example.demo.planner.plan.dto.entity.PlanSnapshot;
import com.example.demo.planner.plan.service.PlanSnapshotService;
import com.example.demo.planner.plan.utils.CategoryNames;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PlanToolSupport {

    private final PlanDao planDao;
    private final PlanDayDao planDayDao;
    private final PlanPlaceDao planPlaceDao;
    private final PlanSnapshotService planSnapshotService;

    private final ThreadLocal<Long> currentPlanId = new ThreadLocal<>();

    // ========== planId 관리 ==========

    public void setPlanId(Long planId) {
        currentPlanId.set(planId);
    }

    public void clearPlanId() {
        currentPlanId.remove();
    }

    public Long getPlanId() {
        return currentPlanId.get();
    }

    // ========== 공통: 스냅샷 저장 ==========

    public Integer saveSnapshot(Long planId) throws Exception {
        Plan plan = planDao.selectPlanById(planId);
        List<PlanDay> planDays = planDayDao.selectPlanDaysByPlanId(planId);
        List<PlanPlace> planPlaces = planPlaceDao.selectPlanPlacesByPlanId(planId);
        PlanSnapshot snapshot;
            snapshot = planSnapshotService.savePlanSnapshot(plan, planDays, planPlaces);
            return snapshot.getVersionNo();
    }
    
    
    public void deleteAllSnapshot(Long userId) throws Exception {
        planSnapshotService.deletePlanSnapshotsByUserId(userId);
    }

    // ========== 공통: Plan 조회 ==========

    public Plan loadPlan(Long planId) {
        Plan plan = planDao.selectPlanById(planId);
        if (plan == null) {
            throw new IllegalArgumentException("Plan not found: " + planId);
        }
        return plan;
    }

    public List<PlanDay> loadDays(Long planId) {
        return planDayDao.selectPlanDaysByPlanId(planId);
    }

    // ✅ 추가: 특정 일차 조회
    public PlanDay loadDayByIndex(Long planId, Integer dayIndex) {
        PlanDay day = planDayDao.selectPlanDayByPlanIdAndDayIndex(planId, dayIndex);
        if (day == null) {
            throw new IllegalArgumentException(dayIndex + "일차를 찾을 수 없습니다.");
        }
        return day;
    }

    // 기존: Days 리스트로 Places Map 조회
    public Map<Long, List<PlanPlace>> loadPlacesByDayId(List<PlanDay> days) {
        Map<Long, List<PlanPlace>> map = new HashMap<>();
        for (PlanDay day : days) {
            List<PlanPlace> places = planPlaceDao.selectPlanPlacesByPlanDayId(day.getId());
            map.put(day.getId(), places);
        }
        return map;
    }

    // ✅ 추가: 특정 dayId로 Places 조회 (오버로드)
    public List<PlanPlace> loadPlacesByDayId(Long dayId) {
        return planPlaceDao.selectPlanPlacesByPlanDayId(dayId);
    }

    // ========== 공통: 렌더링 ==========

    public String renderPlan(Plan plan, List<PlanDay> planDays, Map<Long, List<PlanPlace>> placesByDayId) {
        StringBuilder sb = new StringBuilder();

        sb.append("📅 ").append(plan.getTitle() != null ? plan.getTitle() : "여행 일정").append("\n");
        sb.append("기간: ").append(plan.getStartDate())
                .append(" ~ ").append(plan.getEndDate()).append("\n");


        for (PlanDay day : planDays) {
            sb.append("=== Day ").append(day.getDayIndex()).append(" ===\n");

            if (day.getTitle() != null) {
                sb.append(day.getTitle()).append("\n");
            }
            sb.append(day.getPlanDate()).append("\n\n");

            List<PlanPlace> places = placesByDayId.get(day.getId());

            if (places == null || places.isEmpty()) {
                sb.append("등록된 장소가 없습니다.\n\n");
                continue;
            }

            for (PlanPlace place : places) {
                sb.append("• ");

                if (place.getStartAt() != null) {
                    sb.append(place.getStartAt().toLocalTime()).append(" ");
                }

                sb.append(place.getTitle()).append("\n");

                if (place.getAddress() != null) {
                    sb.append("  🗺️ ").append(place.getAddress()).append("\n");
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }

        public String renderDay(PlanDay day, List<PlanPlace> places) {
        StringBuilder sb = new StringBuilder();

        sb.append("📅 Day ").append(day.getDayIndex()).append("\n");
        sb.append("🗓 날짜: ").append(day.getPlanDate()).append("\n");
        sb.append("────────────────────\n");

        if (places == null || places.isEmpty()) {
            sb.append("⚠️ 일정에 등록된 장소가 없습니다.\n");
            return sb.toString();
        }

        for (PlanPlace place : places) {
            // 시간
            sb.append("• ");
            // if (place.getStartAt() != null) {
            //     sb.append("⏰ ")
            //             .append(place.getStartAt().toLocalTime())
            //             .append(" ");
            // }

            // 장소명
            sb.append(place.getTitle());

            // 카테고리
            if (place.getNormalizedCategory() != null) {
                sb.append("  [")
                        .append(CategoryNames.categoryLabel(place.getNormalizedCategory()))
                        .append("]");
            }
            sb.append("\n");

            // 주소
            if (place.getAddress() != null) {
                sb.append("   주소: ")
                        .append(place.getAddress())
                        .append("\n");
            }

            sb.append("\n");
        }
        return sb.toString();
    }
}
