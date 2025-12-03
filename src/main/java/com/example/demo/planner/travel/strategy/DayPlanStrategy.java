package com.example.demo.planner.travel.strategy;

import java.util.List;

import com.example.demo.planner.travel.dto.DayTarget;

public interface DayPlanStrategy {
    List<DayTarget> createDayTargets(int duration);
}