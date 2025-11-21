package com.example.demo.dto.ai;

import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class AiReviewHashtag {
    private Long id;
    private String name;
    private OffsetDateTime createdAt;
    private Long reviewAnalysisId;
}
