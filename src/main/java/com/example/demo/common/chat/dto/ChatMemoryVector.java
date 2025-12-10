package com.example.demo.common.chat.dto;

import java.time.OffsetDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatMemoryVector {
    private Long id;
    private Long userId;
    private String agentName;
    private Integer orderIndex;
    private String content;
    private Long tokenUsage;
    private OffsetDateTime createdAt;
    private String role;
    private float[] embedding;
}
