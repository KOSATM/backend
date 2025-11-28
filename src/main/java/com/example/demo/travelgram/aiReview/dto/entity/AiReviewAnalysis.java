package com.example.demo.travelgram.aiReview.dto.entity;

import java.time.OffsetDateTime;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class AiReviewAnalysis {
    private Long id;
    private String promptText;
    private String inputJson;
    private String outputJson;
    private OffsetDateTime createdAt;
    private Long userId;
    private Long reviewPostId;
}
