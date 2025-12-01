package com.example.demo.planner.plan.dao;

import java.util.List;

import com.example.demo.planner.plan.dto.entity.Plan;

public class CurrentActivityDao {
        /**
     * ID로 여행 계획을 조회합니다.
     */
    Plan selectTravelPlanById(Long id);
    
    /**
     * 사용자의 모든 여행 계획을 조회합니다.
     */
    List<Plan> selectTravelPlansByUserId(Long userId);
    
    /**
     * 여행 계획을 생성합니다.
     */
    int insertTravelPlan(Plan travelPlan);
    
    /**
     * 여행 계획을 수정합니다.
     */
    int updateTravelPlan(Plan travelPlan);
    
    /**
     * 여행 계획을 삭제합니다.
     */
    int deleteTravelPlan(Long id);
}
