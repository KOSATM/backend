package com.example.demo.planner.travel.dto.response;

import java.util.Comparator;
import java.util.List;

import com.example.demo.planner.travel.cluster.GeoUtils;
import com.example.demo.planner.travel.dto.entity.TravelPlaces;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@ToString
public class TravelPlaceSearchResult {
    double score;
    TravelPlaces travelPlaces;

    public static final Comparator<TravelPlaceSearchResult> BY_SCORE_DESC = Comparator
            .comparingDouble(TravelPlaceSearchResult::getScore).reversed();

    /**
     * 두 장소 간 거리 계산 (km)
     */
    public double distanceTo(TravelPlaceSearchResult other) {
        return GeoUtils.haversine(
                this.travelPlaces.getLat(),
                this.travelPlaces.getLng(),
                other.travelPlaces.getLat(),
                other.travelPlaces.getLng());
    }

    /**
     * 평균 점수 계산
     */
    public static double calculateAverageScore(List<TravelPlaceSearchResult> places) {
        if (places == null || places.isEmpty()) {
            return 0.0;
        }

        return places.stream()
                .mapToDouble(TravelPlaceSearchResult::getScore)
                .average()
                .orElse(0.0);
    }
}
