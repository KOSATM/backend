package com.example.demo.planner.travel.dto.response;

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
}
