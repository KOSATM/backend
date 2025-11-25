package com.example.demo.planner.travel.dao;

import org.apache.ibatis.annotations.Mapper;
import com.example.demo.planner.travel.dto.entity.TravelPlanSnapshot;
import java.util.List;

@Mapper
public interface TravelPlanSnapshotDao {
    /**
     * ID로 여행 계획 스냅샷을 조회합니다.
     */
    TravelPlanSnapshot selectTravelPlanSnapshotById(Long id);

    /**
     * 사용자의 모든 여행 계획 스냅샷을 조회합니다.
     */
    List<TravelPlanSnapshot> selectTravelPlanSnapshotsByUserId(Long userId);

    /**
     * 사용자의 최신 여행 계획 스냅샷을 조회합니다.
     */
    TravelPlanSnapshot selectLatestTravelPlanSnapshotByUserId(Long userId);

    /**
     * 여행 계획 스냅샷을 생성합니다.
     */
    int insertTravelPlanSnapshot(TravelPlanSnapshot travelPlanSnapshot);

    /**
     * 여행 계획 스냅샷을 삭제합니다.
     */
    int deleteTravelPlanSnapshot(Long id);

    /**
     * 사용자의 모든 여행 계획 스냅샷을 삭제합니다.
     */
    int deleteTravelPlanSnapshotsByUserId(Long userId);
}
