package com.example.demo.planner.plan.service.create;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.DoublePoint;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import org.springframework.stereotype.Service;

import com.example.demo.planner.plan.dto.Cluster;
import com.example.demo.planner.plan.dto.ClusterBundle;
import com.example.demo.planner.plan.dto.ClusterPlace;
import com.example.demo.planner.plan.dto.TravelPlaceCandidate;
import com.example.demo.planner.plan.utils.CategoryNames;
import com.example.demo.planner.travel.cluster.GeoUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class KMeansClusterService {

    public ClusterBundle cluster(List<TravelPlaceCandidate> candidates, int k) {

        ClusterBundle bundle = new ClusterBundle();

        // candidates < k
        if (candidates.size() < k) {
            return fallbackClusters(candidates, k, bundle);
        }

        // --------------------- KMeans 준비 ---------------------
        double[][] points = new double[candidates.size()][2];
        for (int i = 0; i < candidates.size(); i++) {
            points[i][0] = candidates.get(i).getTravelPlacesLat();
            points[i][1] = candidates.get(i).getTravelPlacesLng();
        }

        KMeansPlusPlusClusterer<DoublePoint> clusterer = new KMeansPlusPlusClusterer<>(k, 1000);
        List<DoublePoint> pointList = Arrays.stream(points)
                .map(DoublePoint::new)
                .collect(Collectors.toList());

        List<CentroidCluster<DoublePoint>> raw = clusterer.cluster(pointList);

        int clusterId = 1;

        for (CentroidCluster<DoublePoint> c : raw) {
            Cluster cluster = new Cluster();
            cluster.setId(clusterId++);
            cluster.setCenterLat(c.getCenter().getPoint()[0]);
            cluster.setCenterLng(c.getCenter().getPoint()[1]);

            for (DoublePoint p : c.getPoints()) {
                int idx = index(points, p.getPoint());
                TravelPlaceCandidate place = candidates.get(idx);

                cluster.getPlaces().add(
                        new ClusterPlace(place, cluster.getCenterLat(), cluster.getCenterLng()));
            }

            bundle.addCluster(cluster);
        }

        // --------------------- KMeans 로그 ---------------------
        log.info("=== KMeans 결과 ===");
        for (Cluster c : bundle.getClusters()) {
            log.info("Cluster {} (center={}, {}) size={}",
                    c.getId(),
                    c.getCenterLat(),
                    c.getCenterLng(),
                    c.getPlaces().size());
        }

        // --------------------- 필수 카테고리 보강 ---------------------
        strengthenAllClusters(bundle, candidates);

        return bundle;
    }

    // ============================================================
    // ★ 클러스터 보강 로직 (SPOT·FOOD 최소 개수 보장)
    // ============================================================
    private void strengthenAllClusters(ClusterBundle bundle, List<TravelPlaceCandidate> global) {

        log.info("=== 클러스터 보강 시작 ===");

        int MIN_SPOT = 3;
        int MIN_FOOD = 3;

        for (Cluster cl : bundle.getClusters()) {
            strengthenCluster(cl, global, CategoryNames.SPOT, MIN_SPOT);
            strengthenCluster(cl, global, CategoryNames.FOOD, MIN_FOOD);
        }

        log.info("=== 클러스터 보강 완료 ===");
    }

    private void strengthenCluster(Cluster cluster,
            List<TravelPlaceCandidate> global,
            String category,
            int minCount) {

        long current = cluster.getPlaces().stream()
                .filter(p -> p.getOriginal().getNormalizedCategory().equals(category))
                .count();

        if (current >= minCount)
            return;

        int lack = (int) (minCount - current);

        log.warn("[Cluster {}] {} 부족 → {}개 보강 시작",
                cluster.getId(), category, lack);

        double cx = cluster.getCenterLat();
        double cy = cluster.getCenterLng();

        // 전역 후보 중 아직 해당 클러스터에서 사용되지 않은 동일 카테고리
        List<TravelPlaceCandidate> candidates = global.stream()
                .filter(p -> p.getNormalizedCategory().equals(category))
                .filter(p -> cluster.getPlaces().stream()
                        .noneMatch(cp -> cp.getOriginal().getId().equals(p.getId())))
                .sorted(Comparator.comparingDouble(p -> GeoUtils.haversine(
                        cx, cy,
                        p.getTravelPlacesLat(),
                        p.getTravelPlacesLng())))
                .toList();

        for (int i = 0; i < Math.min(lack, candidates.size()); i++) {
            TravelPlaceCandidate p = candidates.get(i);
            cluster.getPlaces().add(new ClusterPlace(p, cx, cy));
        }

        long after = cluster.getPlaces().stream()
                .filter(p -> p.getOriginal().getNormalizedCategory().equals(category))
                .count();

        log.info("[Cluster {}] {} 보강 완료 → {}개",
                cluster.getId(), category, after);
    }

    // KMeans index finder
    private int index(double[][] points, double[] target) {
        for (int i = 0; i < points.length; i++) {
            if (points[i][0] == target[0] && points[i][1] == target[1]) {
                return i;
            }
        }
        return -1;
    }

    // fallback clusters
    private ClusterBundle fallbackClusters(
            List<TravelPlaceCandidate> list,
            int k,
            ClusterBundle bundle) {

        for (int i = 1; i <= k; i++) {
            Cluster c = new Cluster();
            c.setId(i);
            bundle.addCluster(c);
        }

        int idx = 0;
        for (TravelPlaceCandidate place : list) {
            Cluster cluster = bundle.getClusters().get(idx % k);
            cluster.getPlaces().add(
                    new ClusterPlace(place, cluster.getCenterLat(), cluster.getCenterLng()));
            idx++;
        }

        return bundle;
    }
}
