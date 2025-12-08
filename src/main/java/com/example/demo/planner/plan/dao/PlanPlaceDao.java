package com.example.demo.planner.plan.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.planner.plan.dto.entity.PlanPlace;

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
     * 여행 장소들을 생성합니다.
     */
    int insertPlanPlaceBatch(List<PlanPlace> planPlaces);
    
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

    // ==================== EDIT/DELETE Operations ====================

    /**
     * place의 plan_day_id 변경 (day 간 이동)
     */
    void updatePlanDayId(@org.apache.ibatis.annotations.Param("id") Long id, 
                         @org.apache.ibatis.annotations.Param("planDayId") Long planDayId);

    /**
     * place의 order 변경 (순서 교환)
     */
    void updatePlaceOrder(@org.apache.ibatis.annotations.Param("id") Long id, 
                          @org.apache.ibatis.annotations.Param("order") int order);

    /**
     * place의 plan_day_id와 order 동시 변경
     */
    void updatePlanDayIdAndOrder(@org.apache.ibatis.annotations.Param("id") Long id, 
                                  @org.apache.ibatis.annotations.Param("planDayId") Long planDayId, 
                                  @org.apache.ibatis.annotations.Param("order") int order);

    /**
     * place 정보 업데이트 (장소 교체 시)
     */
    void updatePlaceInfo(@org.apache.ibatis.annotations.Param("id") Long id,
                         @org.apache.ibatis.annotations.Param("placeName") String placeName,
                         @org.apache.ibatis.annotations.Param("address") String address,
                         @org.apache.ibatis.annotations.Param("latitude") Double latitude,
                         @org.apache.ibatis.annotations.Param("longitude") Double longitude,
                         @org.apache.ibatis.annotations.Param("category") String category,
                         @org.apache.ibatis.annotations.Param("cost") java.math.BigDecimal cost);

    /**
     * place의 시작 시간 변경
     */
    void updatePlaceTime(@org.apache.ibatis.annotations.Param("id") Long id, 
                         @org.apache.ibatis.annotations.Param("time") java.time.LocalTime time);

    /**
     * place의 duration 변경
     */
    void updatePlaceDuration(@org.apache.ibatis.annotations.Param("id") Long id, 
                             @org.apache.ibatis.annotations.Param("duration") Integer duration);

    /**
     * place 삭제 (ID로)
     */
    void deletePlanPlaceById(Long id);
}
