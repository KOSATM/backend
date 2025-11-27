package com.example.demo.planner.travel.controller;

import com.example.demo.planner.travel.agent.TravelPlanVersionAgent;
import com.example.demo.planner.travel.dto.entity.TravelPlanSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 여행 계획 버전 관리 API
 * 
 * 사용자와의 상호작용을 통해 계획 버전을 관리합니다:
 * - 계획 수정 여부 확인
 * - 스냅샷 생성 및 저장
 * - 이전 버전 복원 (rollback)
 * - 버전 간 비교 및 상세 정보 조회
 */
@Slf4j
@RestController
@RequestMapping("/api/travel/plan-versions")
@RequiredArgsConstructor
public class TravelPlanVersionController {

    private final TravelPlanVersionAgent travelPlanVersionAgent;

    /**
     * 여행 계획 버전을 관리합니다 (사용자 상호작용)
     * 
     * 프로세스:
     * 1. 사용자에게 계획 수정 여부 확인
     * 2. 승인 시 스냅샷 생성
     * 3. 필요시 이전 버전 복원 제안
     * 
     * @param userId 사용자 ID
     * @param planId 여행 계획 ID
     * @param currentPlanJson 현재 여행 계획 JSON
     * @return LLM 에이전트의 상호작용 응답
     */
    // @PostMapping("/manage")
    // public ResponseEntity<String> managePlanVersionWithInteraction(
    //         @RequestParam Long userId,
    //         @RequestParam Long planId,
    //         @RequestBody String currentPlanJson) {
        
    //     log.info("여행 계획 버전 관리 시작 - 사용자: {}, 계획ID: {}", userId, planId);
        
    //     try {
    //         String response = travelPlanVersionAgent.manageVersionWithUserInteraction(
    //             userId, planId, currentPlanJson);
            
    //         log.info("버전 관리 상호작용 완료");
    //         return ResponseEntity.ok(response);
            
    //     } catch (Exception e) {
    //         log.error("버전 관리 중 오류 발생", e);
    //         return ResponseEntity.status(500).body("버전 관리 중 오류 발생: " + e.getMessage());
    //     }
    // }

    /**
     * 이전 버전을 직접 복원합니다
     * 
     * 복원 방식: 이전 버전 조회 → JSON 복사 → 버전+1 → 새로운 행으로 저장
     * 원본 버전은 보존됩니다.
     * 
     * @param userId 사용자 ID
     * @param targetVersionNo 복원할 버전 번호
     * @return 복원된 스냅샷 (새로운 버전)
     */
    // @PostMapping("/restore")
    // public ResponseEntity<TravelPlanSnapshot> restoreVersion(
    //         @RequestParam Long userId,
    //         @RequestParam Integer targetVersionNo) {
        
    //     log.info("버전 복원 요청 - 사용자: {}, 대상버전: {}", userId, targetVersionNo);
        
    //     try {
    //         TravelPlanSnapshot restored = travelPlanVersionAgent.restoreVersionDirect(
    //             userId, targetVersionNo);
            
    //         log.info("버전 복원 완료 - 새버전: {}", restored.getVersionNo());
    //         return ResponseEntity.ok(restored);
            
    //     } catch (IllegalArgumentException e) {
    //         log.warn("버전을 찾을 수 없음", e);
    //         return ResponseEntity.notFound().build();
            
    //     } catch (Exception e) {
    //         log.error("버전 복원 중 오류 발생", e);
    //         return ResponseEntity.status(500).build();
    //     }
    // }
}
