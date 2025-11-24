package com.example.demo.planner.controller;

import com.example.demo.dto.travel.TravelPlan;
import com.example.demo.planner.service.PlannerService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/planner")
@Slf4j
public class PlannerController {
    @Autowired
    private PlannerService plannerService;

    @PostMapping("/plans")
    public String createPlan(@RequestBody TravelPlan travelPlan) {
        plannerService.createTravelPlan(travelPlan);
        return "Travel Plan Created! ID: " + travelPlan.getId();
    }

    @PostMapping(
    value = "/chat",
    consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
    produces = MediaType.TEXT_PLAIN_VALUE
    )
    public String chat(@RequestParam("question") String question) {
        String answer = plannerService.chat(question);
        return answer;
    }
}