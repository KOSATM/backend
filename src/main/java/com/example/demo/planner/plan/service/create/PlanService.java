package com.example.demo.planner.plan.service.create;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.planner.plan.dao.PlanDao;
import com.example.demo.planner.plan.dao.PlanDayDao;
import com.example.demo.planner.plan.dao.PlanPlaceDao;
import com.example.demo.planner.plan.dto.entity.Plan;
import com.example.demo.planner.plan.dto.entity.PlanDay;
import com.example.demo.planner.plan.dto.entity.PlanPlace;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// Plan 엔티티 CRUD 전용 서비스
@Service
@Slf4j
@RequiredArgsConstructor
public class PlanService {

    private final PlanDao planDao;
    private final PlanDayDao planDayDao;
    private final PlanPlaceDao planPlaceDao;

    // =========================================================
    // PLAN 조회 (READ) - 기본 조회만 유지, 상세 조회는 PlanQueryService 사용
    // =========================================================

    // Plan 단건 조회 by ID
    public Plan findById(Long planId) {
        log.info("Plan 조회: planId={}", planId);
        Plan plan = planDao.selectPlanById(planId);
        if (plan == null) {
            throw new IllegalArgumentException("존재하지 않는 여행 계획입니다: planId=" + planId);
        }
        return plan;
    }

    // 사용자별 Plan 목록 조회
    public List<Plan> findByUserId(Long userId) {
        log.info("사용자별 Plan 목록 조회: userId={}", userId);
        return planDao.selectPlansByUserId(userId);
    }

    // 사용자의 활성(진행 중인) 여행 계획 조회 (isEnded=false 또는 NULL인 Plan 반환)
    public Plan findActiveByUserId(Long userId) {
        log.info("활성 Plan 조회: userId={}", userId);
        Plan activePlan = planDao.selectActiveTravelPlanByUserId(userId);
        log.info("활성 Plan 조회 결과: {}", activePlan);
        return activePlan;
    }

    // =========================================================
    // PLAN 생성 / 수정 / 삭제
    // =========================================================

    /**
     * 여행 계획 생성 (빈 Plan만)
     */
    public Plan createPlan(Plan plan) {
        log.info("여행 계획 생성: userId={}", plan.getUserId());

        if (plan.getIsEnded() != null) {
            throw new IllegalArgumentException("isEnded는 생성 시 입력할 수 없습니다. /complete 엔드포인트를 사용하세요.");
        }

        if (plan.getStartDate() != null && plan.getStartDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("여행 시작일은 오늘 이후여야 합니다.");
        }

        Plan newPlan = Plan.builder()
                .userId(plan.getUserId())
                .budget(plan.getBudget())
                .startDate(plan.getStartDate())
                .endDate(plan.getEndDate())
                .isEnded(false)
                .title(plan.getTitle())
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        planDao.insertPlan(newPlan);
        log.info("여행 계획 생성 완료: planId={}", newPlan.getId());
        return newPlan;
    }

    /**
     * 샘플 데이터 포함 Plan 생성 (Agent용)
     */
    public Plan createPlanWithSampleData(Long userId, Integer days, BigDecimal budget, LocalDate startDate) {
        if (days == null)
            days = 3;
        if (budget == null)
            budget = new BigDecimal("500000");
        if (startDate == null)
            startDate = LocalDate.now();

        if (startDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("여행 시작일은 오늘 이후여야 합니다.");
        }

        log.info("샘플 데이터 포함 여행 계획 생성 시작: userId={}, days={}", userId, days);

        Plan plan = Plan.builder()
                .userId(userId)
                .budget(budget)
                .startDate(startDate)
                .endDate(startDate.plusDays(days - 1))
                .isEnded(false)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        planDao.insertPlan(plan);
        Long planId = plan.getId();
        log.info("Plan 생성 완료: planId={}", planId);

        for (int i = 1; i <= days; i++) {
            LocalDate currentDate = startDate.plusDays(i - 1);

            PlanDay day = PlanDay.builder()
                    .planId(planId)
                    .dayIndex(i)
                    .title("Day " + i)
                    .planDate(currentDate)
                    .build();
            planDayDao.insertPlanDay(day);
            Long dayId = day.getId();

            PlanPlace morningPlace = PlanPlace.builder()
                    .dayId(dayId)
                    .title("Morning Activity")
                    .placeName("Sample Place " + i + "-1")
                    .address("Seoul, South Korea")
                    .lat(37.5665)
                    .lng(126.9780)
                    .startAt(OffsetDateTime.of(currentDate, LocalTime.of(9, 0), ZoneOffset.ofHours(9)))
                    .endAt(OffsetDateTime.of(currentDate, LocalTime.of(12, 0), ZoneOffset.ofHours(9)))
                    .expectedCost(new BigDecimal("20000"))
                    .build();
            planPlaceDao.insertPlanPlace(morningPlace);

            PlanPlace afternoonPlace = PlanPlace.builder()
                    .dayId(dayId)
                    .title("Afternoon Activity")
                    .placeName("Sample Place " + i + "-2")
                    .address("Seoul, South Korea")
                    .lat(37.4979)
                    .lng(127.0276)
                    .startAt(OffsetDateTime.of(currentDate, LocalTime.of(14, 0), ZoneOffset.ofHours(9)))
                    .endAt(OffsetDateTime.of(currentDate, LocalTime.of(18, 0), ZoneOffset.ofHours(9)))
                    .expectedCost(new BigDecimal("30000"))
                    .build();
            planPlaceDao.insertPlanPlace(afternoonPlace);
        }

        log.info("샘플 데이터 포함 여행 계획 생성 완료: planId={}, 총 {}일, {}개 장소", planId, days, days * 2);
        return plan;
    }

    /**
     * Plan 수정 (부분 수정 지원) - Plan 엔티티 기반
     */
    public void updatePlan(Long planId, Plan plan) {
        Plan existing = planDao.selectPlanById(planId);
        if (existing == null) {
            throw new IllegalArgumentException("존재하지 않는 여행 계획입니다: planId=" + planId);
        }

        if (plan.getUserId() != null && !plan.getUserId().equals(existing.getUserId())) {
            throw new IllegalArgumentException("userId는 수정할 수 없습니다.");
        }

        Plan updatedPlan = Plan.builder()
                .id(planId)
                .userId(existing.getUserId())
                .budget(plan.getBudget() != null ? plan.getBudget() : existing.getBudget())
                .startDate(plan.getStartDate() != null ? plan.getStartDate() : existing.getStartDate())
                .endDate(plan.getEndDate() != null ? plan.getEndDate() : existing.getEndDate())
                .createdAt(existing.getCreatedAt())
                .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .isEnded(plan.getIsEnded() != null ? plan.getIsEnded() : existing.getIsEnded())
                .title(plan.getTitle() != null ? plan.getTitle() : existing.getTitle())
                .build();

        planDao.updatePlan(updatedPlan);
        log.info("Plan 수정 완료: planId={}", planId);
    }

    /**
     * Plan 정보만 수정 (제목, 예산)
     */
    @Transactional
    public void updatePlanInfo(Long planId, String title, BigDecimal budget) {
        log.info("Plan 정보 수정: planId={}, title={}, budget={}", planId, title, budget);

        Plan existing = planDao.selectPlanById(planId);
        if (existing == null) {
            throw new IllegalArgumentException("존재하지 않는 여행 계획입니다: planId=" + planId);
        }

        Plan updated = Plan.builder()
                .id(planId)
                .userId(existing.getUserId())
                .budget(budget != null ? budget : existing.getBudget())
                .startDate(existing.getStartDate())
                .endDate(existing.getEndDate())
                .createdAt(existing.getCreatedAt())
                .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .isEnded(existing.getIsEnded())
                .title(title != null ? title : existing.getTitle())
                .build();

        planDao.updatePlan(updated);
        log.info("Plan 정보 수정 완료");
    }

    /**
     * 여행 완료 처리
     */
    public Plan completePlan(Long planId) {
        Plan existing = planDao.selectPlanById(planId);
        if (existing == null) {
            throw new IllegalArgumentException("존재하지 않는 여행 계획입니다: planId=" + planId);
        }

        if (Boolean.TRUE.equals(existing.getIsEnded())) {
            throw new IllegalArgumentException("이미 완료된 여행입니다: planId=" + planId);
        }

        Plan completedPlan = Plan.builder()
                .id(planId)
                .userId(existing.getUserId())
                .budget(existing.getBudget())
                .startDate(existing.getStartDate())
                .endDate(existing.getEndDate())
                .createdAt(existing.getCreatedAt())
                .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .isEnded(true)
                .title(existing.getTitle())
                .build();

        planDao.updatePlan(completedPlan);
        log.info("여행 완료 처리: planId={}", planId);
        return completedPlan;
    }

    /**
     * 여행 전체 기간 변경 (startDate, endDate 수정 + Day 재배치)
     */
    @Transactional
    public void updatePlanDates(Long planId, LocalDate newStartDate, LocalDate newEndDate) {
        log.info("여행 기간 변경: planId={}, newStart={}, newEnd={}", planId, newStartDate, newEndDate);

        Plan plan = planDao.selectPlanById(planId);
        if (plan == null) {
            throw new IllegalArgumentException("Plan not found: " + planId);
        }

        planDao.updatePlanDates(planId, newStartDate, newEndDate);

        List<PlanDay> days = planDayDao.selectPlanDaysByPlanId(planId);
        for (PlanDay day : days) {
            LocalDate newDate = newStartDate.plusDays(day.getDayIndex() - 1);
            planDayDao.updatePlanDate(day.getId(), newDate);
            log.info("Day {} updated to {}", day.getDayIndex(), newDate);
        }
    }

    /**
     * Plan 삭제 (연관 Day/Place 포함)
     */
    public void deletePlan(Long planId) {
        Plan existing = planDao.selectPlanById(planId);
        if (existing == null) {
            throw new IllegalArgumentException("존재하지 않는 여행 계획입니다: planId=" + planId);
        }

        List<PlanDay> days = planDayDao.selectPlanDaysByPlanId(planId);
        for (PlanDay day : days) {
            planPlaceDao.deletePlacesByDayId(day.getId());
        }
        planDayDao.deletePlanDaysByPlanId(planId);

        planDao.deletePlan(planId);
        log.info("Plan 삭제 완료: planId={}", planId);
    }

}
