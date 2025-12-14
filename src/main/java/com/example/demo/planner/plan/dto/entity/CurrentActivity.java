package com.example.demo.planner.plan.dto.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CurrentActivity {
    private Long id;
    private Long planPlaceId;
    private BigDecimal actualCost;
    private String memo;
    private OffsetDateTime endedAt;
}
