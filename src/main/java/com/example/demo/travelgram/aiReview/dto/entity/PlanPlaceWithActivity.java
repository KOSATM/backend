package com.example.demo.travelgram.aiReview.dto.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import lombok.Data;

@Data
public class PlanPlaceWithActivity {
    private Long dayId;
    private Integer dayIndex;
    private String dayTitle;
    private LocalDate planDate;

    private Long placeId;
    private String placeTitle;
    private Instant startAt;
    private Instant endAt;
    private String placeName;
    private String address;
    private BigDecimal expectedCost;
    private Double lat;
    private Double lng;

    private BigDecimal actualCost;
    private String memo;
    private Instant activityEndedAt;
}

