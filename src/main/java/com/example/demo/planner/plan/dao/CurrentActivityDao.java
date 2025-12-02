package com.example.demo.planner.plan.dao;

import com.example.demo.planner.plan.dto.entity.CurrentActivity;

public interface CurrentActivityDao {

    void insertCurrentActivity(CurrentActivity currentActivity);

    void updateCurrentActivity(CurrentActivity currentActivity);

    CurrentActivity selectCurrentActivityById(Long id);
    CurrentActivity selectCurrentActivityByPlanPlaceId(Long placeId);

    // List<CurrentActivity> selectCurrentActivitiesByPlanId(Long planPlaceId);
    
    void deleteCurrentActivity(Long id);
}
