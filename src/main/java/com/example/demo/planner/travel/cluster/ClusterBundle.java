package com.example.demo.planner.travel.cluster;

import java.util.List;

import com.example.demo.planner.travel.dto.response.ClusterResult;
import com.example.demo.planner.travel.dto.response.TravelPlaceSearchResult;

public record ClusterBundle(
        List<ClusterResult> clusters,
        List<TravelPlaceSearchResult> noise
) {}