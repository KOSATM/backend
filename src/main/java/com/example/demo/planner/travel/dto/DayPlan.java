package com.example.demo.planner.travel.dto;

import java.util.ArrayList;
import java.util.List;

import com.example.demo.planner.travel.dto.response.ClusterResult;
import com.example.demo.planner.travel.dto.response.TravelPlaceSearchResult;

import lombok.Getter;

@Getter
public class DayPlan {
    private final int dayNumber;
    private final DayTarget target;
    private final List<ClusterResult> clusters = new ArrayList<>();

    public DayPlan(int dayNumber, DayTarget target) {
        this.dayNumber = dayNumber;
        this.target = target;
    }

    public void addCluster(ClusterResult cluster) {
        clusters.add(cluster);
    }

    public int getCountByCategory(String category) {
        return (int) clusters.stream()
                .flatMap(cluster -> cluster.getPlaces().stream())
                .filter(r -> category.equals(r.getTravelPlaces().getNormalizedCategory()))
                .count();
    }

    public int getTotalCount() {
        return clusters.stream()
                .mapToInt(ClusterResult::getPlaceCount)
                .sum();
    }

    public boolean isFull() {
        return getTotalCount() >= target.getMaxTotal();
    }

    public TravelPlaceSearchResult getLastPlace() {
        if (clusters == null || clusters.isEmpty()) {
            return null;
        }
        ClusterResult lastCluster = clusters.get(clusters.size() - 1);
        if (lastCluster.getPlaces() == null || lastCluster.getPlaces().isEmpty()) {
            return null;
        }
        List<TravelPlaceSearchResult> places = lastCluster.getPlaces();
        return places.get(places.size() - 1);
    }
}