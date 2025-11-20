package com.example.demo.dto.place;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import lombok.Data;

@Data
public class CurrentActivity {
    private Long id;
    private Long travelPlaceId;
    private BigDecimal actualCost;
    private String memo;
    private OffsetDateTime endedAt; // 사용자가 조기종료 눌렀을시 사용
}
