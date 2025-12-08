package com.example.demo.planner.plan.dao;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.planner.plan.dto.entity.PlanPlace;

import java.util.List;

@Mapper
public interface PlanPlaceDao {
    
    /**
     * ID로 여행 장소를 조회합니다.
     */
    PlanPlace selectPlanPlaceById(Long id);

    /**
     * 여행 날짜의 모든 장소를 조회합니다.
     * 시간 순서대로 정렬됩니다.
     */
    List<PlanPlace> selectPlanPlacesByPlanDayId(Long dayId);
    
    /**
     * 여행 계획의 모든 장소를 조회합니다.
     * 날짜와 시간 순서대로 정렬됩니다.
     */
    List<PlanPlace> selectPlanPlacesByPlanId(Long planId);
    
    /**
     * 여행 장소를 생성합니다.
     */
    int insertPlanPlace(PlanPlace place);
    
    /**
     * 여행 장소를 수정합니다.
     */
    int updatePlanPlace(PlanPlace place);
    
    /**
     * 여행 장소를 삭제합니다.
     */
    int deletePlanPlace(Long id);
    
    /**
     * 여행 날짜의 모든 장소를 삭제합니다.
     */
    int deletePlanPlaceByDayId(Long dayId);
    
    /**
     * dayId로 장소 목록 조회 (Agent용)
     */
    List<PlanPlace> selectPlacesByDayId(Long dayId);
    
    /**
     * placeId로 장소 조회 (Agent용)
     */
    PlanPlace selectPlaceById(Long placeId);
}
