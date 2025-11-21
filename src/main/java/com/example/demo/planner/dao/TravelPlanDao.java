package com.example.demo.planner.dao;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.dto.travel.TravelPlan;

@Mapper
public interface TravelPlanDao {
    int insertTravelPlan(TravelPlan travelPlan);
}
