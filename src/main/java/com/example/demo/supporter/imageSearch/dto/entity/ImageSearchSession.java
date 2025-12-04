package com.example.demo.supporter.imageSearch.dto.entity;


import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class ImageSearchSession {
    private Long id; //PK
    private Long userId; //FK
    private OffsetDateTime createdAt;
    private String actionType; //"save_only", "add plan", "replaced_plan"

    public enum ActionType {
        SAVE_ONLY,
        ADD_PLAN,
        REPLACED_PLAN
    }
}
