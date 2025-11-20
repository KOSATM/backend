package com.example.demo.dto.travel;

import java.time.OffsetDateTime;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class TravelSpotDto {
    private Long id;
    private Long dayId;
    private String title;
    private OffsetDateTime startAtUtc;
    private OffsetDateTime endAtUtc;
    private String placeName;
    private String address;
    private BigDecimal lat;
    private BigDecimal lng;
    private Long cost;
}
