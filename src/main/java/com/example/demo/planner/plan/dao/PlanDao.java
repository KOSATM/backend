package com.example.demo.planner.plan.dao;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.planner.plan.dto.entity.Plan;

import java.util.List;

@Mapper
public interface PlanDao {
    
    /**
     * ID로 여행 계획을 조회합니다.
     */
    Plan selectPlanById(Long id);
    
    /**
     * 사용자의 모든 여행 계획을 조회합니다.
     */
    List<Plan> selectPlansByUserId(Long userId);
    
    /**
     * 여행 계획을 생성합니다.
     */
    int insertPlan(Plan Plan);
    
    /**
     * 여행 계획을 수정합니다.
     */
    int updatePlan(Plan Plan);
    
    /**
     * 여행 계획을 삭제합니다.
     */
    int deletePlan(Long id);
}
