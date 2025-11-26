package com.example.demo.planner.travel.dao;

import org.apache.ibatis.annotations.Mapper;
import com.example.demo.planner.travel.dto.entity.TravelPlanSnapshot;
import java.util.List;

@Mapper
public interface TravelPlanSnapshotDao {
    // ID로 개별 스냅샷 조회
    TravelPlanSnapshot selectTravelPlanSnapshotById(Long id);

    // 사용자의 모든 스냅샷 조회
    List<TravelPlanSnapshot> selectTravelPlanSnapshotsByUserId(Long userId);

    // 사용자의 최신 스냅샷 조회
    TravelPlanSnapshot selectLatestTravelPlanSnapshotByUserId(Long userId);

    // 스냅샷 저장
    int insertTravelPlanSnapshot(TravelPlanSnapshot travelPlanSnapshot);

    // 특정 스냅샷 삭제
    int deleteTravelPlanSnapshot(Long id);

    // 사용자의 모든 스냅샷 삭제
    int deleteTravelPlanSnapshotsByUserId(Long userId);
}
