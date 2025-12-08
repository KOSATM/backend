package com.example.demo.planner.plan.dto.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@ToString
@JsonDeserialize(builder = Plan.PlanBuilder.class)
public class Plan {
    private Long id;
    private Long userId;
    private BigDecimal budget;
    private LocalDate startDate; // 여행일자는 여행지 달력에 맞춰서 봐야함
    private LocalDate endDate;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Boolean isEnded;
    private String title; // 여행 제목 끝날 때 생성

    @JsonPOJOBuilder(withPrefix = "")
    public static class PlanBuilder {
    }
}
