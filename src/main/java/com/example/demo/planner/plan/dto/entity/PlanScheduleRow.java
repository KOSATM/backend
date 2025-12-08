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
    Long planId;
    BigDecimal budget;
    LocalDate startDate;
    LocalDate endDate;

    Long dayId;
    Integer dayIndex;
    LocalDate planDate;
    String dayTitle;

    Long placeId;
    String placeTitle;
    OffsetDateTime startAt;
    OffsetDateTime endAt;
    String placeName;
    String address;
    BigDecimal expectedCost;
}