package com.example.demo.dto.checklist;

import java.time.LocalDate;
import lombok.Data;

@Data
public class ChecklistDto {
    private Long id;
    private Long userId;
    private Long dayIndex;
    private LocalDate createdAt;
}
