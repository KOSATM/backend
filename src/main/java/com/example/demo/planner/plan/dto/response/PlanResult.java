package com.example.demo.planner.plan.dto.response;

import java.util.List;

import com.example.demo.planner.plan.dto.entity.Plan;
import com.example.demo.planner.plan.dto.entity.PlanDay;
import com.example.demo.planner.plan.dto.entity.PlanPlace;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@ToString
public class PlanResult {
    Plan plan;
    List<PlanDay> planDays;
    List<PlanPlace> planPlaces;
}
