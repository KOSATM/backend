package com.example.demo.travelgram.aiReview.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.travelgram.aiReview.service.AiReviewService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestBody;



@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/aiReviews")
public class AiReviewController {

    private final AiReviewService aiReviewService;

    // mybatis, db 통신
    @PostMapping("/{}/hashtags")
    public String postMethodName(@RequestBody String entity) {
        
        return entity;
    }
    
    // 2) 스타일 선택 적용 (AI 캡션까지 자동 반영)
    // @PutMapping("/{postId}/style")
    // public ResponseEntity<ReviewPostResponse> applyStyle(
    //         @PathVariable Long postId,
    //         @RequestBody ReviewStyleSelectRequest request
    // ) {
    //     return ResponseEntity.ok(reviewService.applyStyle(postId, request));
    // }

    // 1) AI 스타일 생성
    // @PostMapping("/{postId}/ai/styles")
    // public ResponseEntity<List<AiReviewStyleResponse>> generateStyles(
    //         @PathVariable Long postId
    // ) {
    //     return ResponseEntity.ok(aiReviewService.generateAiStyles(postId));
    // }
    
}
