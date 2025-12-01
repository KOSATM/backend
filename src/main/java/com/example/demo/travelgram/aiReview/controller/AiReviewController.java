package com.example.demo.travelgram.aiReview.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.travelgram.aiReview.agent.TrendSearchAgent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;



@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/ai")
public class AiReviewController {

    private final TrendSearchAgent trendSearchAgent;

    @GetMapping("/trend")
    public String getTrend(@RequestParam String keyword) {
        return trendSearchAgent.generateTrend(keyword);
    }

}
