package com.example.demo.planner.travel.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.planner.travel.cluster.GeoUtils;
import com.example.demo.planner.travel.dto.Cluster;
import com.example.demo.planner.travel.dto.ClusterPlace;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ClusterInternalSortService {


    public void sortInternal(Cluster cluster) {

        log.info("=== [정렬 전] Cluster {} ===", cluster.getId());
        printPlaces(cluster);

        List<ClusterPlace> list = new ArrayList<>(cluster.getPlaces());
        if (list.size() <= 1)
            return;

        List<ClusterPlace> sorted = new ArrayList<>();

        double curLat = cluster.getCenterLat();
        double curLng = cluster.getCenterLng();

        while (!list.isEmpty()) {

            ClusterPlace nearest = null;
            double minDist = Double.MAX_VALUE;

            for (ClusterPlace p : list) {
                double d = GeoUtils.haversine(curLat, curLng, p.getLat(), p.getLng());
                if (d < minDist) {
                    minDist = d;
                    nearest = p;
                }
            }

            sorted.add(nearest);
            list.remove(nearest);

            curLat = nearest.getLat();
            curLng = nearest.getLng();
        }

        cluster.setPlaces(sorted);

        log.info("=== [정렬 후] Cluster {} ===", cluster.getId());
        printPlaces(cluster);
    }

    private void printPlaces(Cluster cluster) {
        for (ClusterPlace p : cluster.getPlaces()) {
            log.info(" - {} (lat={}, lng={}, dist={})",
                    p.getOriginal().getTravelPlaces().getTitle(),
                    p.getLat(),
                    p.getLng(),
                    p.getDistance());
        }
    }
}
