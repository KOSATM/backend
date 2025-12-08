package com.example.demo.travelgram.review.ai.dto.entity;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class AiReviewAnalysis {
    private Long id;
    private String inputJson;
    private String outputJson;
    private OffsetDateTime createdAt;
    private Long reviewPostId;
}
