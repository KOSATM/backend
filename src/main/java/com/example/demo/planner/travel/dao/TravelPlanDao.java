package com.example.demo.planner.travel.dao;

import org.apache.ibatis.annotations.Mapper;
import com.example.demo.planner.travel.dto.entity.TravelPlan;
import java.util.List;

@Mapper
public interface TravelPlanDao {
    
    /**
     * ID로 여행 계획을 조회합니다.
     */
    TravelPlan selectTravelPlanById(Long id);
    
    /**
     * 사용자의 모든 여행 계획을 조회합니다.
     */
    List<TravelPlan> selectTravelPlansByUserId(Long userId);
    
    /**
     * 여행 계획을 생성합니다.
     */
    int insertTravelPlan(TravelPlan travelPlan);
    
    /**
     * 여행 계획을 수정합니다.
     */
    int updateTravelPlan(TravelPlan travelPlan);
    
    /**
     * 여행 계획을 삭제합니다.
     */
    int deleteTravelPlan(Long id);
}
