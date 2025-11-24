package com.example.demo.travelgram.aiReview.dto;

import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class AiReviewHashtag {
    private Long id;
    private String name;
    private OffsetDateTime createdAt;
    private Long reviewAnalysisId;
}
