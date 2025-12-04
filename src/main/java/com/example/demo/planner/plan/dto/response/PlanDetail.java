package com.example.demo.planner.plan.dto.response;

import java.util.List;

import com.example.demo.planner.plan.dto.entity.Plan;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlanDetail {
    private Plan plan;
    private List<PlanDayWithPlaces> days;
}
