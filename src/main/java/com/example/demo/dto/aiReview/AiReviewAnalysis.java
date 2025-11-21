package com.example.demo.dto.aiReview;

import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class AiReviewAnalysis {
    private Long id;
    private String promptText;
    private String inputJson;
    private String outputJson;
    private OffsetDateTime createdAt;
    private Long userId;
    private Long reviewPostId;
}
