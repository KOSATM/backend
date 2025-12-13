package com.example.demo.planner.plan.dto.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@NoArgsConstructor
public class PlanScheduleRow {
    private Long planId;
    private BigDecimal budget;
    private LocalDate startDate;
    private LocalDate endDate;

    private Long dayId;
    private Integer dayIndex;
    private LocalDate planDate;
    private String dayTitle;

    private Long placeId;
    private String placeTitle;
    private OffsetDateTime startAt;
    private OffsetDateTime endAt;
    private String placeName;
    private String address;
    private BigDecimal expectedCost;
    private String normalizedCategory;
    private String firstImage;
    private String firstImage2;
    private Boolean isEnded;
}