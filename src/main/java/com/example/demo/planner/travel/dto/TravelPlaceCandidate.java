package com.example.demo.planner.travel.dto;

import com.example.demo.planner.travel.dto.entity.TravelPlaces;

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
        return travelPlaces != null ? travelPlaces.getNormalizedCategory() : null;
    }

}
