package com.example.demo.dto.plannner;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import lombok.Data;

@Data
public class CurrentActivity {
    private Long id;
    private Long travelPlaceId;
    private BigDecimal actualCost;
    private String memo;
    private OffsetDateTime endedAt;
}
