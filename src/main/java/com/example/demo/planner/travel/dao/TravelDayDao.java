package com.example.demo.planner.travel.dao;

import org.apache.ibatis.annotations.Mapper;
import com.example.demo.planner.travel.dto.entity.TravelDay;
import java.util.List;

/**
 * 여행 날짜 DAO
 * 
 * 책임:
 * - 여행 계획의 각 날짜별 일정 관리
 * - 여행 날짜의 CRUD 작업
 * - 날짜 순서 관리
 */
@Mapper
public interface TravelDayDao {
    
    /**
     * ID로 여행 날짜를 조회합니다.
     */
    TravelDay selectTravelDayById(Long id);
    
    /**
     * 여행 계획의 모든 날짜를 조회합니다.
     * 날짜 순서대로 정렬됩니다.
     */
    List<TravelDay> selectTravelDaysByPlanId(Long travelPlanId);
    
    /**
     * 여행 날짜를 생성합니다.
     */
    int insertTravelDay(TravelDay travelDay);
    
    /**
     * 여행 날짜를 수정합니다.
     */
    int updateTravelDay(TravelDay travelDay);
    
    /**
     * 여행 날짜를 삭제합니다.
     */
    int deleteTravelDay(Long id);
    
    /**
     * 여행 계획의 모든 날짜를 삭제합니다.
     */
    int deleteTravelDaysByPlanId(Long travelPlanId);
}
