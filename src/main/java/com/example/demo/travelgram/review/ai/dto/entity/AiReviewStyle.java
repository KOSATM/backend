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
public class AiReviewStyle {
    private Long id;
    private String name;
    private String toneCode;
    private String caption;
    private OffsetDateTime createdAt;
    private Long reviewAnalysisId;
}
