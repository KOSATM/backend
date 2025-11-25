package com.example.demo.planner.travel.dto.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import lombok.Getter;

@Getter
public class TravelPlace {
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