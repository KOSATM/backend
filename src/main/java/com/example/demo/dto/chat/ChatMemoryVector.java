package com.example.demo.dto.chat;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.Data;

@Data
public class ChatMemoryVector {
    private Long id;
    private Long userId;
    private String agentName;
    private Integer orderIndex;
    private String content;
    private Long tokenUsage;
    private OffsetDateTime createdAt;
    private String role;
    private List<Double> embedding;
}
