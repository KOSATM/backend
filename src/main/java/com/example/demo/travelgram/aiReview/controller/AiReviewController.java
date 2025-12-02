package com.example.demo.travelgram.aiReview.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.travelgram.agent.TrendSearchAgent;
import com.example.demo.travelgram.aiReview.service.AiReviewService;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;



@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/ai/review")
public class AiReviewController {

    private final TrendSearchAgent trendSearchAgent;
    private final AiReviewService aiReviewService;

    @GetMapping("/trend")
    public String getTrend(@RequestParam("keyword") String keyword) {
        return trendSearchAgent.generateTrend(keyword);
    }


    @GetMapping("/{planId}/input")
    public ResponseEntity<ObjectNode> getInput(@PathVariable("planId") Long planId) {
        ObjectNode json = aiReviewService.createPlanInputJson(planId);
        return ResponseEntity.ok(json);
    }
}
