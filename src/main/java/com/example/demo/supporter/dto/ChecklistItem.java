package com.example.demo.supporter.dto;

import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class ChecklistItem {
    private Long id;
    private Long checklistId;
    private String content;
    private String category;
    private Boolean isChecked;
    private OffsetDateTime createdAt;
}
