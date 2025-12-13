package com.example.demo.planner.plan.service;

import com.example.demo.common.user.dao.UserDao;
import com.example.demo.common.user.dto.User;
import com.example.demo.planner.plan.dao.PlanDao;
import com.example.demo.planner.plan.dao.PlanDayDao;
import com.example.demo.planner.plan.dao.PlanPlaceDao;
import com.example.demo.planner.plan.dto.entity.Plan;
import com.example.demo.planner.plan.dto.entity.PlanDay;
import com.example.demo.planner.plan.dto.entity.PlanPlace;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Plan(여행 계획) 관련 CRUD 서비스
 * - Plan CRUD (생성, 조회, 수정, 삭제)
 * - 샘플 데이터 포함 Plan 생성
 * - Plan 완료 처리
 * - 활성 Plan 조회
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanCrudService {

    private final PlanDao planDao;
    private final PlanDayDao planDayDao;
    private final PlanPlaceDao planPlaceDao;
    private final UserDao userDao;

    // ========== Plan CRUD ==========

    /**
     * Plan 생성 (기본)
     */
    @Transactional
    public Plan createPlan(Plan plan) {
        // Validation
        if (plan.getUserId() == null) {
            throw new IllegalArgumentException("userId는 필수입니다.");
        }
        if (plan.getTitle() == null || plan.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("여행 제목은 필수입니다.");
        }
        if (plan.getStartDate() == null || plan.getEndDate() == null) {
            throw new IllegalArgumentException("여행 시작일과 종료일은 필수입니다.");
        }
        if (plan.getEndDate().isBefore(plan.getStartDate())) {
            throw new IllegalArgumentException("종료일은 시작일보다 이전일 수 없습니다.");
        }

        // 기본값 설정 (builder로 재생성)
        Plan planToInsert = Plan.builder()
                .userId(plan.getUserId())
                .title(plan.getTitle())
                .budget(plan.getBudget())
                .startDate(plan.getStartDate())
                .endDate(plan.getEndDate())
                .isEnded(plan.getIsEnded() != null ? plan.getIsEnded() : false)
                .createdAt(plan.getCreatedAt() != null ? plan.getCreatedAt() : OffsetDateTime.now(ZoneId.of("Asia/Seoul")))
                .updatedAt(plan.getUpdatedAt() != null ? plan.getUpdatedAt() : OffsetDateTime.now(ZoneId.of("Asia/Seoul")))
                .build();

        planDao.insertPlan(planToInsert);
        log.info("✅ Plan 생성 완료: planId={}, title={}", planToInsert.getId(), planToInsert.getTitle());
        return planToInsert;
    }

    /**
     * Plan 생성 (샘플 데이터 포함)
     * - Plan + 3일 일정 + 각 일정당 2개 장소 생성
     */
    @Transactional
    public Plan createPlanWithSampleData(Long userId, String title,
                                          LocalDate startDate, LocalDate endDate) {
        log.info("샘플 데이터 포함 Plan 생성: userId={}, title={}", userId, title);

        // 1. Plan 생성
        Plan plan = Plan.builder()
                .userId(userId)
                .title(title)
                .startDate(startDate)
                .endDate(endDate)
                .isEnded(false)
                .createdAt(OffsetDateTime.now(ZoneId.of("Asia/Seoul")))
                .updatedAt(OffsetDateTime.now(ZoneId.of("Asia/Seoul")))
                .build();
        planDao.insertPlan(plan);
        log.info("Plan 생성 완료: planId={}", plan.getId());

        // 2. 3일 일정 생성
        for (int i = 1; i <= 3; i++) {
            LocalDate planDate = startDate.plusDays(i - 1);
            PlanDay day = PlanDay.builder()
                    .planId(plan.getId())
                    .dayIndex(i)
                    .planDate(planDate)
                    .build();
            planDayDao.insertPlanDay(day);
            log.info("Day {} 생성 완료: dayId={}, date={}", i, day.getId(), planDate);

            // 3. 각 일정당 2개 장소 생성
            for (int j = 1; j <= 2; j++) {
                LocalTime startTime = LocalTime.of(9 + (j - 1) * 3, 0);
                OffsetDateTime startAt = OffsetDateTime.of(planDate, startTime, ZoneId.of("Asia/Seoul").getRules().getOffset(planDate.atTime(startTime)));
                OffsetDateTime endAt = startAt.plusHours(2);

                PlanPlace place = PlanPlace.builder()
                        .dayId(day.getId())
                        .title("샘플 장소 " + i + "-" + j)
                        .placeName("샘플 장소 " + i + "-" + j)
                        .address("서울특별시 강남구")
                        .lat(37.5 + (i * 0.01) + (j * 0.001))
                        .lng(127.0 + (i * 0.01) + (j * 0.001))
                        .startAt(startAt)
                        .endAt(endAt)
                        .expectedCost(new BigDecimal("10000"))
                        .build();
                planPlaceDao.insertPlanPlace(place);
                log.info("Place {} 생성 완료: placeId={}, name={}", j, place.getId(), place.getPlaceName());
            }
        }

        log.info("✅ 샘플 데이터 포함 Plan 생성 완료: planId={}, 3일 + 6개 장소", plan.getId());
        return plan;
    }

    /**
     * Plan 조회 (단일)
     */
    public Plan findById(Long planId) {
        log.info("Plan 조회: planId={}", planId);
        Plan plan = planDao.selectPlanById(planId);
        if (plan == null) {
            throw new IllegalArgumentException("존재하지 않는 Plan입니다: planId=" + planId);
        }
        return plan;
    }

    /**
     * Plan 조회 (사용자별 전체)
     */
    public List<Plan> findByUserId(Long userId) {
        log.info("사용자별 Plan 조회: userId={}", userId);
        return planDao.selectPlansByUserId(userId);
    }

    /**
     * 활성 Plan 조회 (isEnded = false)
     */
    public Plan findActiveByUserId(Long userId) {
        log.info("활성 Plan 조회: userId={}", userId);

        User user = userDao.selectUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("존재하지 않는 사용자입니다: userId=" + userId);
        }

        Plan plan = planDao.selectActiveTravelPlanByUserId(userId);
        if (plan == null) {
            throw new IllegalArgumentException("활성화된 Plan이 없습니다: userId=" + userId);
        }

        return plan;
    }

    /**
     * Plan 수정 (부분 수정 지원)
     */
    @Transactional
    public void updatePlan(Long planId, Plan plan) {
        Plan existing = planDao.selectPlanById(planId);
        if (existing == null) {
            throw new IllegalArgumentException("존재하지 않는 Plan입니다: planId=" + planId);
        }

        // null이 아닌 필드만 업데이트
        Plan updatedPlan = Plan.builder()
                .id(planId)
                .userId(existing.getUserId())  // userId는 변경 불가
                .title(plan.getTitle() != null ? plan.getTitle() : existing.getTitle())
                .budget(plan.getBudget() != null ? plan.getBudget() : existing.getBudget())
                .startDate(plan.getStartDate() != null ? plan.getStartDate() : existing.getStartDate())
                .endDate(plan.getEndDate() != null ? plan.getEndDate() : existing.getEndDate())
                .isEnded(plan.getIsEnded() != null ? plan.getIsEnded() : existing.getIsEnded())
                .createdAt(existing.getCreatedAt())
                .updatedAt(OffsetDateTime.now(ZoneId.of("Asia/Seoul")))
                .build();

        planDao.updatePlan(updatedPlan);
        log.info("Plan 수정 완료: planId={}", planId);
    }

    /**
     * Plan 완료 처리 (isEnded = true)
     */
    @Transactional
    public void completePlan(Long planId) {
        log.info("Plan 완료 처리: planId={}", planId);

        Plan existing = planDao.selectPlanById(planId);
        if (existing == null) {
            throw new IllegalArgumentException("존재하지 않는 Plan입니다: planId=" + planId);
        }

        // isEnded를 true로 변경
        Plan updatedPlan = Plan.builder()
                .id(planId)
                .userId(existing.getUserId())
                .title(existing.getTitle())
                .budget(existing.getBudget())
                .startDate(existing.getStartDate())
                .endDate(existing.getEndDate())
                .isEnded(true)
                .createdAt(existing.getCreatedAt())
                .updatedAt(OffsetDateTime.now(ZoneId.of("Asia/Seoul")))
                .build();

        planDao.updatePlan(updatedPlan);
        log.info("✅ Plan 완료 처리 완료: planId={}", planId);
    }

    /**
     * Plan 삭제 (cascade: Days + Places 모두 삭제)
     */
    @Transactional
    public void deletePlan(Long planId) {
        log.info("Plan 삭제 시작: planId={}", planId);

        Plan existing = planDao.selectPlanById(planId);
        if (existing == null) {
            throw new IllegalArgumentException("존재하지 않는 Plan입니다: planId=" + planId);
        }

        // 1. 모든 Day 조회
        List<PlanDay> days = planDayDao.selectPlanDaysByPlanId(planId);

        // 2. 각 Day의 Places 삭제
        int totalPlaces = 0;
        for (PlanDay day : days) {
            List<PlanPlace> places = planPlaceDao.selectPlanPlacesByPlanDayId(day.getId());
            for (PlanPlace place : places) {
                planPlaceDao.deletePlanPlaceById(place.getId());
                totalPlaces++;
            }
            // 3. Day 삭제
            planDayDao.deletePlanDayById(day.getId());
        }

        // 4. Plan 삭제
        planDao.deletePlan(planId);

        log.info("✅ Plan 삭제 완료: planId={}, {}개 Day, {}개 Place 삭제됨",
                planId, days.size(), totalPlaces);
    }
}
