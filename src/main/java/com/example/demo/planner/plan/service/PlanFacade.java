package com.example.demo.planner.plan.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.planner.plan.dto.entity.Plan;
import com.example.demo.planner.plan.dto.entity.PlanDay;
import com.example.demo.planner.plan.dto.entity.PlanPlace;
import com.example.demo.planner.plan.dto.response.ActivePlanInfoResponse;
import com.example.demo.planner.plan.dto.response.MovePreview;
import com.example.demo.planner.plan.dto.response.PlanDetail;
import com.example.demo.planner.plan.dto.response.PlanSnapshotContent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Plan 관련 모든 기능의 Facade
 * - 컨트롤러는 이 Facade만 주입받음
 * - 내부적으로 CRUD/Query/Action 서비스들을 조합
 * - 트랜잭션 경계 관리
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PlanFacade {

    // CRUD Services
    private final PlanCrudService planCrudService;
    private final PlanDayService planDayService;
    private final PlanPlaceService planPlaceService;

    // Query Service
    private final PlanQueryService planQueryService;

    // Utilities
    private final PlanSnapshotUtility planSnapshotUtility;

    // ==================== Plan CRUD ====================

    @Transactional
    public Plan createPlan(Plan plan) {
        return planCrudService.createPlan(plan);
    }

    @Transactional
    public Plan createPlanWithSampleData(Long userId, Integer days, BigDecimal budget, LocalDate startDate) {
        return planCrudService.createPlanWithSampleData(userId, days, budget, startDate);
    }

    public Plan findPlanById(Long planId) {
        return planCrudService.findById(planId);
    }

    public List<Plan> findPlansByUserId(Long userId) {
        return planCrudService.findByUserId(userId);
    }

    @Transactional
    public void updatePlan(Long planId, Plan plan) {
        planCrudService.updatePlan(planId, plan);
    }

    @Transactional
    public void deletePlan(Long planId) {
        planCrudService.deletePlan(planId);
    }

    @Transactional
    public void completePlan(Long planId) {
        planCrudService.completePlan(planId);
    }

    // ==================== Plan Query ====================

    public ActivePlanInfoResponse getActivePlanIdAndDayIndex(Long userId) {
        return planQueryService.getActivePlanIdAndDayIndex(userId);
    }

    public PlanDetail getPlanDetail(Long planId) {
        return planQueryService.getPlanDetail(planId);
    }

    public PlanDetail getLatestPlanDetail(Long userId) {
        return planQueryService.getLatestPlanDetail(userId);
    }

    public List<PlanDetail> getPlanDetailsByUserId(Long userId) {
        return planQueryService.getPlanDetailsByUserId(userId);
    }

    // ==================== PlanDay CRUD ====================

    @Transactional
    public PlanDay createDay(PlanDay day, Boolean confirm) {
        return planDayService.createDay(day, confirm);
    }

    public PlanDay findDayById(Long dayId) {
        return planDayService.findDayById(dayId);
    }

    @Transactional
    public void updateDay(Long dayId, PlanDay day) {
        planDayService.updateDay(dayId, day);
    }

    @Transactional
    public void deleteDay(Long dayId) {
        planDayService.deleteDay(dayId);
    }

    // ==================== PlanDay Actions ====================

    @Transactional
    public PlanDetail moveDay(Long dayId, Integer toIndex, Boolean confirm) {
        return planDayService.moveDay(dayId, toIndex, confirm, planQueryService);
    }

    public MovePreview movePreview(Long dayId, Integer toIndex) {
        return planDayService.movePreview(dayId, toIndex);
    }

    public MovePreview createDayPreview(Long planId, Integer dayIndex) {
        return planDayService.createDayPreview(planId, dayIndex);
    }

    // ==================== PlanPlace CRUD ====================

    @Transactional
    public PlanPlace createPlace(PlanPlace place) {
        return planPlaceService.createPlace(place);
    }

    public PlanPlace findPlaceById(Long placeId) {
        return planPlaceService.findPlaceById(placeId);
    }

    @Transactional
    public void updatePlace(Long placeId, PlanPlace place) {
        planPlaceService.updatePlace(placeId, place);
    }

    @Transactional
    public void deletePlace(Long placeId) {
        planPlaceService.deletePlace(placeId);
    }

    // ==================== Special Functions ====================

    public PlanSnapshotContent parseSnapshot(String snapshotJson) throws Exception {
        return planSnapshotUtility.parseSnapshot(snapshotJson);
    }
}
