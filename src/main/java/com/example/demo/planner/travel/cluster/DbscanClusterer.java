package com.example.demo.planner.travel.cluster;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.example.demo.planner.travel.dto.response.ClusterResult;
import com.example.demo.planner.travel.dto.response.TravelPlaceSearchResult;

public class DbscanClusterer {

    // eps: 이 거리(m) 안에 있으면 "이웃"으로 본다 (예: 800m)
    private final double epsMeters;
    // minPts: 최소 몇 개가 뭉쳐 있어야 "클러스터"로 인정할지 (예: 3)
    private final int minPts;

    public DbscanClusterer(double epsMeters, int minPts) {
        this.epsMeters = epsMeters;
        this.minPts = minPts;
    }

    /**
     * DBSCAN 클러스터링 실행
     *
     * @param points 벡터 검색 결과 리스트
     * @return 클러스터 리스트 (ClusterResult로 반환)
     */
    public ClusterBundle cluster(List<TravelPlaceSearchResult> points) {
        int n = points.size();

        // 0: 미분류, -1: 노이즈, 1~: 클러스터 ID
        int[] labels = new int[n];

        int clusterId = 0;
        List<ClusterResult> clusters = new ArrayList<>();
        List<TravelPlaceSearchResult> noise = new ArrayList<>();

        for (int i = 0; i < n; i++) {

            // 이미 방문된 포인트라면 스킵
            if (labels[i] != 0) {
                continue;
            }

            // 현재 포인트 기준 eps 이웃 찾기
            List<Integer> neighbors = regionQuery(points, i);

            // minPts 미만이면 노이즈 처리
            if (neighbors.size() < minPts) {
                labels[i] = -1;
                noise.add(points.get(i));
                continue;
            }

            // 새로운 클러스터 생성
            clusterId++;
            List<TravelPlaceSearchResult> clusterPoints = new ArrayList<>();

            // 클러스터 확장
            expandCluster(points, labels, i, neighbors, clusterId, clusterPoints, noise);

            // ClusterResult로 추가
            clusters.add(ClusterResult.builder()
                    .clusterNumber(clusterId)
                    .places(clusterPoints)
                    .build());
        }

        // ClusterBundle에 담아서 반환
        return new ClusterBundle(clusters, noise);
    }

    /**
     * 특정 포인트 index에 대해 eps 이내에 있는 이웃 포인트 index 리스트 반환
     */
    private List<Integer> regionQuery(List<TravelPlaceSearchResult> points, int index) {
        List<Integer> neighbors = new ArrayList<>();

        TravelPlaceSearchResult base = points.get(index);
        double baseLat = base.getTravelPlaces().getLat();
        double baseLng = base.getTravelPlaces().getLng();

        for (int i = 0; i < points.size(); i++) {
            TravelPlaceSearchResult other = points.get(i);

            double lat = other.getTravelPlaces().getLat();
            double lng = other.getTravelPlaces().getLng();

            double distance = GeoUtils.haversine(baseLat, baseLng, lat, lng);

            if (distance <= epsMeters) {
                neighbors.add(i);
            }
        }

        return neighbors;
    }

    /**
     * DBSCAN의 핵심: seed point 주변에서 클러스터를 확장하는 로직
     */
    private void expandCluster(List<TravelPlaceSearchResult> points, int[] labels, int pointIndex,
            List<Integer> neighbors, int clusterId, List<TravelPlaceSearchResult> cluster,
            List<TravelPlaceSearchResult> noise) {

        labels[pointIndex] = clusterId;
        TravelPlaceSearchResult base = points.get(pointIndex);
        cluster.add(base);

        // noise에서 제거 (주석 유지)
        noise.remove(base);

        // neighbors를 큐처럼 사용하면서 계속 확장
        Set<Integer> neighborSet = new HashSet<>(neighbors);

        while (!neighborSet.isEmpty()) {
            // 하나 꺼내기
            Integer currentIndex = neighborSet.iterator().next();
            neighborSet.remove(currentIndex);

            TravelPlaceSearchResult currentPoint = points.get(currentIndex);

            if (labels[currentIndex] == -1) {
                // 이전에 노이즈로 분류됐던 포인트면 -> 클러스터에 포함시켜버림
                labels[currentIndex] = clusterId;
                cluster.add(currentPoint);

                // noise에서 제거 (주석 유지)
                noise.remove(currentPoint);
            }

            if (labels[currentIndex] != 0) {
                // 이미 클러스터에 속했으면 스킵
                continue;
            }

            // 아직 미분류였으면 클러스터에 포함
            labels[currentIndex] = clusterId;
            cluster.add(currentPoint);

            // noise에서 제거
            noise.remove(currentPoint);

            // 해당 포인트 기준으로도 이웃을 다시 탐색
            List<Integer> currentNeighbors = regionQuery(points, currentIndex);
            if (currentNeighbors.size() >= minPts) {
                // 이웃이 많으면(= Core point) 이웃들을 확장 후보에 추가
                neighborSet.addAll(currentNeighbors);
            }
        }
    }

}