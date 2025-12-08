package com.example.demo.planner.plan.dto;

import com.example.demo.planner.plan.utils.GeoUtils;

import lombok.Getter;

@Getter
public class ClusterPlace {

    private final TravelPlaceCandidate original;
    private final double centerLat;
    private final double centerLng;
    private final double distance; // center까지 거리(km)

    public ClusterPlace(TravelPlaceCandidate original,
                        double centerLat,
                        double centerLng) {
        this.original = original;
        this.centerLat = centerLat;
        this.centerLng = centerLng;

        this.distance = GeoUtils.haversine(
                original.getTravelPlacesLat(),
                original.getTravelPlacesLng(),
                centerLat,
                centerLng
        );
    }

    public double getLat() {
        return original.getTravelPlacesLat();
    }

    public double getLng() {
        return original.getTravelPlacesLng();
    }

    public String getCategory() {
        return original.getNormalizedCategory();
    }
}
