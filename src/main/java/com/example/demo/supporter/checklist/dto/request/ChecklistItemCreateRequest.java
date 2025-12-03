package com.example.demo.supporter.checklist.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 체크리스트 항목 생성 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChecklistItemCreateRequest {
    
    private Long checklistId;
    private String content;
    private String category;
    private Boolean isChecked;
}