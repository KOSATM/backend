package com.example.demo.dto.aiReview;

import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class AiReviewStyle {
    private Long id;
    private String name;
    private OffsetDateTime createdAt;
    private Long reviewAnalysisId;
}
