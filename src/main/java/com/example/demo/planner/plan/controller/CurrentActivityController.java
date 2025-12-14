package com.example.demo.planner.plan.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
    @PostMapping
    public ResponseEntity<String> saveCurrentActivity(@RequestBody CurrentActivity currentActivity) {
        log.info("받은 데이터: {}", currentActivity);
        service.saveCurrentActivity(currentActivity);
        return ResponseEntity.ok("Current activity saved successfully");
    }
}
