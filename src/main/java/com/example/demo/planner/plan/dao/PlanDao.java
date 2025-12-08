package com.example.demo.planner.plan.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.demo.planner.plan.dto.TravelPlaceCandidate;
import com.example.demo.planner.plan.dto.entity.Plan;
import com.example.demo.planner.plan.dto.entity.TravelPlaces;

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
     * 사용자의 활성화된 여행 계획을 조회합니다.
     */
    Plan selectActiveTravelPlanByUserId(Long userId);

    /**
     * 여행 계획을 생성합니다.
     */
    int insertPlan(Plan Plan);

    /**
     * 여행 계획을 수정합니다.
     */
    int updatePlan(Plan Plan);

    void updatePlanTitleById(@Param("id") Long id, @Param("title") String title);

    /**
     * 여행 계획을 삭제합니다.
     */
    int deletePlan(Long id);


    /* 여행 계획 관련 메서드*/
    TravelPlaces findById(@Param("id") Long id);
    List<TravelPlaces> findAll(@Param("limit") int limit, @Param("offset") int offset);
    List<TravelPlaceCandidate> searchByVector(@Param("embedding") float[] embedding, @Param("limit") int limit);
    List<TravelPlaceCandidate> searchMissingCategoryByVector(Map<String, Object> params);

    /*is_ended=true && title = null일시 제목 자동생성위해 조회 */
    List<Plan> selectEndedPlansWithNoTitle();
}
