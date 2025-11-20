package com.example.demo.dto.plannner;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import lombok.Data;

@Data
public class TravelPlace {
    private Long id;
    private Long dayId;
    private String title;
    private OffsetDateTime startedAt; 
    private OffsetDateTime endedAt; 
    private String placeName;
    private String address;
    private double lat;
    private double lng;
    private BigDecimal expectedCost;
}
