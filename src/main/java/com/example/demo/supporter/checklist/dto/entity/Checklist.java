package com.example.demo.supporter.checklist.dto.entity;

import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class Checklist {
    private Long id; //PK
    private Long userId; //FK
    private Integer dayIndex; //day1, day2, ...
    private OffsetDateTime createdAt;
}
