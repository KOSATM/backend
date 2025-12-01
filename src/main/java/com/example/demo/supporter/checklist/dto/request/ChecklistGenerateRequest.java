package com.example.demo.supporter.checklist.dto.request;

import lombok.Data;

@Data
public class ChecklistGenerateRequest {
    private Long planId;
    private Integer dayIndex;
    private String title;
}
