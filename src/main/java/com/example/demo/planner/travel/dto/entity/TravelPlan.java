package com.example.demo.planner.travel.dto.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Getter;

@Getter
public class TravelPlan {
    private Long id;
    private Long userId;
    private BigDecimal budget;
    private LocalDate startDate; // 여행일자는 여행지 달력에 맞춰서 봐야함
    private LocalDate endDate;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Boolean isEnded;
}