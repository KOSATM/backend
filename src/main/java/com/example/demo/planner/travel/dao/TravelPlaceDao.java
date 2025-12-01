package com.example.demo.planner.travel.dao;

import org.apache.ibatis.annotations.Mapper;
import com.example.demo.planner.travel.dto.entity.TravelPlace;
import java.util.List;

@Mapper
public interface TravelPlaceDao {
    
    /**
     * ID로 여행 장소를 조회합니다.
     */
    TravelPlace selectTravelPlaceById(Long id);
    
    /**
     * 여행 날짜의 모든 장소를 조회합니다.
     * 시간 순서대로 정렬됩니다.
     */
    List<TravelPlace> selectTravelPlacesByDayId(Long dayId);
    
    /**
     * 여행 계획의 모든 장소를 조회합니다.
     * 날짜와 시간 순서대로 정렬됩니다.
     */
    List<TravelPlace> selectTravelPlacesByPlanId(Long travelPlanId);
    
    /**
     * 여행 장소를 생성합니다.
     */
    int insertTravelPlace(TravelPlace travelPlace);
    
    /**
     * 여행 장소를 수정합니다.
     */
    int updateTravelPlace(TravelPlace travelPlace);
    
    /**
     * 여행 장소를 삭제합니다.
     */
    int deleteTravelPlace(Long id);
    
    /**
     * 여행 날짜의 모든 장소를 삭제합니다.
     */
    int deleteTravelPlacesByDayId(Long dayId);
}
