package com.example.demo.dto.chat;

import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class ChatMemory {
    private Long id;
    private Long userId;
    private String agentId;
    private Integer turnIndex;
    private String content;
    private Integer tokenCount;
    private OffsetDateTime createdAt;
    private String role;
}
