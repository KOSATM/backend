package com.example.demo.planner.travel.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * 클러스터 (지리적으로 가까운 장소들의 묶음)
 */
@Getter
@Builder
public class ClusterResult {
    private int clusterNumber;
    private List<TravelPlaceSearchResult> places;
    
    /**
     * 장소 개수
     */
    public int getPlaceCount() {
        return places.size();
    }
    
    /**
     * 클러스터 중심 좌표
     */
    public double[] getCenterCoordinates() {
        double avgLat = places.stream()
                .mapToDouble(p -> p.getTravelPlaces().getLat())
                .average()
                .orElse(0.0);
        
        double avgLng = places.stream()
                .mapToDouble(p -> p.getTravelPlaces().getLng())
                .average()
                .orElse(0.0);
        
        return new double[]{avgLat, avgLng};
    }
}