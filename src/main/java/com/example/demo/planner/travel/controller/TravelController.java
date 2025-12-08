package com.example.demo.planner.travel.controller;

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

import com.example.demo.planner.travel.dto.entity.TravelPlan;
import com.example.demo.planner.travel.dto.response.TravelPlanDetail;
import com.example.demo.planner.travel.service.TravelService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/travel/plans")
@RequiredArgsConstructor
public class TravelController {

    private final TravelService travelService;

    /**
     * 여행 계획 생성 (Plan만 생성, Day/Place는 별도 추가 필요)
     * POST /api/travel/plans
     * @return 생성된 TravelPlan 객체 (id 포함)
     */
    @PostMapping
    public ResponseEntity<TravelPlan> createTravelPlan(@RequestBody TravelPlan travelPlan) {
        try {
            log.info("여행 계획 생성 요청: userId={}", travelPlan.getUserId());
            travelService.createTravelPlan(travelPlan);
            log.info("여행 계획 생성 완료: planId={}", travelPlan.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(travelPlan);
        } catch (Exception e) {
            log.error("여행 계획 생성 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 여행 계획 단건 조회
     * GET /api/travel/plans/{planId}
     */
    @GetMapping("/{planId}")
    public ResponseEntity<TravelPlan> getTravelPlan(@PathVariable Long planId) {
        try {
            log.info("여행 계획 조회 요청: planId={}", planId);
            TravelPlan plan = travelService.getTravelPlanById(planId);
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
     * 여행 계획 상세 조회 (일자별 장소 포함)
     * GET /api/travel/plans/{planId}/detail
     */
    @GetMapping("/{planId}/detail")
    public ResponseEntity<TravelPlanDetail> getTravelPlanDetail(@PathVariable Long planId) {
        try {
            log.info("여행 계획 상세 조회 요청: planId={}", planId);
            TravelPlanDetail detail = travelService.getTravelPlanDetail(planId);
            if (detail == null || detail.getPlan() == null) {
                log.warn("여행 계획 상세 정보를 찾을 수 없음: planId={}", planId);
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(detail);
        } catch (Exception e) {
            log.error("여행 계획 상세 조회 실패: planId={}", planId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 사용자별 여행 계획 목록 조회
     * GET /api/travel/plans?userId={userId}
     */
    @GetMapping
    public ResponseEntity<List<TravelPlan>> getTravelPlansByUser(@RequestParam Long userId) {
        try {
            log.info("사용자별 여행 계획 목록 조회 요청: userId={}", userId);
            List<TravelPlan> plans = travelService.getTravelPlansByUserId(userId);
            return ResponseEntity.ok(plans);
        } catch (Exception e) {
            log.error("사용자별 여행 계획 목록 조회 실패: userId={}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 사용자의 활성 여행 계획 조회 (is_ended = false)
     * GET /api/travel/plans/active?userId={userId}
     */
    @GetMapping("/active")
    public ResponseEntity<TravelPlan> getActiveTravelPlan(@RequestParam Long userId) {
        try {
            log.info("활성 여행 계획 조회 요청: userId={}", userId);
            TravelPlan plan = travelService.getActiveTravelPlanByUserId(userId);
            if (plan == null) {
                log.warn("활성 여행 계획을 찾을 수 없음: userId={}", userId);
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(plan);
        } catch (Exception e) {
            log.error("활성 여행 계획 조회 실패: userId={}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 사용자의 완료된 여행 계획 목록 조회 (is_ended = true)
     * GET /api/travel/plans/completed?userId={userId}
     */
    @GetMapping("/completed")
    public ResponseEntity<List<TravelPlan>> getCompletedTravelPlans(@RequestParam Long userId) {
        try {
            log.info("완료된 여행 계획 목록 조회 요청: userId={}", userId);
            List<TravelPlan> plans = travelService.getCompletedTravelPlansByUserId(userId);
            return ResponseEntity.ok(plans);
        } catch (Exception e) {
            log.error("완료된 여행 계획 목록 조회 실패: userId={}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 모든 여행 계획 조회
     * GET /api/travel/plans/all
     */
    @GetMapping("/all")
    public ResponseEntity<List<TravelPlan>> getAllTravelPlans() {
        try {
            log.info("전체 여행 계획 목록 조회 요청");
            List<TravelPlan> plans = travelService.getAllTravelPlans();
            return ResponseEntity.ok(plans);
        } catch (Exception e) {
            log.error("전체 여행 계획 목록 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 여행 계획 수정
     * PUT /api/travel/plans/{planId}
     */
    @PutMapping("/{planId}")
    public ResponseEntity<String> updateTravelPlan(
            @PathVariable Long planId,
            @RequestBody TravelPlan travelPlan) {
        try {
            log.info("여행 계획 수정 요청: planId={}", planId);

            boolean success = travelService.updateTravelPlanById(planId, travelPlan);
            if (!success) {
                log.warn("여행 계획을 찾을 수 없음: planId={}", planId);
                return ResponseEntity.notFound().build();
            }

            log.info("여행 계획 수정 완료: planId={}", planId);
            return ResponseEntity.ok("여행 계획이 성공적으로 수정되었습니다");
        } catch (Exception e) {
            log.error("여행 계획 수정 실패: planId={}", planId, e);
            return ResponseEntity.internalServerError()
                .body("여행 계획 수정 실패: " + e.getMessage());
        }
    }

    /**
     * 여행 계획 삭제 (Plan과 연결된 모든 데이터 완전 삭제)
     * DELETE /api/travel/plans/{planId}
     */
    @DeleteMapping("/{planId}")
    public ResponseEntity<String> deleteTravelPlan(@PathVariable Long planId) {
        try {
            log.info("여행 계획 삭제 요청: planId={}", planId);

            boolean success = travelService.deleteTravelPlanByIdWithValidation(planId);
            if (!success) {
                log.warn("여행 계획을 찾을 수 없음: planId={}", planId);
                return ResponseEntity.notFound().build();
            }

            log.info("여행 계획 삭제 완료: planId={}", planId);
            return ResponseEntity.ok("여행 계획이 성공적으로 삭제되었습니다");

        } catch (Exception e) {
            log.error("여행 계획 삭제 실패: planId={}", planId, e);
            return ResponseEntity.internalServerError()
                .body("여행 계획 삭제 실패: " + e.getMessage());
        }
    }
}
