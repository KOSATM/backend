package com.example.demo.planner.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.dto.travel.TravelPlan;

@Mapper
public interface TravelPlanMapper {
    int insertTravelPlan(TravelPlan travelPlan);
}
