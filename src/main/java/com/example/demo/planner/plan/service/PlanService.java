package com.example.demo.planner.plan.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.stereotype.Service;

import com.example.demo.planner.plan.dao.PlanDao;
import com.example.demo.planner.plan.dao.PlanDayDao;
import com.example.demo.planner.plan.dao.PlanPlaceDao;
import com.example.demo.planner.plan.dao.PlanSnapshotDao;
import com.example.demo.planner.plan.dto.entity.Plan;
import com.example.demo.planner.plan.dto.entity.PlanDay;
import com.example.demo.planner.plan.dto.entity.PlanPlace;
import com.example.demo.planner.plan.dto.response.PlanDayWithPlaces;
import com.example.demo.planner.plan.dto.response.PlanDetail;
import com.example.demo.planner.plan.dto.response.PlanSnapshotContent;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class PlanService {
  private final PlanDao planDao;
  private final PlanDayDao planDayDao;
  private final PlanPlaceDao planPlaceDao;
  private final PlanSnapshotDao planSnapshotDao;

  // 스냅샷을 여행 계획, 여행 일자, 여행 장소로 분리
  public PlanSnapshotContent parseSnapshot(String snapshotJson) throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    PlanSnapshotContent planSnapshotContent = objectMapper.readValue(snapshotJson, PlanSnapshotContent.class);
    return planSnapshotContent;
  }

  /**
   * 여행 계획 생성 with 샘플 데이터 (Agent에서 호출용)
   * Plan + 지정된 일수만큼의 Day + 각 Day마다 2개의 샘플 Place 생성
   */
  public Plan createPlanWithSampleData(Long userId, Integer days, BigDecimal budget, LocalDate startDate) {
    log.info("샘플 데이터 포함 여행 계획 생성 시작: userId={}, days={}", userId, days);

    // 1. Plan 생성
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

    // 2. 요청된 일수만큼 Day와 Place 생성
    for (int i = 1; i <= days; i++) {
      LocalDate currentDate = startDate.plusDays(i - 1);

      // PlanDay 생성
      PlanDay day = PlanDay.builder()
          .planId(planId)
          .dayIndex(i)
          .title("Day " + i)
          .planDate(currentDate)
          .build();

      planDayDao.insertPlanDay(day);
      Long dayId = day.getId();
      log.debug("PlanDay 생성 완료: dayId={}, dayIndex={}", dayId, i);

      // 각 Day마다 샘플 Place 2개 생성
      // 오전 장소
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

      // 오후 장소
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

      log.debug("PlanPlace 2개 생성 완료: dayId={}", dayId);
    }

    log.info("샘플 데이터 포함 여행 계획 생성 완료: planId={}, 총 {}일, {}개 장소", planId, days, days * 2);
    return plan;
  }

  /**
   * Plan 단건 조회
   */
  public Plan findById(Long planId) {
    return planDao.selectPlanById(planId);
  }

  /**
   * 사용자별 Plan 목록 조회
   */
  public java.util.List<Plan> findByUserId(Long userId) {
    return planDao.selectPlansByUserId(userId);
  }

  /**
   * Plan 상세 조회 (Days + Places 포함)
   * Plan -> List<PlanDayWithPlaces> 중첩 구조
   */
  public PlanDetail getPlanDetail(Long planId) {
    log.info("Plan 상세 조회 시작: planId={}", planId);

    // 1. Plan 조회
    Plan plan = planDao.selectPlanById(planId);
    if (plan == null) {
      log.warn("Plan을 찾을 수 없음: planId={}", planId);
      return null;
    }

    // 2. Plan의 모든 Day 조회
    java.util.List<PlanDay> days = planDayDao.selectPlanDaysByPlanId(planId);

    // 3. 각 Day의 Places를 조회하여 PlanDayWithPlaces 생성
    java.util.List<PlanDayWithPlaces> daysWithPlaces = days.stream()
        .map(day -> {
          java.util.List<PlanPlace> places = planPlaceDao.selectPlanPlacesByPlanDayId(day.getId());
          return new PlanDayWithPlaces(day, places);
        })
        .collect(java.util.stream.Collectors.toList());

    log.info("Plan 상세 조회 완료: planId={}, days={}, 총 places={}",
        planId, daysWithPlaces.size(),
        daysWithPlaces.stream().mapToInt(d -> d.getPlaces().size()).sum());

    return new PlanDetail(plan, daysWithPlaces);
  }

}
