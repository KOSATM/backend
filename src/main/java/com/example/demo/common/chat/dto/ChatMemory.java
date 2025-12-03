package com.example.demo.common.chat.dto;

import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class ChatMemory {
    private Long id;
    private Long userId;
    private String agentName;
    private Integer orderIndex;
    private String content;
    private Long tokenUsage;
    private OffsetDateTime createdAt;
    private String role;
}
