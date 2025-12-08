package com.example.demo.planner.plan.dto.response;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * 장소의 위치 정보 (몇 일차, 몇 번째 장소인지)
 */
@Builder
@Getter
@ToString
public class PlacePosition {
    private Integer dayIndex;      // 몇 일차
    private Integer order;         // 해당 일차에서 몇 번째 (1부터 시작)
    private LocalDate date;        // 여행 날짜
    private String placeName;      // 실제 매칭된 장소명
    private Long dayId;            // PlanDay ID (전체 일정 조회용)
}
