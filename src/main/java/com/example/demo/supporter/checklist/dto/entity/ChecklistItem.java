package com.example.demo.supporter.checklist.dto.entity;

import java.time.OffsetDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChecklistItem {
    private Long id; //PK
    private Long checklistId; //FK
    private String content;
    private String category; //"location", "general", "weather"
    private Boolean isChecked;
    private OffsetDateTime createdAt;

    public enum Category {
        LOCATION,
        GENERAL,
        WEATHER
    }
}
