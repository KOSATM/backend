package com.example.demo.dto.plannner;

import java.time.OffsetDateTime;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class TravelSpot {
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
