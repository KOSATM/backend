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

  /**
   * 여행 계획 생성 (샘플 데이터 포함)
   * POST /api/plans?days=3
   */
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

  /**
   * 여행 계획 단건 조회
   * GET /api/plans/{planId}
   */
  @GetMapping("/{planId}")
  public ResponseEntity<Plan> getPlan(@PathVariable Long planId) {
    try {
      log.info("여행 계획 조회 요청: planId={}", planId);
      Plan plan = planService.findById(planId);
      if (plan == null) {
        log.warn("여행 계획을 찾을 수 없음: planId={}", planId);
        return ResponseEntity.notFound().build();
      }
      return ResponseEntity.ok(plan);
    } catch (Exception e) {
      log.error("여행 계획 조회 실패: planId={}", planId, e);
      return ResponseEntity.internalServerError().build();
    }
  }

  /**
   * 여행 계획 상세 조회 (Days + Places 포함)
   * GET /api/plans/{planId}/detail
   */
  @GetMapping("/{planId}/detail")
  public ResponseEntity<PlanDetail> getPlanDetail(@PathVariable Long planId) {
    try {
      log.info("여행 계획 상세 조회 요청: planId={}", planId);
      PlanDetail planDetail = planService.getPlanDetail(planId);
      if (planDetail == null) {
        log.warn("여행 계획을 찾을 수 없음: planId={}", planId);
        return ResponseEntity.notFound().build();
      }
      return ResponseEntity.ok(planDetail);
    } catch (Exception e) {
      log.error("여행 계획 상세 조회 실패: planId={}", planId, e);
      return ResponseEntity.internalServerError().build();
    }
  }

  /**
   * 사용자별 여행 계획 목록 조회
   * GET /api/plans/user/{userId}
   */
  @GetMapping("/user/{userId}")
  public ResponseEntity<List<Plan>> getPlansByUserId(@PathVariable Long userId) {
    try {
      log.info("사용자 여행 계획 목록 조회: userId={}", userId);
      List<Plan> plans = planService.findByUserId(userId);
      return ResponseEntity.ok(plans);
    } catch (Exception e) {
      log.error("사용자 여행 계획 목록 조회 실패: userId={}", userId, e);
      return ResponseEntity.internalServerError().build();
    }
  }

  /**
   * 여행 계획 수정
   * PUT /api/plans/{planId}
   */
  @PutMapping("/{planId}")
  public ResponseEntity<Void> updatePlan(@PathVariable Long planId, @RequestBody Plan plan) {
    try {
      log.info("여행 계획 수정 요청: planId={}", planId);
      // Plan은 @Builder이므로 수정 메서드 필요
      // 현재 PlanService에 update 메서드가 없으므로 추가 필요
      log.warn("Plan 수정 기능은 아직 구현되지 않았습니다");
      return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    } catch (Exception e) {
      log.error("여행 계획 수정 실패: planId={}", planId, e);
      return ResponseEntity.internalServerError().build();
    }
  }

  /**
   * 여행 계획 삭제
   * DELETE /api/plans/{planId}
   */
  @DeleteMapping("/{planId}")
  public ResponseEntity<Void> deletePlan(@PathVariable Long planId) {
    try {
      log.info("여행 계획 삭제 요청: planId={}", planId);
      // 현재 PlanService에 delete 메서드가 없으므로 추가 필요
      log.warn("Plan 삭제 기능은 아직 구현되지 않았습니다");
      return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    } catch (Exception e) {
      log.error("여행 계획 삭제 실패: planId={}", planId, e);
      return ResponseEntity.internalServerError().build();
    }
  }
  
}
