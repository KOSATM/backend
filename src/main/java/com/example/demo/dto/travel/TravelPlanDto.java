package com.example.demo.dto.travel;

import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class TravelPlanDto {
    private Long id;
    private Long userId;
    private OffsetDateTime startDate;
    private OffsetDateTime endDate;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Boolean isEnded;
}
