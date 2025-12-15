package com.example.demo.planner.plan.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.planner.plan.dto.entity.CurrentActivity;
import com.example.demo.planner.plan.service.CurrentActivityService;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/travel/current-activity")
@Slf4j
public class CurrentActivityController {

    @Autowired
    private CurrentActivityService service;

    // planPlaceId로 current activity 조회
    @GetMapping("/{planPlaceId}")
    public ResponseEntity<CurrentActivity> getActivityByPlanPlaceId(@PathVariable("planPlaceId") Long planPlaceId) {
        log.info("조회 요청: planPlaceId={}", planPlaceId);
        CurrentActivity activity = service.getActivityByPlanPlaceId(planPlaceId);
        return ResponseEntity.ok(activity);
    }

    // current activity Upsert
    @PostMapping
    public ResponseEntity<String> saveOrUpdateCurrentActivity(@RequestBody CurrentActivity currentActivity) {
        log.info("받은 데이터: {}", currentActivity);
        service.saveOrUpdateCurrentActivity(currentActivity);
        return ResponseEntity.ok("Current activity saved successfully");
    }
}
