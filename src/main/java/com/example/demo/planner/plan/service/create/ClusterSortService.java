package com.example.demo.planner.plan.service.create;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.planner.plan.dto.Cluster;
import com.example.demo.planner.travel.cluster.GeoUtils;

@Service
public class ClusterSortService {

    public List<Cluster> sortClusters(List<Cluster> clusters) {

        if (clusters.size() <= 1) return clusters;

        List<Cluster> remaining = new ArrayList<>(clusters);
        List<Cluster> result = new ArrayList<>();

        // 1. 시작 클러스터 선택 (가장 중앙에 가까운 것)
        Cluster start = pickStartCluster(remaining);
        result.add(start);
        remaining.remove(start);

        // 2. Nearest Neighbor 정렬
        Cluster current = start;
        while (!remaining.isEmpty()) {
            Cluster next = findNearestCluster(current, remaining);
            result.add(next);
            remaining.remove(next);
            current = next;
        }

        return result;
    }

    /**
     * 서울 중심(lat/lng 평균)에 가장 가까운 클러스터를 시작점으로 선택
     */
    private Cluster pickStartCluster(List<Cluster> clusters) {
        double avgLat = clusters.stream().mapToDouble(Cluster::getCenterLat).average().orElse(0);
        double avgLng = clusters.stream().mapToDouble(Cluster::getCenterLng).average().orElse(0);

        return clusters.stream()
                .min(Comparator.comparingDouble(c ->
                        GeoUtils.haversine(
                                c.getCenterLat(), c.getCenterLng(),
                                avgLat, avgLng)))
                .orElse(clusters.get(0));
    }

    /**
     * 현재 클러스터에서 가장 가까운 클러스터 선택
     */
    private Cluster findNearestCluster(Cluster current, List<Cluster> candidates) {
        return candidates.stream()
                .min(Comparator.comparingDouble(c ->
                        GeoUtils.haversine(
                                current.getCenterLat(), current.getCenterLng(),
                                c.getCenterLat(), c.getCenterLng())))
                .orElse(candidates.get(0));
    }
}
