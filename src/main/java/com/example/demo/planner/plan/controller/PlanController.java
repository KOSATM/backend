package com.example.demo.planner.plan.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.planner.plan.dto.entity.Plan;
import com.example.demo.planner.plan.dto.entity.PlanDay;
import com.example.demo.planner.plan.dto.entity.PlanPlace;
import com.example.demo.planner.plan.dto.response.PlanDetail;
import com.example.demo.planner.plan.service.PlanService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
@Slf4j
public class PlanController {
  private final PlanService planService;

  @GetMapping("/snapshot/search")
  public String snapshotSearch(@RequestBody String snapshot) {
    try {
      planService.parseSnapshot(snapshot);
      return "콘솔에 로그 있어요";
    } catch (Exception e) {
      return "에러";
    }
  }

  // 여행 계획 생성 (샘플 데이터 포함) - POST /api/plans?days=3
  @PostMapping
  public ResponseEntity<Plan> createPlan(
      @RequestParam Long userId,
      @RequestParam(defaultValue = "3") Integer days,
      @RequestParam(required = false) java.math.BigDecimal budget,
      @RequestParam(required = false) java.time.LocalDate startDate) {
    try {
      log.info("여행 계획 생성 요청: userId={}, days={}", userId, days);

      // 기본값 설정
      if (budget == null) {
        budget = new java.math.BigDecimal("500000");
      }
      if (startDate == null) {
        startDate = java.time.LocalDate.now();
      }

      Plan createdPlan = planService.createPlanWithSampleData(userId, days, budget, startDate);
      log.info("여행 계획 생성 완료: planId={}", createdPlan.getId());
      return ResponseEntity.status(HttpStatus.CREATED).body(createdPlan);
    } catch (Exception e) {
      log.error("여행 계획 생성 실패", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  // 여행 계획 단건 조회 - GET /api/plans/{planId}
  @GetMapping("/{planId}")
  public ResponseEntity<Plan> getPlan(@PathVariable Long planId) {
    Plan plan = planService.findById(planId);
    return ResponseEntity.ok(plan);
  }

  // 여행 계획 상세 조회 (Days + Places 포함) - GET /api/plans/{planId}/detail
  @GetMapping("/{planId}/detail")
  public ResponseEntity<PlanDetail> getPlanDetail(@PathVariable Long planId) {
    PlanDetail planDetail = planService.getPlanDetail(planId);
    return ResponseEntity.ok(planDetail);
  }

  // 사용자별 여행 계획 목록 조회 - GET /api/plans/user/{userId}
  @GetMapping("/user/{userId}")
  public ResponseEntity<List<Plan>> getPlansByUserId(@PathVariable Long userId) {
    List<Plan> plans = planService.findByUserId(userId);
    return ResponseEntity.ok(plans);
  }

  // 여행 계획 수정 - PUT /api/plans/{planId}
  @PutMapping("/{planId}")
  public ResponseEntity<Plan> updatePlan(@PathVariable Long planId, @RequestBody Plan plan) {
    planService.updatePlan(planId, plan);
    Plan updated = planService.findById(planId);
    return ResponseEntity.ok(updated);
  }

  // 여행 계획 삭제 - DELETE /api/plans/{planId}
  @DeleteMapping("/{planId}")
  public ResponseEntity<String> deletePlan(@PathVariable Long planId) {
    planService.deletePlan(planId);
    return ResponseEntity.ok("여행 계획이 삭제되었습니다.");
  }

  // ==================== PlanDay CRUD ====================

  // 여행 일자 생성 - POST /api/plans/days
  @PostMapping("/days")
  public ResponseEntity<PlanDay> createDay(@RequestBody PlanDay day) {
    PlanDay created = planService.createDay(day);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  // 여행 일자 조회 - GET /api/plans/days/{dayId}
  @GetMapping("/days/{dayId}")
  public ResponseEntity<PlanDay> getDay(@PathVariable Long dayId) {
    PlanDay day = planService.findDayById(dayId);
    return ResponseEntity.ok(day);
  }

  // 여행 일자 수정 - PUT /api/plans/days/{dayId}
  @PutMapping("/days/{dayId}")
  public ResponseEntity<PlanDay> updateDay(@PathVariable Long dayId, @RequestBody PlanDay day) {
    planService.updateDay(dayId, day);
    PlanDay updated = planService.findDayById(dayId);
    return ResponseEntity.ok(updated);
  }

  // 여행 일자 삭제 - DELETE /api/plans/days/{dayId}
  @DeleteMapping("/days/{dayId}")
  public ResponseEntity<String> deleteDay(@PathVariable Long dayId) {
    planService.deleteDay(dayId);
    return ResponseEntity.ok("여행 일자가 삭제되었습니다.");
  }

  // ==================== PlanPlace CRUD ====================

  // 여행 장소 생성 - POST /api/plans/places
  @PostMapping("/places")
  public ResponseEntity<PlanPlace> createPlace(@RequestBody PlanPlace place) {
    PlanPlace created = planService.createPlace(place);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  // 여행 장소 조회 - GET /api/plans/places/{placeId}
  @GetMapping("/places/{placeId}")
  public ResponseEntity<PlanPlace> getPlace(@PathVariable Long placeId) {
    PlanPlace place = planService.findPlaceById(placeId);
    return ResponseEntity.ok(place);
  }

  // 여행 장소 수정 - PUT /api/plans/places/{placeId}
  @PutMapping("/places/{placeId}")
  public ResponseEntity<PlanPlace> updatePlace(@PathVariable Long placeId, @RequestBody PlanPlace place) {
    planService.updatePlace(placeId, place);
    PlanPlace updated = planService.findPlaceById(placeId);
    return ResponseEntity.ok(updated);
  }

  // 여행 장소 삭제 - DELETE /api/plans/places/{placeId}
  @DeleteMapping("/places/{placeId}")
  public ResponseEntity<String> deletePlace(@PathVariable Long placeId) {
    planService.deletePlace(placeId);
    return ResponseEntity.ok("여행 장소가 삭제되었습니다.");
  }

}
