package com.example.demo.planner.plan.dao;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.planner.plan.dto.entity.PlanSnapshot;

import java.util.List;

@Mapper
public interface PlanSnapshotDao {
    // ID로 개별 스냅샷 조회
    PlanSnapshot selectPlanSnapshotById(Long id);

    // 사용자의 모든 스냅샷 조회
    List<PlanSnapshot> selectPlanSnapshotsByUserId(Long userId);

    // 사용자의 최신 스냅샷 조회
    PlanSnapshot selectLatestPlanSnapshotByUserId(Long userId);

    // 스냅샷 저장
    int insertPlanSnapshot(PlanSnapshot PlanSnapshot);

    // 특정 스냅샷 삭제
    int deletePlanSnapshot(Long id);

    // 사용자의 모든 스냅샷 삭제
    int deletePlanSnapshotsByUserId(Long userId);
}
