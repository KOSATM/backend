package com.example.demo.travelgram.review.ai.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.travelgram.review.ai.agent.ReviewImageAnalysisAgent;
import com.example.demo.travelgram.review.ai.agent.TrendSearchAgent;
import com.example.demo.travelgram.review.ai.service.AiReviewService;
import com.example.demo.travelgram.review.service.ReviewService;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/ai/review")
public class AiReviewController {

    private final TrendSearchAgent trendSearchAgent;
    private final ReviewImageAnalysisAgent photoAnalysisAgent;
    private final AiReviewService aiReviewService;
    private final ReviewService reviewService;

    @GetMapping("/trend")
    public String getTrend(@RequestParam("keyword") String keyword) {
        return trendSearchAgent.generateTrend(keyword);
    }

    @GetMapping("/{planId}/input")
    public ResponseEntity<ObjectNode> getInput(@PathVariable("planId") Long planId) {
        ObjectNode json = aiReviewService.createPlanInputJson(planId);
        return ResponseEntity.ok(json);
    }

    @GetMapping("{planId}/title")
    public ResponseEntity<String> getPlanTitle(@PathVariable("planId") Long planId) {
        String title = aiReviewService.generatePlanTitle(planId);
        return ResponseEntity.ok(title);
    }

    @PostMapping("/generate-styles")
    public ResponseEntity<?> generateStyles(
            @RequestParam("planId") Long planId, 
            @RequestParam("reviewPostId") Long reviewPostId) {
        
        // 생성 및 저장 수행
        var styles = aiReviewService.createAndSaveStyles(planId, reviewPostId);
        
        return ResponseEntity.ok(styles);
    }
}
