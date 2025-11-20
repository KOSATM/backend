package com.example.demo.dto.ai;

import java.time.OffsetDateTime;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class AiStyleRecommendation {
    private Long id;
    private String name;
    private BigDecimal confidence;
    private OffsetDateTime createdAt;
    private Long analysisId;
}
