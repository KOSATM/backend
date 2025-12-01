package com.example.demo.planner.plan.dao;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.planner.plan.dto.entity.PlanSnapshot;

import java.util.List;

@Mapper
public interface PlanSnapshotDao {
    // ID로 개별 스냅샷 조회
    PlanSnapshot selectTravelPlanSnapshotById(Long id);

    // 사용자의 모든 스냅샷 조회
    List<PlanSnapshot> selectTravelPlanSnapshotsByUserId(Long userId);

    // 사용자의 최신 스냅샷 조회
    PlanSnapshot selectLatestTravelPlanSnapshotByUserId(Long userId);

    // 스냅샷 저장
    int insertTravelPlanSnapshot(PlanSnapshot travelPlanSnapshot);

    // 특정 스냅샷 삭제
    int deleteTravelPlanSnapshot(Long id);

    // 사용자의 모든 스냅샷 삭제
    int deleteTravelPlanSnapshotsByUserId(Long userId);
}
