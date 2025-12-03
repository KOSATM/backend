package com.example.demo.planner.travel.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.demo.planner.travel.dto.response.ClusterResult;
import com.example.demo.planner.travel.dto.response.TravelPlaceSearchResult;

import lombok.extern.slf4j.Slf4j;

/**
 * 클러스터 전처리 담당
 * - 정렬 (점수, 거리)
 * - 필터링
 */
@Service
@Slf4j
public class ClusterProcessor {

    private static final double MIN_AVG_SCORE = 0.3;

    /**
     * 점수 기준 내림차순 정렬
     */
    public void sortByScore(List<ClusterResult> clusters) {
        for (ClusterResult cluster : clusters) {
            cluster.getPlaces().sort(TravelPlaceSearchResult.BY_SCORE_DESC);
        }
    }

    /**
     * 평균 점수 기준 필터링
     */
    public List<ClusterResult> filter(List<ClusterResult> clusters) {
        if (clusters.isEmpty()) {
            log.warn("입력 클러스터 없음");
            return clusters;
        }

        List<ClusterResult> filtered = clusters.stream()
                .filter(cluster -> {
                    double avgScore = TravelPlaceSearchResult.calculateAverageScore(cluster.getPlaces());
                    return avgScore >= MIN_AVG_SCORE;
                })
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            log.warn("모든 클러스터가 기준({}) 미달 → 원본 사용", MIN_AVG_SCORE);
            return clusters;
        }

        log.info("필터링: {}개 → {}개 (제거: {}개)",
                clusters.size(), filtered.size(), clusters.size() - filtered.size());

        return filtered;
    }

    /**
     * 거리 기준 정렬 (Nearest Neighbor)
     */
    public void sortByDistance(List<ClusterResult> clusters) {
        for (ClusterResult clusterResult : clusters) {
            List<TravelPlaceSearchResult> cluster = clusterResult.getPlaces();

            if (cluster.isEmpty())
                continue;

            List<TravelPlaceSearchResult> sorted = new ArrayList<>();
            List<TravelPlaceSearchResult> remaining = new ArrayList<>(cluster);

            // 첫 번째 장소 추가
            sorted.add(remaining.remove(0));

            // 가장 가까운 장소를 순서대로 추가
            while (!remaining.isEmpty()) {
                TravelPlaceSearchResult current = sorted.get(sorted.size() - 1);
                TravelPlaceSearchResult nearest = findNearest(current, remaining);
                sorted.add(nearest);
                remaining.remove(nearest);
            }

            cluster.clear();
            cluster.addAll(sorted);
        }
    }

    /**
     * 현재 위치에서 가장 가까운 장소 찾기
     */
    private TravelPlaceSearchResult findNearest(
            TravelPlaceSearchResult current,
            List<TravelPlaceSearchResult> candidates) {

        return candidates.stream()
                .min(Comparator.comparingDouble(current::distanceTo))
                .orElse(candidates.get(0));
    }

}