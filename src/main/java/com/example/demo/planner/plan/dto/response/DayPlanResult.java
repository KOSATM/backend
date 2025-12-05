package com.example.demo.planner.plan.dto.response;

import java.util.ArrayList;
import java.util.List;

import com.example.demo.planner.plan.dto.ClusterPlace;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DayPlanResult {

    private int dayNumber;
    private List<ClusterPlace> places = new ArrayList<>();

}