package com.example.demo.planner.travel.dto.entity;

import java.time.OffsetDateTime;

import lombok.Getter;

@Getter
public class TravelPlanSnapshot {
    private Long id;
    private Long userId;
    private Integer versionNo;
    private String snapshotJson;
    private OffsetDateTime createdAt;
}