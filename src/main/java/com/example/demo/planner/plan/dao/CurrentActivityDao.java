package com.example.demo.planner.plan.dao;

import java.util.List;

import com.example.demo.planner.plan.dto.entity.CurrentActivity;

public interface CurrentActivityDao {

    void insertCurrentActivity(CurrentActivity currentActivity);

    CurrentActivity selectCurrentActivityById(Long planPlaceId);

    List<CurrentActivity> selectCurrentActivitiesByPlanId(Long planPlaceId);
    
    int deleteCurrentActivity(Long id);
}
