package com.example.demo.supporter.dto;

import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class Checklist {
    private Long id;
    private Long userId;
    private Integer dayIndex;
    private OffsetDateTime createdAt;
}
