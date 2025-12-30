package com.example.demo.planner.plan.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.demo.planner.plan.dto.entity.PlanSnapshot;

@Mapper
public interface PlanSnapshotDao {
    // ID로 개별 스냅샷 조회
    PlanSnapshot selectPlanSnapshotById(Long id);

    // virsionNo로 개별 스냅샷 조회
    PlanSnapshot selectPlanSnapshotByVersionNo(@Param("versionNo") int versionNo, @Param("userId") Long userId);

    // 사용자의 모든 스냅샷 조회
    List<PlanSnapshot> selectPlanSnapshotsByUserId(Long userId);

    // 사용자의 최신 스냅샷 조회
    PlanSnapshot selectLatestPlanSnapshotByUserId(Long userId);
    
    // 사용자의 최신 스냅샷 버전 조회
    int selectLatestVersionNoByUserId(Long userId);

    // 사용자의 특정 버전의 스냅샷 조회
    PlanSnapshot selectPlanSnapshotByUserIdAndVersionNo(PlanSnapshot planSnapshot);

    // 스냅샷 저장
    int insertPlanSnapshot(PlanSnapshot PlanSnapshot);

    // 특정 스냅샷 삭제
    int deletePlanSnapshot(Long id);

    // 사용자의 모든 스냅샷 삭제
    int deletePlanSnapshotsByUserId(Long userId);

    // 사용자의 스냅샷 가장 최신 스냅샷 버전 조회
    int selectLatestPlanSnapshotIdByUserId(Long userId);
}
