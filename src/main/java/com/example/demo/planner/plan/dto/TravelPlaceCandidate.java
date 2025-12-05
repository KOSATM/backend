package com.example.demo.planner.plan.dto;

import com.example.demo.planner.plan.dto.entity.TravelPlaces;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class TravelPlaceCandidate {
    double score;
    TravelPlaces travelPlaces;

    public String getNormalizedCategory() {
        return travelPlaces.getNormalizedCategory();
    }

    public Double getTravelPlacesLat() {
        return travelPlaces.getLat();
    }

    public Double getTravelPlacesLng() {
        return travelPlaces.getLng();
    }

    public Long getId() {
        return travelPlaces.getId();
    }

}
