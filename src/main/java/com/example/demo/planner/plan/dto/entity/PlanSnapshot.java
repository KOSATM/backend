package com.example.demo.planner.plan.dto.entity;

import java.time.OffsetDateTime;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;

@Builder
@ToString
@Getter
public class PlanSnapshot {
    private Long id;
    private Long userId;
    private Integer versionNo;
    private String snapshotJson;
    private OffsetDateTime createdAt;
}