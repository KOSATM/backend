package com.example.demo.planner.plan.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.planner.plan.dao.PlanDao;
import com.example.demo.planner.plan.dao.PlanDayDao;
import com.example.demo.planner.plan.dao.PlanPlaceDao;
import com.example.demo.planner.plan.dto.entity.GeneratedTravelPlan;
import com.example.demo.planner.plan.dto.entity.Plan;
import com.example.demo.planner.plan.dto.entity.PlanDay;
import com.example.demo.planner.plan.dto.entity.PlanPlace;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class TravelPlanSaveService {

    private final PlanPlaceDao planPlaceDao;
    private final PlanDayDao planDayDao;
    private final PlanDao planDao;

    // 일정 저장
    @Transactional
    public Long save(Long userId, GeneratedTravelPlan generated, ObjectMapper objectMapper) {
        log.info("💾 여행 일정 저장 시작 ({}일)", generated.duration());

        // 1️⃣ Plan (단건)
        Plan plan = Plan.builder()
                .userId(userId)
                .title(null)
                .budget(BigDecimal.ZERO)
                .startDate(generated.startDate())
                .endDate(generated.endDate())
                .isEnded(false)
                .build();

        planDao.insertPlan(plan);
        Long planId = plan.getId();

        // 2️⃣ Days (리스트)
        // 2️⃣ Days 저장
        List<PlanDay> planDays = new ArrayList<>();

        for (GeneratedTravelPlan.GeneratedDay day : generated.days()) {
            PlanDay planDay = PlanDay.builder()
                    .planId(planId)
                    .dayIndex(day.dayIndex())
                    .title(null)
                    .planDate(day.dayDate())
                    .build();

            planDays.add(planDay);
        }

        planDayDao.insertPlanDayBatch(planDays);

        // 3️⃣ Day ID 조회 (정렬된 상태)
        List<Long> planDayIds = planDayDao.selectPlanDayIdsByPlanId(planId);
        // size 검증 (중요)
        if (planDayIds.size() != generated.days().size()) {
            throw new IllegalStateException(
                    "plan_days 개수 불일치: expected=" + generated.days().size()
                            + ", actual=" + planDayIds.size());
        }

        // 3️⃣ Places (리스트)
        List<PlanPlace> planPlaces = new ArrayList<>();

        for (int i = 0; i < generated.days().size(); i++) {
            GeneratedTravelPlan.GeneratedDay day = generated.days().get(i);
            Long dayId = planDayIds.get(i);

            for (GeneratedTravelPlan.GeneratedPlace place : day.places()) {
                PlanPlace planPlace = PlanPlace.builder()
                        .dayId(dayId)
                        .title(place.title())
                        .placeName(place.placeName())
                        .address(place.address())
                        .lat(place.lat())
                        .lng(place.lng())
                        .startAt(place.startAt())
                        .endAt(place.endAt())
                        .expectedCost(BigDecimal.ZERO)
                        .normalizedCategory(place.category())
                        .firstImage(place.firstImage())
                        .firstImage2(place.firstImage2())
                        .isEnded(false)
                        .build();

                planPlaces.add(planPlace);
            }
        }

        planPlaceDao.insertPlanPlaceBatch(planPlaces);

        log.info("✅ 여행 일정 저장 완료 (planId={})", planId);
        return planId;
    }
}
