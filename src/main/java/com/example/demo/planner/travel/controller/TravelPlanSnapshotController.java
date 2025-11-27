package com.example.demo.planner.travel.controller;

import com.example.demo.planner.travel.agent.TravelPlanVersionAgent;
import com.example.demo.planner.travel.dto.entity.TravelPlanSnapshot;
import com.example.demo.planner.travel.service.TravelPlanSnapshotService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * TravelPlanSnapshotController는 여행 계획 스냅샷 관련 API를 처리합니다.
 * 
 * 스냅샷은 여행 계획의 버전 관리와 히스토리 추적을 위해 사용됩니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/travel/snapshots")
@RequiredArgsConstructor
public class TravelPlanSnapshotController {

    private final TravelPlanSnapshotService travelPlanSnapshotService;
    private final TravelPlanVersionAgent travelPlanVersionAgent;

    /**
     * ID로 여행 계획 스냅샷을 조회합니다.
     *
     * @param id 스냅샷 ID
     * @return 여행 계획 스냅샷
     */
    @GetMapping("/{id}")
    public ResponseEntity<TravelPlanSnapshot> getTravelPlanSnapshot(@PathVariable Long id) {
        log.info("GET /api/travel/snapshots/{}", id);
        TravelPlanSnapshot snapshot = travelPlanSnapshotService.getTravelPlanSnapshotById(id);
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
    public ResponseEntity<List<TravelPlanSnapshot>> getTravelPlanSnapshotsByUser(@PathVariable Long userId) {
        log.info("GET /api/travel/snapshots/user/{}", userId);
        List<TravelPlanSnapshot> snapshots = travelPlanSnapshotService.getTravelPlanSnapshotsByUserId(userId);
        return ResponseEntity.ok(snapshots);
    }

    /**
     * 사용자의 최신 여행 계획 스냅샷을 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 최신 여행 계획 스냅샷
     */
    @GetMapping("/user/{userId}/latest")
    public ResponseEntity<TravelPlanSnapshot> getLatestTravelPlanSnapshot(@PathVariable Long userId) {
        log.info("GET /api/travel/snapshots/user/{}/latest", userId);
        TravelPlanSnapshot snapshot = travelPlanSnapshotService.getLatestTravelPlanSnapshot(userId);
        if (snapshot == null) {
            log.warn("Latest travel plan snapshot not found for user: {}", userId);
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(snapshot);
    }

    /**
     * 여행 계획 스냅샷을 생성합니다.
     *
     * @param travelPlanSnapshot 생성할 여행 계획 스냅샷
     * @return 생성된 여행 계획 스냅샷 (ID 포함)
     */
    @PostMapping("/create")
    public ResponseEntity<TravelPlanSnapshot> createTravelPlanSnapshot(@RequestBody TravelPlanSnapshot travelPlanSnapshot) {
        log.info("POST /api/travel/snapshots - Creating snapshot for user: {}", travelPlanSnapshot.getUserId());
        try {
            TravelPlanSnapshot saved = travelPlanSnapshotService.saveTravelPlanSnapshot(travelPlanSnapshot);
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
    public ResponseEntity<Void> deleteTravelPlanSnapshot(@PathVariable Long id) {
        log.info("DELETE /api/travel/snapshots/{}", id);
        try {
            TravelPlanSnapshot snapshot = travelPlanSnapshotService.getTravelPlanSnapshotById(id);
            if (snapshot == null) {
                log.warn("Travel plan snapshot not found: {}", id);
                return ResponseEntity.notFound().build();
            }
            travelPlanSnapshotService.deleteTravelPlanSnapshot(id);
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
    public ResponseEntity<Void> deleteTravelPlanSnapshotsByUser(@PathVariable Long userId) {
        log.info("DELETE /api/travel/snapshots/user/{}", userId);
        try {
            travelPlanSnapshotService.deleteTravelPlanSnapshotsByUserId(userId);
            log.info("All travel plan snapshots deleted for user: {}", userId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting travel plan snapshots for user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/llm-test/{userId}")
    public Object postMethodName(@PathVariable Long userId, @RequestParam String question) throws JsonMappingException, JsonProcessingException {
        // ObjectMapper objectMapper = new ObjectMapper();
        // return objectMapper.readValue(travelPlanVersionAgent.manageVersionWithUserInteraction(userId, question), TravelPlanSnapshot.class);
        
        return travelPlanVersionAgent.manageVersionWithUserInteraction(userId, question);
    }
    
}
