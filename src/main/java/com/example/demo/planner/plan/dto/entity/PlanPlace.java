package com.example.demo.planner.plan.dto.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@ToString
public class PlanPlace {
    private Long id;
    private Long dayId;
    private Integer order; // 같은 날 내에서의 장소 순서 (1-based)
    private String title;
    private OffsetDateTime startAt; 
    private OffsetDateTime endAt; 
    private String placeName;
    private String address;
    private double lat;
    private double lng;
    private BigDecimal expectedCost;
}