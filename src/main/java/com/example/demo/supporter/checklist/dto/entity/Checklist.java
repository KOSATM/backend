package com.example.demo.supporter.checklist.dto.entity;

import java.time.OffsetDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Checklist {
    private Long id; //PK
    private Long userId; //FK
    private Integer dayIndex; //day1, day2, ...
    private OffsetDateTime createdAt;
}
