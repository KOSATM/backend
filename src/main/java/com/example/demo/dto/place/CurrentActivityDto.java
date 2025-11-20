package com.example.demo.dto.place;

import java.time.LocalDate;
import lombok.Data;

@Data
public class CurrentActivityDto {
    private Long id;
    private Long travelspotId;
    private Long actualCost;
    private String review;
    private LocalDate endedAt;
}
