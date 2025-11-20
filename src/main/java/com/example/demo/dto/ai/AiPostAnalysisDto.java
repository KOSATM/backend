package com.example.demo.dto.ai;

import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class AiPostAnalysisDto {
    private Long id;
    private String promptText;
    private String inputJson;
    private String outputJson;
    private OffsetDateTime createdAt;
    private Long userId;
    private Long postId;
}
