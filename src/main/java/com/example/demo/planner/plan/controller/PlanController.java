package com.example.demo.planner.plan.controller;

import java.util.List;
import java.util.Map;

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
import com.example.demo.planner.plan.dto.response.ActivePlanInfoResponse;
import com.example.demo.planner.plan.dto.response.PlanDetail;
import com.example.demo.planner.plan.service.create.PlanService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@RestController
@RequestMapping("/plans")
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

  
  // 플랜 id, 해당 날짜 dayindex 조회
  @GetMapping("/{planId}/active/plan/info")
  public ResponseEntity<ActivePlanInfoResponse> getActivePlanIdAndDayIndex(@PathVariable("planId") Long planId) {
    return ResponseEntity.ok(planService.getActivePlanIdAndDayIndex(planId));
  }

  // 여행 계획 생성 (빈 Plan만) - POST /plans
  @PostMapping
  public ResponseEntity<Plan> createPlan(@RequestBody Plan plan) {
    log.info("=== Plan 생성 요청 받음 ===");
    log.info("Request Body: userId={}, budget={}, startDate={}, endDate={}, title={}",
        plan.getUserId(), plan.getBudget(), plan.getStartDate(), plan.getEndDate(), plan.getTitle());

    Plan created = planService.createPlan(plan);

    log.info("=== Plan 생성 완료: planId={} ===", created.getId());
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  // 여행 계획 생성 (샘플 데이터 포함) - POST /plans/with-sample?days=3
  @PostMapping("/with-sample")
  public ResponseEntity<Plan> createPlanWithSample(
      @RequestParam(value = "userId", required = false) Long userId,
      @RequestParam(value = "days", required = false) Integer days,
      @RequestParam(value = "budget", required = false) java.math.BigDecimal budget,
      @RequestParam(value = "startDate", required = false) java.time.LocalDate startDate) {
    Plan created = planService.createPlanWithSampleData(userId, days, budget, startDate);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  // 여행 계획 단건 조회 - GET /plans/{planId}
  @GetMapping("/{planId}")
  public ResponseEntity<Plan> getPlan(@PathVariable("planId") Long planId) {
    Plan plan = planService.findById(planId);
    return ResponseEntity.ok(plan);
  }

  // 여행 계획 상세 조회 (Days + Places 포함) - GET /plans/{planId}/detail
  @GetMapping("/{planId}/detail")
  public ResponseEntity<PlanDetail> getPlanDetail(@PathVariable("planId") Long planId) {
    PlanDetail planDetail = planService.getPlanDetail(planId);
    return ResponseEntity.ok(planDetail);
  }

  // 사용자의 활성화된 여행 계획 상세 조회 (Days + Places 포함) - GET /plans/{userId}/active/detail
  @GetMapping("/{userId}/active/detail")
  public ResponseEntity<PlanDetail> getActivePlanDetail(@PathVariable("userId") Long userId) {
    PlanDetail planDetail = planService.getLatestPlanDetail(userId);
    return ResponseEntity.ok(planDetail);
  }

  // 사용자별 여행 계획 목록 조회 - GET /plans/user/{userId}
  @GetMapping("/user/{userId}")
  public ResponseEntity<List<Plan>> getPlansByUserId(@PathVariable("userId") Long userId) {
    List<Plan> plans = planService.findByUserId(userId);
    return ResponseEntity.ok(plans);
  }

  // 사용자별 여행 계획 상세 목록 조회 (모든 Plan + Days + Places) - GET /plans/user/{userId}/details
  @GetMapping("/user/{userId}/details")
  public ResponseEntity<List<PlanDetail>> getPlanDetailsByUserId(@PathVariable("userId") Long userId) {
    List<PlanDetail> planDetails = planService.getPlanDetailsByUserId(userId);
    return ResponseEntity.ok(planDetails);
  }

  // 여행 계획 수정 - PUT /plans/{planId}
  @PutMapping("/{planId}")
  public ResponseEntity<Plan> updatePlan(@PathVariable("planId") Long planId, @RequestBody Plan plan) {
    planService.updatePlan(planId, plan);
    Plan updated = planService.findById(planId);
    return ResponseEntity.ok(updated);
  }

  // 여행 계획 삭제 - DELETE /plans/{planId}
  @DeleteMapping("/{planId}")
  public ResponseEntity<String> deletePlan(@PathVariable("planId") Long planId) {
    planService.deletePlan(planId);
    return ResponseEntity.ok("여행 계획이 삭제되었습니다.");
  }

  // 여행 완료 처리 - POST /plans/{planId}/complete
  @PostMapping("/{planId}/complete")
  public ResponseEntity<Plan> completePlan(@PathVariable("planId") Long planId) {
    Plan completed = planService.completePlan(planId);
    return ResponseEntity.ok(completed);
  }

  // ==================== PlanDay CRUD ====================

  // 여행 일자 생성 - POST /plans/days
  // Optional query param: confirm=true to allow extending plan endDate when creating a day beyond current duration
  @PostMapping("/days")
  public ResponseEntity<PlanDay> createDay(@RequestBody PlanDay day,
      @RequestParam(value = "confirm", required = false) Boolean confirm) {
    PlanDay created = planService.createDay(day, confirm);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  // 여행 일자 조회 - GET /plans/days/{dayId}
  @GetMapping("/days/{dayId}")
  public ResponseEntity<PlanDay> getDay(@PathVariable("dayId") Long dayId) {
    PlanDay day = planService.findDayById(dayId);
    return ResponseEntity.ok(day);
  }

  // 여행 일자 수정 - PUT /plans/days/{dayId}
  @PutMapping("/days/{dayId}")
  public ResponseEntity<PlanDay> updateDay(@PathVariable("dayId") Long dayId, @RequestBody PlanDay day) {
    planService.updateDay(dayId, day);
    PlanDay updated = planService.findDayById(dayId);
    return ResponseEntity.ok(updated);
  }

  // 여행 일자 삭제 - DELETE /plans/days/{dayId}
  @DeleteMapping("/days/{dayId}")
  public ResponseEntity<String> deleteDay(@PathVariable("dayId") Long dayId) {
    planService.deleteDay(dayId);
    return ResponseEntity.ok("여행 일자가 삭제되었습니다.");
  }

  // 여행 일자 이동 (in-place) - POST /plans/days/{dayId}/move?toIndex=3&confirm=true
  @PostMapping("/days/{dayId}/move")
  public ResponseEntity<PlanDetail> moveDay(@PathVariable("dayId") Long dayId,
      @RequestParam("toIndex") Integer toIndex,
      @RequestParam(value = "confirm", required = false) Boolean confirm) {
    PlanDetail detail = planService.moveDay(dayId, toIndex, confirm);
    return ResponseEntity.ok(detail);
  }

  // 이동 미리보기: 확장이 필요한지 여부와 새 endDate 예측 - GET /plans/days/{dayId}/move-preview?toIndex=6
  @GetMapping("/days/{dayId}/move-preview")
  public ResponseEntity<com.example.demo.planner.plan.dto.response.MovePreview> movePreview(
      @PathVariable("dayId") Long dayId,
      @RequestParam("toIndex") Integer toIndex) {
    com.example.demo.planner.plan.dto.response.MovePreview preview = planService.movePreview(dayId, toIndex);
    return ResponseEntity.ok(preview);
  }

  // PlanDay 생성 미리보기 - GET /plans/{planId}/days/create-preview?dayIndex=6
  @GetMapping("/{planId}/days/create-preview")
  public ResponseEntity<com.example.demo.planner.plan.dto.response.MovePreview> createDayPreview(
      @PathVariable("planId") Long planId,
      @RequestParam("dayIndex") Integer dayIndex) {
    com.example.demo.planner.plan.dto.response.MovePreview preview = planService.createDayPreview(planId, dayIndex);
    return ResponseEntity.ok(preview);
  }

  // ==================== PlanPlace CRUD ====================

  // 여행 장소 생성 - POST /plans/places
  @PostMapping("/places")
  public ResponseEntity<PlanPlace> createPlace(@RequestBody PlanPlace place) {
    PlanPlace created = planService.createPlace(place);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  // 여행 장소 조회 - GET /plans/places/{placeId}
  @GetMapping("/places/{placeId}")
  public ResponseEntity<PlanPlace> getPlace(@PathVariable("placeId") Long placeId) {
    PlanPlace place = planService.findPlaceById(placeId);
    return ResponseEntity.ok(place);
  }

  // 여행 장소 수정 - PUT /plans/places/{placeId}
  @PutMapping("/places/{placeId}")
  public ResponseEntity<PlanPlace> updatePlace(@PathVariable("placeId") Long placeId, @RequestBody PlanPlace place) {
    planService.updatePlace(placeId, place);
    PlanPlace updated = planService.findPlaceById(placeId);
    return ResponseEntity.ok(updated);
  }

  // 여행 장소 삭제 - DELETE /plans/places/{placeId}
  @DeleteMapping("/places/{placeId}")
  public ResponseEntity<String> deletePlace(@PathVariable("placeId") Long placeId) {
    planService.deletePlace(placeId);
    return ResponseEntity.ok("여행 장소가 삭제되었습니다.");
  }

}
