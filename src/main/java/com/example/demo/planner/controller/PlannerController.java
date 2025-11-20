package com.example.demo.planner.controller;

import com.example.demo.dto.travel.TravelPlan;
import com.example.demo.planner.service.PlannerService;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/planner")
@RequiredArgsConstructor
public class PlannerController {

    private final PlannerService plannerService;

    @PostMapping("/plans")
    public String createPlan(@RequestBody TravelPlan travelPlan) {
        plannerService.createTravelPlan(travelPlan);
        return "Travel Plan Created! ID: " + travelPlan.getId();
    }
}