package com.example.demo.supporter.checklist.dto.entity;

import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class Checklist {
    private Long id;
    private Long userId;
    private Integer dayIndex;
    private OffsetDateTime createdAt;
}
