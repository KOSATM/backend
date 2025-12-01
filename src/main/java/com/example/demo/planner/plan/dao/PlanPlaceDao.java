package com.example.demo.planner.plan.dao;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.planner.plan.dto.entity.PlanPlace;

import java.util.List;

@Mapper
public interface PlanPlaceDao {
    
    /**
     * ID로 여행 장소를 조회합니다.
     */
    PlanPlace selectPlaceById(Long id);
    
    /**
     * 여행 날짜의 모든 장소를 조회합니다.
     * 시간 순서대로 정렬됩니다.
     */
    List<PlanPlace> selectPlacesByDayId(Long dayId);
    
    /**
     * 여행 계획의 모든 장소를 조회합니다.
     * 날짜와 시간 순서대로 정렬됩니다.
     */
    List<PlanPlace> selectPlacesByPlanId(Long planId);
    
    /**
     * 여행 장소를 생성합니다.
     */
    int insertPlace(PlanPlace place);
    
    /**
     * 여행 장소를 수정합니다.
     */
    int updatePlace(PlanPlace place);
    
    /**
     * 여행 장소를 삭제합니다.
     */
    int deletePlace(Long id);
    
    /**
     * 여행 날짜의 모든 장소를 삭제합니다.
     */
    int deletePlacesByDayId(Long dayId);
}
