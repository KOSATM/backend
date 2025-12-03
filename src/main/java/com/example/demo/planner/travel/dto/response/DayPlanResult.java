package com.example.demo.planner.travel.dto.response;

import java.util.ArrayList;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * Day별 여행 계획
 */
@Getter
@Builder
public class DayPlanResult {
    private int dayNumber;

    @Builder.Default
    private List<ClusterResult> clusters = new ArrayList<>();

    /**
     * 전체 장소 개수
     */
    public int getTotalPlaceCount() {
        return clusters.stream()
                .mapToInt(ClusterResult::getPlaceCount)
                .sum();
    }

    /**
     * 특정 카테고리 개수
     */
    public int getCategoryCount(String category) {
        return (int) clusters.stream()
                .flatMap(c -> c.getPlaces().stream())
                .filter(p -> category.equals(p.getTravelPlaces().getNormalizedCategory()))
                .count();
    }
}