package com.example.demo.planner.travel.dto.response;

import java.util.ArrayList;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * 전체 여행 계획
 */
@Getter
@Builder
public class TravelPlanResult {
    private String destination;
    private int duration;
    
    @Builder.Default
    private List<DayPlanResult> days = new ArrayList<>();

    /**
     * 전체 장소 개수
     */
    public int getTotalPlaceCount() {
        return days.stream()
                .mapToInt(DayPlanResult::getTotalPlaceCount)
                .sum();
    }

    /**
     * 특정 Day 가져오기
     */
    public DayPlanResult getDay(int dayNumber) {
        return days.stream()
                .filter(d -> d.getDayNumber() == dayNumber)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Day " + dayNumber + " not found"));
    }
}