package com.example.demo.travelgram.aiReview.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.travelgram.aiReview.dto.response.AiReviewStyleResponse;
import com.example.demo.travelgram.aiReview.service.AiReviewService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/aiReview")
public class AiReviewController {

    private final AiReviewService aiReviewService;
        // 1) AI 스타일 생성
    @PostMapping("/{postId}/ai/styles")
    public ResponseEntity<List<AiReviewStyleResponse>> generateStyles(
            @PathVariable Long postId
    ) {
        return ResponseEntity.ok(aiReviewService.generateAiStyles(postId));
    }
    
}
