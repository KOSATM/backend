package com.example.demo.planner.plan.dto.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class PlanPlace {
    private Long id;
    private Long dayId;
    private String title;
    private OffsetDateTime startAt; 
    private OffsetDateTime endAt; 
    private String placeName;
    private String address;
    private double lat;
    private double lng;
    private BigDecimal expectedCost;
}