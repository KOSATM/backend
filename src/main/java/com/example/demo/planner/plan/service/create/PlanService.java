package com.example.demo.planner.plan.service.create;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.common.user.dao.UserDao;
import com.example.demo.common.user.dto.User;
import com.example.demo.planner.plan.dao.PlanDao;
import com.example.demo.planner.plan.dao.PlanDayDao;
import com.example.demo.planner.plan.dao.PlanPlaceDao;
import com.example.demo.planner.plan.dao.PlanSnapshotDao;
import com.example.demo.planner.plan.dto.entity.Plan;
import com.example.demo.planner.plan.dto.entity.PlanDay;
import com.example.demo.planner.plan.dto.entity.PlanPlace;
import com.example.demo.planner.plan.dto.response.PlanDayWithPlaces;
import com.example.demo.planner.plan.dto.response.PlanDetail;
import com.example.demo.planner.plan.dto.response.PlacePosition;
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
  private final UserDao userDao;

  /**
   * 두 일차(PlanDay)의 dayIndex를 서로 교체 (장소는 그대로)
   */
  @Transactional
  public void swapDay(Long planId, int dayA, int dayB) {
    if (dayA == dayB) return;
    PlanDay d1 = planDayDao.selectPlanDayByPlanIdAndDayIndex(planId, dayA);
    PlanDay d2 = planDayDao.selectPlanDayByPlanIdAndDayIndex(planId, dayB);
    if (d1 == null || d2 == null) throw new IllegalArgumentException("해당 일차가 존재하지 않습니다.");
    // 임시 인덱스(-1)로 충돌 방지 후 교체
    planDayDao.updateDayIndex(d1.getId(), -1);
    planDayDao.updateDayIndex(d2.getId(), dayA);
    planDayDao.updateDayIndex(d1.getId(), dayB);
  }

  /**
   * 사용자의 활성(진행 중인) 여행 계획 조회
   * isEnded=false 또는 NULL인 Plan 반환
   */
  public Plan findActiveByUserId(Long userId) {
    log.info("활성 Plan 조회: userId={}", userId);
    Plan activePlan = planDao.selectActiveTravelPlanByUserId(userId);
    log.info("활성 Plan 조회 결과: {}", activePlan);
    return activePlan;
  }

  /**
   * 특정 일차의 전체 일정 조회 (PlanDay + PlanPlace 리스트)
   */
  public PlanDayWithPlaces queryDay(Long planId, int dayIndex) {
    log.info("일차 조회: planId={}, dayIndex={}", planId, dayIndex);
    PlanDay day = planDayDao.selectPlanDayByPlanIdAndDayIndex(planId, dayIndex);
    if (day == null) throw new IllegalArgumentException(dayIndex + "일차가 존재하지 않습니다.");
    java.util.List<PlanPlace> places = planPlaceDao.selectPlanPlacesByPlanDayId(day.getId());
    return new PlanDayWithPlaces(day, places);
  }

  /**
   * 특정 일차의 특정 장소 조회 (placeIndex는 1부터 시작)
   */
  public PlanPlace queryPlace(Long planId, int dayIndex, int placeIndex) {
    log.info("장소 조회: planId={}, dayIndex={}, placeIndex={}", planId, dayIndex, placeIndex);
    PlanDay day = planDayDao.selectPlanDayByPlanIdAndDayIndex(planId, dayIndex);
    if (day == null) throw new IllegalArgumentException(dayIndex + "일차가 존재하지 않습니다.");
    java.util.List<PlanPlace> places = planPlaceDao.selectPlanPlacesByPlanDayId(day.getId());
    if (placeIndex < 1 || placeIndex > places.size()) {
      throw new IllegalArgumentException(dayIndex + "일차의 " + placeIndex + "번째 장소가 존재하지 않습니다.");
    }
    return places.get(placeIndex - 1);
  }

  // 스냅샷을 여행 계획, 여행 일자, 여행 장소로 분리
  public PlanSnapshotContent parseSnapshot(String snapshotJson) throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    PlanSnapshotContent planSnapshotContent = objectMapper.readValue(snapshotJson, PlanSnapshotContent.class);
    return planSnapshotContent;
  }

  // 여행 계획 생성 (빈 Plan만) - POST /plans
  public Plan createPlan(Plan plan) {
    log.info("여행 계획 생성: userId={}", plan.getUserId());

    // isEnded는 생성 시 입력 불가
    if (plan.getIsEnded() != null) {
      throw new IllegalArgumentException("isEnded는 생성 시 입력할 수 없습니다. 여행 완료는 /plans/{id}/complete 엔드포인트를 사용하세요.");
    }

    // startDate 검증: 오늘 이전 날짜는 불가
    if (plan.getStartDate() != null && plan.getStartDate().isBefore(LocalDate.now())) {
      throw new IllegalArgumentException("여행 시작일은 오늘 이후여야 합니다.");
    }

    // 생성 시간 설정
    Plan newPlan = Plan.builder()
        .userId(plan.getUserId())
        .budget(plan.getBudget())
        .startDate(plan.getStartDate())
        .endDate(plan.getEndDate())
        .isEnded(false)  // 생성 시 항상 false
        .title(plan.getTitle())
        .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
        .build();

    planDao.insertPlan(newPlan);
    log.info("여행 계획 생성 완료: planId={}", newPlan.getId());
    return newPlan;
  }

  // 여행 계획 생성 with 샘플 데이터 (Agent에서 호출용) - Plan + 지정된 일수만큼의 Day + 각 Day마다 2개의 샘플 Place 생성
  public Plan createPlanWithSampleData(Long userId, Integer days, BigDecimal budget, LocalDate startDate) {
    // 기본값 설정
    if (days == null) {
      days = 3;
    }
    if (budget == null) {
      budget = new BigDecimal("500000");
    }
    if (startDate == null) {
      startDate = LocalDate.now();
    }

    // startDate 검증: 오늘 이전 날짜는 불가
    if (startDate.isBefore(LocalDate.now())) {
      throw new IllegalArgumentException("여행 시작일은 오늘 이후여야 합니다.");
    }

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

  // Plan 단건 조회
  public Plan findById(Long planId) {
    log.info("Plan 조회: planId={}", planId);
    Plan plan = planDao.selectPlanById(planId);
    if (plan == null) {
      throw new IllegalArgumentException("존재하지 않는 여행 계획입니다: planId=" + planId);
    }
    return plan;
  }

  // 사용자별 Plan 목록 조회
  public java.util.List<Plan> findByUserId(Long userId) {
    log.info("사용자별 Plan 목록 조회: userId={}", userId);
    return planDao.selectPlansByUserId(userId);
  }

  // Plan 수정 (부분 수정 지원)
  public void updatePlan(Long planId, Plan plan) {
    Plan existing = planDao.selectPlanById(planId);
    if (existing == null) {
      throw new IllegalArgumentException("존재하지 않는 여행 계획입니다: planId=" + planId);
    }

    // userId는 수정 불가
    if (plan.getUserId() != null && !plan.getUserId().equals(existing.getUserId())) {
      throw new IllegalArgumentException("userId는 수정할 수 없습니다.");
    }

    // null이 아닌 필드만 업데이트 (부분 수정)
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

  // 여행 완료 처리 - POST /plans/{planId}/complete
  public Plan completePlan(Long planId) {
    Plan existing = planDao.selectPlanById(planId);
    if (existing == null) {
      throw new IllegalArgumentException("존재하지 않는 여행 계획입니다: planId=" + planId);
    }

    if (existing.getIsEnded()) {
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

  // Plan 삭제 (연관된 Day, Place도 함께 삭제)
  public void deletePlan(Long planId) {
    Plan existing = planDao.selectPlanById(planId);
    if (existing == null) {
      throw new IllegalArgumentException("존재하지 않는 여행 계획입니다: planId=" + planId);
    }

    // 연관된 Day와 Place 먼저 삭제
    java.util.List<PlanDay> days = planDayDao.selectPlanDaysByPlanId(planId);
    for (PlanDay day : days) {
      planPlaceDao.deletePlanPlaceByDayId(day.getId());
    }
    planDayDao.deletePlanDaysByPlanId(planId);

    // Plan 삭제
    planDao.deletePlan(planId);
    log.info("Plan 삭제 완료: planId={}", planId);
  }

  // PlanDay 단건 조회
  public PlanDay findDayById(Long dayId) {
    log.info("PlanDay 조회: dayId={}", dayId);
    PlanDay day = planDayDao.selectPlanDayById(dayId);
    if (day == null) {
      throw new IllegalArgumentException("존재하지 않는 여행 일자입니다: dayId=" + dayId);
    }
    return day;
  }

  // PlanDay 수정 (부분 수정 지원)
  public void updateDay(Long dayId, PlanDay day) {
    PlanDay existing = planDayDao.selectPlanDayById(dayId);
    if (existing == null) {
      throw new IllegalArgumentException("존재하지 않는 여행 일자입니다: dayId=" + dayId);
    }

    // null이 아닌 필드만 업데이트 (부분 수정)
    PlanDay updatedDay = PlanDay.builder()
        .id(dayId)
        .planId(day.getPlanId() != null ? day.getPlanId() : existing.getPlanId())
        .dayIndex(day.getDayIndex() != null ? day.getDayIndex() : existing.getDayIndex())
        .title(day.getTitle() != null ? day.getTitle() : existing.getTitle())
        .planDate(day.getPlanDate() != null ? day.getPlanDate() : existing.getPlanDate())
        .build();

    planDayDao.updatePlanDay(updatedDay);
    log.info("PlanDay 수정 완료: dayId={}", dayId);
  }

  // PlanDay 삭제 (연관된 Place도 함께 삭제)
  public void deleteDay(Long dayId) {
    PlanDay existing = planDayDao.selectPlanDayById(dayId);
    if (existing == null) {
      throw new IllegalArgumentException("존재하지 않는 여행 일자입니다: dayId=" + dayId);
    }

    // 연관된 Place 먼저 삭제
    planPlaceDao.deletePlanPlaceByDayId(dayId);

    // Day 삭제
    planDayDao.deletePlanDay(dayId);
    log.info("PlanDay 삭제 완료: dayId={}", dayId);
  }

  // PlanDay 생성
  public PlanDay createDay(PlanDay day, Boolean confirm) {
    log.info("PlanDay 생성: planId={}", day.getPlanId());

    // Plan 조회 (startDate 필요)
    Plan plan = planDao.selectPlanById(day.getPlanId());
    if (plan == null) {
      throw new IllegalArgumentException("존재하지 않는 여행 계획입니다: planId=" + day.getPlanId());
    }

    Integer dayIndex = day.getDayIndex();

    // dayIndex가 null이면 자동 계산 (max + 1)
    if (dayIndex == null) {
      Integer maxIndex = planDayDao.selectMaxDayIndexByPlanId(day.getPlanId());
      dayIndex = (maxIndex == null) ? 1 : maxIndex + 1;
      log.info("dayIndex 자동 계산: {}", dayIndex);
    } else {
      // dayIndex가 지정된 경우 중복 체크
      PlanDay existing = planDayDao.selectPlanDayByPlanIdAndDayIndex(day.getPlanId(), dayIndex);
      if (existing != null) {
        throw new IllegalArgumentException("해당 여행 계획의 " + dayIndex + "일차가 이미 존재합니다.");
      }
    }

    // planDate 자동 계산: Plan의 startDate + (dayIndex - 1)일
    LocalDate planDate = plan.getStartDate() != null
        ? plan.getStartDate().plusDays(dayIndex - 1)
        : null;

    // Plan 기간 초과인 경우: 사용자 승인(confirm)이 있어야만 확장
    if (plan.getStartDate() != null && plan.getEndDate() != null) {
      long planDuration = java.time.temporal.ChronoUnit.DAYS.between(plan.getStartDate(), plan.getEndDate()) + 1;
      if (dayIndex > planDuration) {
        // 확장이 필요하지만 승인 없으면 예외
        if (confirm == null || !confirm) {
          log.warn("PlanDay 생성 시 확장 승인 필요: planId={}, currentDuration={}, requestedDayIndex={}",
              plan.getId(), planDuration, dayIndex);
          throw new IllegalArgumentException("여행 기간 확장이 필요합니다. preview API로 확인 후 confirm=true로 호출하세요. currentDuration="
              + planDuration + ", requestedDayIndex=" + dayIndex);
        }

        // 승인된 경우에만 endDate 확장 수행
        LocalDate newEndDate = plan.getStartDate().plusDays(dayIndex - 1);
        log.info("🔄 Plan 기간 자동 확장(승인됨): planId={}, {}일 → {}일 (endDate: {} → {})",
            plan.getId(), planDuration, dayIndex, plan.getEndDate(), newEndDate);
        Plan updatedPlan = Plan.builder()
            .id(plan.getId())
            .userId(plan.getUserId())
            .budget(plan.getBudget())
            .startDate(plan.getStartDate())
            .endDate(newEndDate)
            .isEnded(plan.getIsEnded())
            .title(plan.getTitle())
            .build();
        planDao.updatePlan(updatedPlan);
        log.info("✅ Plan endDate 자동 업데이트 완료: {} → {}", plan.getEndDate(), newEndDate);
      }
    }

    // dayIndex와 planDate 설정하여 생성
    PlanDay newDay = PlanDay.builder()
        .planId(day.getPlanId())
        .dayIndex(dayIndex)
        .title(day.getTitle())
        .planDate(planDate)
        .build();

    planDayDao.insertPlanDay(newDay);
    log.info("PlanDay 생성 완료: dayId={}, dayIndex={}, planDate={}", newDay.getId(), dayIndex, planDate);
    return newDay;
  }

  // PlanDay in-place 이동 (트랜잭션)
  @Transactional
  public PlanDetail moveDay(Long dayId, Integer toIndex, Boolean confirm) {
    if (toIndex == null || toIndex < 1) {
      throw new IllegalArgumentException("toIndex는 1 이상의 정수여야 합니다.");
    }

    // 이동할 Day 조회
    PlanDay moving = planDayDao.selectPlanDayById(dayId);
    if (moving == null) {
      throw new IllegalArgumentException("존재하지 않는 여행 일자입니다: dayId=" + dayId);
    }

    Plan plan = planDao.selectPlanById(moving.getPlanId());
    if (plan == null) {
      throw new IllegalArgumentException("해당 Day의 Plan을 찾을 수 없습니다: planId=" + moving.getPlanId());
    }

    int fromIndex = moving.getDayIndex();
    if (toIndex == fromIndex) {
      // 변경 없음
      return getPlanDetail(plan.getId());
    }

    // 전체 Day 목록 조회 (정렬되어 반환된다고 가정)
    java.util.List<PlanDay> days = planDayDao.selectPlanDaysByPlanId(plan.getId());

    // 현재 최대 인덱스 계산 (null 안전 처리)
    int currentMaxIndex = days.stream()
        .map(PlanDay::getDayIndex)
        .filter(idx -> idx != null)
        .mapToInt(Integer::intValue)
        .max()
        .orElse(0);
    boolean requiresExtension = toIndex > currentMaxIndex;

    // 확장이 필요하지만 사용자 승인이 없으면 예외로 알림
    if (requiresExtension && (confirm == null || !confirm)) {
      log.warn("확장 승인 필요: planId={}, currentMaxIndex={}, requestedToIndex={}", plan.getId(), currentMaxIndex, toIndex);
      throw new IllegalArgumentException("여행 기간 확장이 필요합니다. 먼저 preview API를 호출하여 사용자 승인을 받은 뒤 confirm=true로 호출하세요. currentMaxIndex="
          + currentMaxIndex + ", requested=" + toIndex);
    }

    // 1) 임시로 이동 Day의 인덱스를 -1로 설정하여 유니크 제약 회피
    PlanDay temp = PlanDay.builder()
        .id(moving.getId())
        .planId(moving.getPlanId())
        .dayIndex(-1)
        .title(moving.getTitle())
        .planDate(moving.getPlanDate())
        .build();
    planDayDao.updatePlanDay(temp);

    // 2) 범위에 따라 다른 Day들을 shift
    if (fromIndex < toIndex) {
      // from+1 .. toIndex -> 각자 -1
      for (PlanDay d : days) {
        Integer idx = d.getDayIndex();
        if (idx != null && idx > fromIndex && idx <= toIndex) {
          PlanDay updated = PlanDay.builder()
              .id(d.getId())
              .planId(d.getPlanId())
              .dayIndex(idx - 1)
              .title(d.getTitle())
              .planDate(d.getPlanDate())
              .build();
          // planDate는 나중에 재계산
          planDayDao.updatePlanDay(updated);
        }
      }
    } else {
      // toIndex .. from-1 -> 각자 +1
      for (PlanDay d : days) {
        Integer idx = d.getDayIndex();
        if (idx != null && idx >= toIndex && idx < fromIndex) {
          PlanDay updated = PlanDay.builder()
              .id(d.getId())
              .planId(d.getPlanId())
              .dayIndex(idx + 1)
              .title(d.getTitle())
              .planDate(d.getPlanDate())
              .build();
          planDayDao.updatePlanDay(updated);
        }
      }
    }

    // 3) 이동 Day을 목표 인덱스로 설정
    PlanDay moved = PlanDay.builder()
        .id(moving.getId())
        .planId(moving.getPlanId())
        .dayIndex(toIndex)
        .title(moving.getTitle())
        .planDate(moving.getPlanDate())
        .build();
    planDayDao.updatePlanDay(moved);

    // 4) 영향을 받은 Day들의 planDate 재계산 (Plan의 startDate 기준)
    java.util.List<PlanDay> updatedDays = planDayDao.selectPlanDaysByPlanId(plan.getId());
    for (PlanDay d : updatedDays) {
      if (plan.getStartDate() != null && d.getDayIndex() != null) {
        LocalDate newDate = plan.getStartDate().plusDays(d.getDayIndex() - 1);
        PlanDay pd = PlanDay.builder()
            .id(d.getId())
            .planId(d.getPlanId())
            .dayIndex(d.getDayIndex())
            .title(d.getTitle())
            .planDate(newDate)
            .build();
        planDayDao.updatePlanDay(pd);
      }
    }

    // 5) Plan 기간 확장 적용 (confirm이 true인 경우에만 실행됨)
    if (requiresExtension && plan.getStartDate() != null) {
      LocalDate newEndDate = plan.getStartDate().plusDays(toIndex - 1);
      Plan updatedPlan = Plan.builder()
          .id(plan.getId())
          .userId(plan.getUserId())
          .budget(plan.getBudget())
          .startDate(plan.getStartDate())
          .endDate(newEndDate)
          .isEnded(plan.getIsEnded())
          .title(plan.getTitle())
          .build();
      planDao.updatePlan(updatedPlan);
      log.info("🔄 Plan 기간 확장 완료(이동): planId={}, newEndDate={}", plan.getId(), newEndDate);
    }

    return getPlanDetail(plan.getId());
  }

  // PlanPlace 단건 조회
  public PlanPlace findPlaceById(Long placeId) {
    log.info("PlanPlace 조회: placeId={}", placeId);
    PlanPlace place = planPlaceDao.selectPlanPlaceById(placeId);
    if (place == null) {
      throw new IllegalArgumentException("존재하지 않는 여행 장소입니다: placeId=" + placeId);
    }
    return place;
  }

  // PlanPlace 수정 (부분 수정 지원)
  public void updatePlace(Long placeId, PlanPlace place) {
    PlanPlace existing = planPlaceDao.selectPlanPlaceById(placeId);
    if (existing == null) {
      throw new IllegalArgumentException("존재하지 않는 여행 장소입니다: placeId=" + placeId);
    }

    // null이 아닌 필드만 업데이트 (부분 수정)
    // lat, lng는 primitive double이라 0.0이 아니면 업데이트
    PlanPlace updatedPlace = PlanPlace.builder()
        .id(placeId)
        .dayId(place.getDayId() != null ? place.getDayId() : existing.getDayId())
        .title(place.getTitle() != null ? place.getTitle() : existing.getTitle())
        .startAt(place.getStartAt() != null ? place.getStartAt() : existing.getStartAt())
        .endAt(place.getEndAt() != null ? place.getEndAt() : existing.getEndAt())
        .placeName(place.getPlaceName() != null ? place.getPlaceName() : existing.getPlaceName())
        .address(place.getAddress() != null ? place.getAddress() : existing.getAddress())
        .lat(place.getLat() != 0.0 ? place.getLat() : existing.getLat())
        .lng(place.getLng() != 0.0 ? place.getLng() : existing.getLng())
        .expectedCost(place.getExpectedCost() != null ? place.getExpectedCost() : existing.getExpectedCost())
        .build();

    planPlaceDao.updatePlanPlace(updatedPlace);
    log.info("PlanPlace 수정 완료: placeId={}", placeId);
  }

  // PlanPlace 삭제
  public void deletePlace(Long placeId) {
    PlanPlace existing = planPlaceDao.selectPlanPlaceById(placeId);
    if (existing == null) {
      throw new IllegalArgumentException("존재하지 않는 여행 장소입니다: placeId=" + placeId);
    }
    planPlaceDao.deletePlanPlace(placeId);
    log.info("PlanPlace 삭제 완료: placeId={}", placeId);
  }

  // PlanPlace 생성
  public PlanPlace createPlace(PlanPlace place) {
    log.info("PlanPlace 생성: dayId={}", place.getDayId());
    planPlaceDao.insertPlanPlace(place);
    log.info("PlanPlace 생성 완료: placeId={}", place.getId());
    return place;
  }

  // Plan 상세 조회 (Days + Places 포함) - Plan -> List<PlanDayWithPlaces> 중첩 구조
  public PlanDetail getPlanDetail(Long planId) {
    log.info("Plan 상세 조회 시작: planId={}", planId);

    // 1. Plan 조회
    Plan plan = planDao.selectPlanById(planId);
    if (plan == null) {
      log.warn("Plan을 찾을 수 없음: planId={}", planId);
      throw new IllegalArgumentException("존재하지 않는 Plan입니다: planId=" + planId);
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

  // 사용자의 활성화된 Plan 상세 조회 (Days + Places 포함) - Plan -> List<PlanDayWithPlaces> 중첩 구조
  public PlanDetail getLatestPlanDetail(Long userId) {
    log.info("사용자의 활성화된 Plan 상세 조회 시작: userId={}", userId);

    // 1. 사용자 조회
    User user = userDao.selectUserById(userId);
    if (user == null) {
      log.warn("존재하지 않는 사용자: userId={}", userId);
      throw new IllegalArgumentException("존재하지 않는 사용자입니다: userId=" + userId);
    }

    // 2. Plan 조회
    Plan plan = planDao.selectActiveTravelPlanByUserId(userId);
    if (plan == null) {
      log.warn("사용자의 활성화된 Plan을 찾을 수 없음: userId={}", userId);
      throw new IllegalArgumentException("존재하지 않는 Plan입니다: userId=" + userId);
    }
    long planId = plan.getId();

    // 3. Plan의 모든 Day 조회
    java.util.List<PlanDay> days = planDayDao.selectPlanDaysByPlanId(planId);

    // 4. 각 Day의 Places를 조회하여 PlanDayWithPlaces 생성
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

  // 이동 미리보기: 확장 필요 여부 및 예상 endDate 계산
  public com.example.demo.planner.plan.dto.response.MovePreview movePreview(Long dayId, Integer toIndex) {
    if (toIndex == null || toIndex < 1) {
      throw new IllegalArgumentException("toIndex는 1 이상의 정수여야 합니다.");
    }

    PlanDay moving = planDayDao.selectPlanDayById(dayId);
    if (moving == null) {
      throw new IllegalArgumentException("존재하지 않는 여행 일자입니다: dayId=" + dayId);
    }

    Plan plan = planDao.selectPlanById(moving.getPlanId());
    if (plan == null) {
      throw new IllegalArgumentException("해당 Day의 Plan을 찾을 수 없습니다: planId=" + moving.getPlanId());
    }

    java.util.List<PlanDay> days = planDayDao.selectPlanDaysByPlanId(plan.getId());
    int currentMaxIndex = days.stream()
        .map(PlanDay::getDayIndex)
        .filter(idx -> idx != null)
        .mapToInt(Integer::intValue)
        .max()
        .orElse(0);
    boolean requiresExtension = toIndex > currentMaxIndex;
    java.time.LocalDate newEndDate = null;
    if (requiresExtension && plan.getStartDate() != null) {
      newEndDate = plan.getStartDate().plusDays(toIndex - 1);
    }

    return new com.example.demo.planner.plan.dto.response.MovePreview(requiresExtension, newEndDate, currentMaxIndex, toIndex);
  }

  // PlanDay 생성 미리보기: planId와 dayIndex로 확장 필요 여부 및 예상 endDate 계산
  public com.example.demo.planner.plan.dto.response.MovePreview createDayPreview(Long planId, Integer dayIndex) {
    if (dayIndex == null || dayIndex < 1) {
      throw new IllegalArgumentException("dayIndex는 1 이상의 정수여야 합니다.");
    }

    Plan plan = planDao.selectPlanById(planId);
    if (plan == null) {
      throw new IllegalArgumentException("존재하지 않는 여행 계획입니다: planId=" + planId);
    }

    if (plan.getStartDate() == null || plan.getEndDate() == null) {
      // startDate가 없는 경우 확장 여부를 판단할 수 없으므로 requiresExtension=true
      java.time.LocalDate newEndDate = plan.getStartDate() != null ? plan.getStartDate().plusDays(dayIndex - 1) : null;
      return new com.example.demo.planner.plan.dto.response.MovePreview(true, newEndDate, 0, dayIndex);
    }

    long planDuration = java.time.temporal.ChronoUnit.DAYS.between(plan.getStartDate(), plan.getEndDate()) + 1;
    Integer maxIndexResult = planDayDao.selectMaxDayIndexByPlanId(planId);
    int currentMaxIndex = (maxIndexResult != null) ? maxIndexResult : 0;
    boolean requiresExtension = dayIndex > planDuration || dayIndex > currentMaxIndex;
    java.time.LocalDate newEndDate = null;
    if (requiresExtension) {
      newEndDate = plan.getStartDate().plusDays(dayIndex - 1);
    }

    return new com.example.demo.planner.plan.dto.response.MovePreview(requiresExtension, newEndDate, currentMaxIndex, dayIndex);
  }

  // 사용자별 Plan 상세 목록 조회 (모든 Plan + Days + Places)
  public java.util.List<PlanDetail> getPlanDetailsByUserId(Long userId) {
    log.info("사용자별 Plan 상세 목록 조회 시작: userId={}", userId);

    // 1. 사용자의 모든 Plan 조회
    java.util.List<Plan> plans = planDao.selectPlansByUserId(userId);

    // 2. 각 Plan의 상세 정보 조회
    java.util.List<PlanDetail> planDetails = plans.stream()
        .map(plan -> getPlanDetail(plan.getId()))
        .collect(java.util.stream.Collectors.toList());

    log.info("사용자별 Plan 상세 목록 조회 완료: userId={}, 총 {}개 Plan", userId, planDetails.size());
    return planDetails;
  }

  // ========== 추가 조회 메서드 (VIEW Intent 지원) ==========

  /**
   * 전체 일정 조회 (모든 Day + Place)
   */
  public java.util.List<PlanDayWithPlaces> queryAllDays(Long planId) {
    log.info("전체 일정 조회: planId={}", planId);
    java.util.List<PlanDay> days = planDayDao.selectPlanDaysByPlanId(planId);
    return days.stream()
        .map(day -> {
          java.util.List<PlanPlace> places = planPlaceDao.selectPlanPlacesByPlanDayId(day.getId());
          return new PlanDayWithPlaces(day, places);
        })
        .collect(java.util.stream.Collectors.toList());
  }

  /**
   * 날짜로 일정 조회
   */
  public PlanDayWithPlaces queryDayByDate(Long planId, String dateStr) {
    log.info("날짜로 일정 조회: planId={}, date={}", planId, dateStr);
    LocalDate date = LocalDate.parse(dateStr);
    Plan plan = planDao.selectPlanById(planId);
    if (plan == null) {
      throw new IllegalArgumentException("Plan not found: " + planId);
    }
    
    // 시작일로부터 몇 일째인지 계산
    int dayIndex = (int) java.time.temporal.ChronoUnit.DAYS.between(plan.getStartDate(), date) + 1;
    return queryDay(planId, dayIndex);
  }

  /**
   * 장소명으로 검색 (부분 일치)
   */
  public java.util.List<PlanPlace> queryPlacesByName(Long planId, String placeName) {
    log.info("장소명 검색: planId={}, placeName={}", planId, placeName);
    java.util.List<PlanDay> days = planDayDao.selectPlanDaysByPlanId(planId);
    return days.stream()
        .flatMap(day -> planPlaceDao.selectPlanPlacesByPlanDayId(day.getId()).stream())
        .filter(place -> place.getPlaceName().toLowerCase().contains(placeName.toLowerCase()) ||
                        place.getTitle().toLowerCase().contains(placeName.toLowerCase()))
        .collect(java.util.stream.Collectors.toList());
  }

  /**
   * 현재 시간 기준 일정 조회
   */
  public PlanPlace queryCurrentActivity(Long planId) {
    log.info("현재 일정 조회: planId={}", planId);
    OffsetDateTime now = OffsetDateTime.now();
    java.util.List<PlanDay> days = planDayDao.selectPlanDaysByPlanId(planId);
    
    return days.stream()
        .flatMap(day -> planPlaceDao.selectPlanPlacesByPlanDayId(day.getId()).stream())
        .filter(place -> place.getStartAt() != null && place.getEndAt() != null)
        .filter(place -> !now.isBefore(place.getStartAt()) && !now.isAfter(place.getEndAt()))
        .findFirst()
        .orElse(null);
  }

  /**
   * 다음 일정 조회
   */
  public PlanPlace queryNextActivity(Long planId) {
    log.info("다음 일정 조회: planId={}", planId);
    OffsetDateTime now = OffsetDateTime.now();
    java.util.List<PlanDay> days = planDayDao.selectPlanDaysByPlanId(planId);
    
    return days.stream()
        .flatMap(day -> planPlaceDao.selectPlanPlacesByPlanDayId(day.getId()).stream())
        .filter(place -> place.getStartAt() != null)
        .filter(place -> place.getStartAt().isAfter(now))
        .sorted((p1, p2) -> p1.getStartAt().compareTo(p2.getStartAt()))
        .findFirst()
        .orElse(null);
  }

  /**
   * 특정 장소가 몇일차에 있는지 조회 (Fuzzy matching 지원)
   */
  public PlanDayWithPlaces findPlaceDay(Long planId, String placeName) {
    log.info("장소→날짜 조회 (fuzzy): planId={}, placeName={}", planId, placeName);
    java.util.List<PlanDay> days = planDayDao.selectPlanDaysByPlanId(planId);
    
    // 1. 모든 장소명 수집
    java.util.List<String> allPlaceNames = new java.util.ArrayList<>();
    java.util.Map<String, PlanDay> placeToDay = new java.util.HashMap<>();
    
    for (PlanDay day : days) {
      java.util.List<PlanPlace> places = planPlaceDao.selectPlanPlacesByPlanDayId(day.getId());
      for (PlanPlace place : places) {
        allPlaceNames.add(place.getPlaceName());
        allPlaceNames.add(place.getTitle());
        placeToDay.put(place.getPlaceName(), day);
        placeToDay.put(place.getTitle(), day);
      }
    }
    
    // 2. Fuzzy matching으로 가장 가까운 장소명 찾기
    String bestMatch = findClosestPlaceName(placeName, allPlaceNames);
    
    if (bestMatch == null) {
      return null;
    }
    
    log.info("Fuzzy match result: '{}' → '{}'", placeName, bestMatch);
    
    // 3. 매칭된 장소가 속한 Day 반환
    PlanDay matchedDay = placeToDay.get(bestMatch);
    if (matchedDay != null) {
      java.util.List<PlanPlace> places = planPlaceDao.selectPlanPlacesByPlanDayId(matchedDay.getId());
      return new PlanDayWithPlaces(matchedDay, places);
    }
    
    return null;
  }

  /**
   * 장소의 위치 정보 조회 (dayIndex, order, date 포함)
   * Fuzzy matching 기반
   */
  public PlacePosition findPlacePosition(String placeName, Long userId) {
    log.info("장소 위치 조회: placeName={}, userId={}", placeName, userId);
    
    // 1. 활성 Plan 조회
    Plan activePlan = findActiveByUserId(userId);
    if (activePlan == null) {
      log.info("활성 여행 계획이 없습니다: userId={}", userId);
      return null;
    }
    
    // 2. 모든 PlanDay 조회
    java.util.List<PlanDay> allDays = planDayDao.selectPlanDaysByPlanId(activePlan.getId());
    if (allDays.isEmpty()) {
      return null;
    }
    
    // 3. 모든 PlanPlace 조회하여 fuzzy matching
    java.util.Map<String, PlacePosition> placePositions = new java.util.HashMap<>();
    
    for (PlanDay day : allDays) {
      java.util.List<PlanPlace> places = planPlaceDao.selectPlanPlacesByPlanDayId(day.getId());
      for (int i = 0; i < places.size(); i++) {
        PlanPlace place = places.get(i);
        placePositions.put(place.getPlaceName(), PlacePosition.builder()
            .dayIndex(day.getDayIndex())
            .order(i + 1)
            .date(day.getPlanDate())
            .placeName(place.getPlaceName())
            .dayId(day.getId())
            .build());
      }
    }
    
    // 4. Fuzzy matching으로 가장 가까운 장소명 찾기
    java.util.List<String> allPlaceNames = new java.util.ArrayList<>(placePositions.keySet());
    String bestMatch = findClosestPlaceName(placeName, allPlaceNames);
    
    if (bestMatch == null) {
      log.info("장소를 찾을 수 없습니다: '{}'", placeName);
      return null;
    }
    
    log.info("Fuzzy match result: '{}' → '{}'", placeName, bestMatch);
    return placePositions.get(bestMatch);
  }

  /**
   * 특정 Day의 모든 장소 조회 (order 순서대로)
   */
  public java.util.List<PlanPlace> getDayPlaces(Long dayId) {
    log.info("Day 장소 목록 조회: dayId={}", dayId);
    return planPlaceDao.selectPlanPlacesByPlanDayId(dayId);
  }

  /**
   * 시간대별 일정 조회 (아침/점심/저녁)
   * @param userId 사용자 ID
   * @param timeRange "morning", "lunch", "evening"
   * @return 해당 시간대의 모든 장소 목록
   */
  public java.util.List<PlanPlace> getPlansByTimeRange(Long userId, String timeRange) {
    log.info("시간대별 일정 조회: userId={}, timeRange={}", userId, timeRange);
    
    // 활성 Plan 조회
    Plan activePlan = findActiveByUserId(userId);
    if (activePlan == null) {
      log.info("활성 여행 계획이 없습니다: userId={}", userId);
      return java.util.Collections.emptyList();
    }
    
    // 시간대 범위 정의
    java.time.LocalTime startTime, endTime;
    switch (timeRange.toLowerCase()) {
      case "morning":
        startTime = java.time.LocalTime.of(5, 0);
        endTime = java.time.LocalTime.of(11, 0);
        break;
      case "lunch":
        startTime = java.time.LocalTime.of(11, 0);
        endTime = java.time.LocalTime.of(15, 0);
        break;
      case "evening":
        startTime = java.time.LocalTime.of(17, 0);
        endTime = java.time.LocalTime.of(23, 59);
        break;
      default:
        log.warn("알 수 없는 시간대: {}", timeRange);
        return java.util.Collections.emptyList();
    }
    
    // 모든 Day 조회
    java.util.List<PlanDay> allDays = planDayDao.selectPlanDaysByPlanId(activePlan.getId());
    java.util.List<PlanPlace> filteredPlaces = new java.util.ArrayList<>();
    
    // 각 Day의 장소를 시간대로 필터링
    for (PlanDay day : allDays) {
      java.util.List<PlanPlace> dayPlaces = planPlaceDao.selectPlanPlacesByPlanDayId(day.getId());
      for (PlanPlace place : dayPlaces) {
        if (place.getStartAt() != null) {
          java.time.LocalTime placeTime = place.getStartAt().toLocalTime();
          if (!placeTime.isBefore(startTime) && placeTime.isBefore(endTime)) {
            filteredPlaces.add(place);
          }
        }
      }
    }
    
    log.info("시간대 '{}' 조회 결과: {}개 장소", timeRange, filteredPlaces.size());
    return filteredPlaces;
  }

  /**
   * Fuzzy matching: 가장 유사한 장소명 찾기 (개선된 버전)
   * 한글/영어 혼용, 띄어쓰기 무시, 유사도 계산
   */
  private String findClosestPlaceName(String userInput, java.util.List<String> placeNames) {
    if (userInput == null || userInput.isEmpty() || placeNames.isEmpty()) {
      return null;
    }
    
    // 정규화: 소문자 + 공백 제거 + 특수문자 제거
    String normalizedInput = normalizeForMatching(userInput);
    
    String bestMatch = null;
    int bestScore = Integer.MAX_VALUE;
    double bestSimilarity = 0.0;
    
    for (String placeName : placeNames) {
      String normalizedPlace = normalizeForMatching(placeName);
      
      // 1. 완전 일치 체크 (최우선)
      if (normalizedPlace.equals(normalizedInput)) {
        return placeName;
      }
      
      // 2. 부분 일치 체크 (높은 우선순위)
      if (normalizedPlace.contains(normalizedInput)) {
        int score = normalizedPlace.length() - normalizedInput.length();
        if (score < bestScore) {
          bestScore = score;
          bestMatch = placeName;
          bestSimilarity = 1.0;
        }
        continue;
      }
      
      if (normalizedInput.contains(normalizedPlace)) {
        int score = normalizedInput.length() - normalizedPlace.length();
        if (score < bestScore) {
          bestScore = score;
          bestMatch = placeName;
          bestSimilarity = 0.9;
        }
        continue;
      }
      
      // 3. Levenshtein distance 계산
      int distance = levenshteinDistance(normalizedInput, normalizedPlace);
      double similarity = 1.0 - ((double) distance / Math.max(normalizedInput.length(), normalizedPlace.length()));
      
      // 유사도가 60% 이상이고, 이전 best보다 좋으면 업데이트
      if (similarity >= 0.6 && (bestMatch == null || similarity > bestSimilarity || 
          (similarity == bestSimilarity && distance < bestScore))) {
        bestScore = distance;
        bestMatch = placeName;
        bestSimilarity = similarity;
      }
    }
    
    // 최소 유사도 40% 이상만 반환
    if (bestSimilarity < 0.4) {
      log.info("No match found for '{}' (best similarity: {})", userInput, bestSimilarity);
      return null;
    }
    
    log.info("Fuzzy match: '{}' → '{}' (similarity: {}, distance: {})", 
        userInput, bestMatch, String.format("%.2f", bestSimilarity), bestScore);
    return bestMatch;
  }
  
  /**
   * 매칭을 위한 문자열 정규화
   * - 소문자 변환
   * - 공백 제거
   * - 특수문자 제거
   */
  private String normalizeForMatching(String input) {
    return input.toLowerCase()
        .replace(" ", "")
        .replace("-", "")
        .replace("_", "")
        .replace("(", "")
        .replace(")", "")
        .replace("[", "")
        .replace("]", "");
  }

  /**
   * Levenshtein Distance 계산 (편집 거리)
   */
  private int levenshteinDistance(String a, String b) {
    int[][] dp = new int[a.length() + 1][b.length() + 1];
    
    for (int i = 0; i <= a.length(); i++) {
      dp[i][0] = i;
    }
    for (int j = 0; j <= b.length(); j++) {
      dp[0][j] = j;
    }
    
    for (int i = 1; i <= a.length(); i++) {
      for (int j = 1; j <= b.length(); j++) {
        int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
        dp[i][j] = Math.min(
            Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
            dp[i - 1][j - 1] + cost
        );
      }
    }
    
    return dp[a.length()][b.length()];
  }

}
