package com.example.demo.planner.plan.dao;

import java.util.List;

import com.example.demo.planner.plan.dto.entity.CurrentActivity;

public interface CurrentActivityDao {

    void insertCurrentActivity(CurrentActivity currentActivity);

    void updateCurrentActivity(CurrentActivity currentActivity);

    CurrentActivity selectCurrentActivityById(Long id);

    // List<CurrentActivity> selectCurrentActivitiesByPlanId(Long planPlaceId);
    
    void deleteCurrentActivity(Long id);
}
