package com.example.demo.dto.checklist;

import java.time.LocalDate;
import lombok.Data;

@Data
public class ChecklistItemDto {
    private Long id;
    private Long checklistId;
    private String content;
    private String category;
    private Boolean isChecked;
    private LocalDate createdAt;
}
