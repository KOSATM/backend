package com.example.demo.planner.plan.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.planner.plan.dao.PlanSnapshotDao;
import com.example.demo.planner.plan.dto.entity.PlanSnapshot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * planSnapshotService는 여행 계획 스냅샷 관련 비즈니스 로직을 처리합니다.
 * 
 * 스냅샷은 여행 계획의 버전 관리와 히스토리 추적을 위해 사용됩니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanSnapshotService {
    private final PlanSnapshotDao planSnapshotDao;

    // ### 단순 CRUD ###
    // ID로 개별 스냅샷 조회
    public PlanSnapshot getPlanSnapshotById(Long id) {
        log.info("Getting travel plan snapshot by id: {}", id);
        return planSnapshotDao.selectPlanSnapshotById(id);
    }

    // 사용자의 모든 스냅샷 조회
    public List<PlanSnapshot> getPlanSnapshotsByUserId(Long userId) {
        log.info("Getting all travel plan snapshots for user: {}", userId);
        return planSnapshotDao.selectPlanSnapshotsByUserId(userId);
    }

    // 사용자의 최신 스냅샷 조회
    public PlanSnapshot getLatestPlanSnapshot(Long userId) {
        log.info("Getting latest travel plan snapshot for user: {}", userId);
        return planSnapshotDao.selectLatestPlanSnapshotByUserId(userId);
    }

    // 스냅샷 저장
    public PlanSnapshot savePlanSnapshot(PlanSnapshot planSnapshot) {
        log.info("Saving travel plan snapshot for user: {}", planSnapshot.getUserId());
        planSnapshotDao.insertPlanSnapshot(planSnapshot);
        return planSnapshot;
    }

    // 특정 스냅샷 삭제
    public void deletePlanSnapshot(Long id) {
        log.info("Deleting travel plan snapshot: {}", id);
        planSnapshotDao.deletePlanSnapshot(id);
    }

    // 사용자의 모든 스냅샷 삭제
    public void deletePlanSnapshotsByUserId(Long userId) {
        log.info("Deleting all travel plan snapshots for user: {}", userId);
        planSnapshotDao.deletePlanSnapshotsByUserId(userId);
    }

}
