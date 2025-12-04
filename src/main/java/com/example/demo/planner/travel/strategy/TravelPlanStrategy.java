package com.example.demo.planner.travel.strategy;

public interface TravelPlanStrategy {
    DayRequirement getDayRequirement(int dayIndex, int duration);
}
