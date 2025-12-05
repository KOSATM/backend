package com.example.demo.planner.plan.dto.entity;

import java.time.LocalDate;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@JsonDeserialize(builder = PlanDay.PlanDayBuilder.class)
public class PlanDay {
    private Long id;
    private Long planId;
    private Integer dayIndex; // 날짜 미정(5일 여행할거야) -> 날짜 매핑(비행기 표 끊은 시점)
    private String title; // 하루에 대한 요약 제목
    private LocalDate planDate; // 여행계획일자는 한국기준이기 때문에, KST 기준으로 진행
    // 사용자가 어디에 있든 여행지의 달력을 따라가야 함 
    
    @JsonPOJOBuilder(withPrefix = "")
    public static class PlanDayBuilder {
    }
}
