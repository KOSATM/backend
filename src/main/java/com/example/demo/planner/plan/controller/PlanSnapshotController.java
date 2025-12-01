package com.example.demo.planner.plan.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.planner.plan.dto.entity.PlanSnapshot;
import com.example.demo.planner.plan.service.PlanSnapshotService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


/**
 * PlanSnapshotController는 여행 계획 스냅샷 관련 API를 처리합니다.
 * 
 * 스냅샷은 여행 계획의 버전 관리와 히스토리 추적을 위해 사용됩니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/travel/snapshots")
@RequiredArgsConstructor
public class PlanSnapshotController {

    private final PlanSnapshotService planSnapshotService;

    /**
     * ID로 여행 계획 스냅샷을 조회합니다.
     *
     * @param id 스냅샷 ID
     * @return 여행 계획 스냅샷
     */
    @GetMapping("/{id}")
    public ResponseEntity<PlanSnapshot> getPlanSnapshot(@PathVariable Long id) {
        log.info("GET /api/travel/snapshots/{}", id);
        PlanSnapshot snapshot = planSnapshotService.getPlanSnapshotById(id);
        if (snapshot == null) {
            log.warn("Travel plan snapshot not found: {}", id);
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(snapshot);
    }

    /**
     * 사용자의 모든 여행 계획 스냅샷을 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 여행 계획 스냅샷 목록
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PlanSnapshot>> getPlanSnapshotsByUser(@PathVariable Long userId) {
        log.info("GET /api/travel/snapshots/user/{}", userId);
        List<PlanSnapshot> snapshots = planSnapshotService.getPlanSnapshotsByUserId(userId);
        return ResponseEntity.ok(snapshots);
    }

    /**
     * 사용자의 최신 여행 계획 스냅샷을 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 최신 여행 계획 스냅샷
     */
    @GetMapping("/user/{userId}/latest")
    public ResponseEntity<PlanSnapshot> getLatestPlanSnapshot(@PathVariable Long userId) {
        log.info("GET /api/travel/snapshots/user/{}/latest", userId);
        PlanSnapshot snapshot = planSnapshotService.getLatestPlanSnapshot(userId);
        if (snapshot == null) {
            log.warn("Latest travel plan snapshot not found for user: {}", userId);
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(snapshot);
    }

    /**
     * 여행 계획 스냅샷을 생성합니다.
     *
     * @param PlanSnapshot 생성할 여행 계획 스냅샷
     * @return 생성된 여행 계획 스냅샷 (ID 포함)
     */
    @PostMapping("/create")
    public ResponseEntity<PlanSnapshot> createPlanSnapshot(@RequestBody PlanSnapshot PlanSnapshot) {
        log.info("POST /api/travel/snapshots - Creating snapshot for user: {}", PlanSnapshot.getUserId());
        try {
            PlanSnapshot saved = planSnapshotService.savePlanSnapshot(PlanSnapshot);
            log.info("Travel plan snapshot created with id: {}", saved.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            log.error("Error creating travel plan snapshot", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 여행 계획 스냅샷을 삭제합니다.
     *
     * @param id 스냅샷 ID
     * @return 응답 상태
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlanSnapshot(@PathVariable Long id) {
        log.info("DELETE /api/travel/snapshots/{}", id);
        try {
            PlanSnapshot snapshot = planSnapshotService.getPlanSnapshotById(id);
            if (snapshot == null) {
                log.warn("Travel plan snapshot not found: {}", id);
                return ResponseEntity.notFound().build();
            }
            planSnapshotService.deletePlanSnapshot(id);
            log.info("Travel plan snapshot deleted: {}", id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting travel plan snapshot", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 사용자의 모든 여행 계획 스냅샷을 삭제합니다.
     *
     * @param userId 사용자 ID
     * @return 응답 상태
     */
    @DeleteMapping("/user/{userId}")
    public ResponseEntity<Void> deletePlanSnapshotsByUser(@PathVariable Long userId) {
        log.info("DELETE /api/travel/snapshots/user/{}", userId);
        try {
            planSnapshotService.deletePlanSnapshotsByUserId(userId);
            log.info("All travel plan snapshots deleted for user: {}", userId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting travel plan snapshots for user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
