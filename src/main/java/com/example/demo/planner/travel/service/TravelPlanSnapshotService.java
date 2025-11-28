package com.example.demo.planner.travel.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.planner.travel.dao.TravelPlanSnapshotDao;
import com.example.demo.planner.travel.dto.entity.TravelPlanSnapshot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * TravelPlanSnapshotService는 여행 계획 스냅샷 관련 비즈니스 로직을 처리합니다.
 * 
 * 스냅샷은 여행 계획의 버전 관리와 히스토리 추적을 위해 사용됩니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TravelPlanSnapshotService {
    private final TravelPlanSnapshotDao travelPlanSnapshotDao;

    // ### 단순 CRUD ###
    // ID로 개별 스냅샷 조회
    public TravelPlanSnapshot getTravelPlanSnapshotById(Long id) {
        log.info("Getting travel plan snapshot by id: {}", id);
        return travelPlanSnapshotDao.selectTravelPlanSnapshotById(id);
    }

    // 사용자의 모든 스냅샷 조회
    public List<TravelPlanSnapshot> getTravelPlanSnapshotsByUserId(Long userId) {
        log.info("Getting all travel plan snapshots for user: {}", userId);
        return travelPlanSnapshotDao.selectTravelPlanSnapshotsByUserId(userId);
    }

    // 사용자의 최신 스냅샷 조회
    public TravelPlanSnapshot getLatestTravelPlanSnapshot(Long userId) {
        log.info("Getting latest travel plan snapshot for user: {}", userId);
        return travelPlanSnapshotDao.selectLatestTravelPlanSnapshotByUserId(userId);
    }

    // 스냅샷 저장
    public TravelPlanSnapshot saveTravelPlanSnapshot(TravelPlanSnapshot travelPlanSnapshot) {
        log.info("Saving travel plan snapshot for user: {}", travelPlanSnapshot.getUserId());
        travelPlanSnapshotDao.insertTravelPlanSnapshot(travelPlanSnapshot);
        return travelPlanSnapshot;
    }

    // 특정 스냅샷 삭제
    public void deleteTravelPlanSnapshot(Long id) {
        log.info("Deleting travel plan snapshot: {}", id);
        travelPlanSnapshotDao.deleteTravelPlanSnapshot(id);
    }

    // 사용자의 모든 스냅샷 삭제
    public void deleteTravelPlanSnapshotsByUserId(Long userId) {
        log.info("Deleting all travel plan snapshots for user: {}", userId);
        travelPlanSnapshotDao.deleteTravelPlanSnapshotsByUserId(userId);
    }

}
