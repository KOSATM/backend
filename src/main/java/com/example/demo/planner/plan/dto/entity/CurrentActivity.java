package com.example.demo.planner.plan.dto.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import lombok.Builder;
import lombok.Getter;


@Builder
@Getter
public class CurrentActivity {
    private Long id;
    private Long planPlaceId;
    private BigDecimal actualCost;
    private String memo;
    private OffsetDateTime ended_at;

}
