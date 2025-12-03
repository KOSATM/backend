package com.example.demo.planner.travel.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.demo.planner.travel.cluster.GeoUtils;
import com.example.demo.planner.travel.dto.DayPlan;
import com.example.demo.planner.travel.dto.response.ClusterResult;
import com.example.demo.planner.travel.dto.response.TravelPlaceSearchResult;
import com.example.demo.planner.travel.utils.CategoryNames;
import com.example.demo.planner.travel.utils.CategoryUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * Day별 장소 할당 전략 담당
 * - 카테고리별 배치
 * - 기타 카테고리 배치
 * - (NEW) noise 기반 보정 로직 포함
 */
@Service
@Slf4j
public class DayPlanAllocator {

    // ==================== 기존 로직 ====================

    public void distributeByCategory(
            List<ClusterInfo> clusterInfos,
            List<DayPlan> dayPlans,
            String category) {

        log.info("▷ {} 분배 시작", category);

        long availableCount = clusterInfos.stream()
                .filter(c -> !c.assigned())
                .mapToLong(c -> c.getCategoryCount(category))
                .sum();

        if (availableCount == 0) {
            log.warn("  {} 카테고리 장소 없음 → 스킵", category);
            return;
        }

        log.info("  {} 카테고리 가용 장소: {}개", category, availableCount);

        for (DayPlan day : dayPlans) {
            int target = day.getTarget().getTarget(category);

            if (target == 0)
                continue;

            if (availableCount < target) {
                log.warn("  Day {} - {} 부족 (목표: {}, 남음: {}) → 가능한 만큼만 배치",
                        day.getDayNumber(), category, target, availableCount);
                target = (int) Math.min(target, availableCount);
            }

            allocateCategoryToDay(clusterInfos, day, category, target);

            availableCount = clusterInfos.stream()
                    .filter(c -> !c.assigned())
                    .mapToLong(c -> c.getCategoryCount(category))
                    .sum();

            if (availableCount == 0)
                break;
        }
    }

    public void distributeOthers(List<ClusterInfo> clusterInfos, List<DayPlan> dayPlans) {
        log.info("▷ 기타 카테고리 분배 시작");

        List<ClusterInfo> unassignedClusters = clusterInfos.stream()
                .filter(c -> !c.assigned())
                .collect(Collectors.toCollection(ArrayList::new));

        if (unassignedClusters.isEmpty()) {
            log.info("  미배정 클러스터 없음");
            return;
        }

        for (ClusterInfo cluster : unassignedClusters) {
            DayPlan targetDay = findMostEmptyDay(dayPlans);

            if (targetDay == null)
                break;

            allocateClusterToDay(targetDay, cluster);
        }
    }

    public List<ClusterInfo> analyzeAll(List<ClusterResult> clusters) {
        return clusters.stream()
                .map(clusterResult -> analyzeCluster(clusterResult.getPlaces()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private void allocateCategoryToDay(
            List<ClusterInfo> clusterInfos,
            DayPlan day,
            String category,
            int target) {

        while (day.getCountByCategory(category) < target) {

            ClusterInfo cluster = findBestCluster(clusterInfos, category);

            if (cluster == null || day.isFull())
                break;

            int current = day.getCountByCategory(category);
            int needed = target - current;

            allocateCategoryPlaces(clusterInfos, day, cluster, category, needed);
        }
    }

    private void allocateCategoryPlaces(
            List<ClusterInfo> clusterInfos,
            DayPlan day,
            ClusterInfo cluster,
            String category,
            int needed) {

        List<TravelPlaceSearchResult> categoryPlaces = CategoryUtils.filterByCategory(cluster.cluster(), category);

        int clusterIndex = clusterInfos.indexOf(cluster);

        if (categoryPlaces.size() <= needed) {

            day.addCluster(ClusterResult.builder()
                    .clusterNumber(day.getClusters().size() + 1)
                    .places(categoryPlaces)
                    .build());

            List<TravelPlaceSearchResult> remaining = CategoryUtils.excludeCategory(cluster.cluster(), category);

            updateClusterInfo(clusterInfos, clusterIndex, remaining);

        } else {

            List<TravelPlaceSearchResult> extracted = categoryPlaces.stream()
                    .limit(needed)
                    .toList();

            day.addCluster(ClusterResult.builder()
                    .clusterNumber(day.getClusters().size() + 1)
                    .places(new ArrayList<>(extracted))
                    .build());

            List<TravelPlaceSearchResult> remaining = new ArrayList<>(cluster.cluster());
            remaining.removeAll(extracted);

            updateClusterInfo(clusterInfos, clusterIndex, remaining);
        }
    }

    private void allocateClusterToDay(DayPlan targetDay, ClusterInfo cluster) {
        int remaining = targetDay.getTarget().getMaxTotal() - targetDay.getTotalCount();
        List<TravelPlaceSearchResult> places = cluster.cluster();

        if (remaining <= 0)
            return;

        List<TravelPlaceSearchResult> sortedByScore = places.stream()
                .sorted(TravelPlaceSearchResult.BY_SCORE_DESC)
                .toList();

        List<TravelPlaceSearchResult> toAdd = sortedByScore.size() <= remaining
                ? sortedByScore
                : sortedByScore.subList(0, remaining);

        targetDay.addCluster(ClusterResult.builder()
                .clusterNumber(targetDay.getClusters().size() + 1)
                .places(new ArrayList<>(toAdd))
                .build());
    }

    private DayPlan findMostEmptyDay(List<DayPlan> dayPlans) {
        return dayPlans.stream()
                .filter(day -> !day.isFull())
                .min(Comparator.comparing(DayPlan::getTotalCount))
                .orElse(null);
    }

    private ClusterInfo findBestCluster(List<ClusterInfo> clusters, String category) {
        return clusters.stream()
                .filter(c -> !c.assigned())
                .filter(c -> c.hasCategory(category))
                .max(Comparator.comparing(c -> c.getCategoryCount(category)))
                .orElse(null);
    }

    private void updateClusterInfo(
            List<ClusterInfo> clusterInfos,
            int index,
            List<TravelPlaceSearchResult> remaining) {

        if (remaining.isEmpty()) {
            clusterInfos.set(index, clusterInfos.get(index).markAsAssigned());
        } else {
            clusterInfos.set(index,
                    new ClusterInfo(remaining,
                            CategoryUtils.countByCategory(remaining),
                            false));
        }
    }

    private ClusterInfo analyzeCluster(List<TravelPlaceSearchResult> cluster) {
        return new ClusterInfo(
                cluster,
                CategoryUtils.countByCategory(cluster),
                false);
    }

    // ==================== NEW: noise 기반 보정 ====================

    /**
     * 노이즈에서 "필수 카테고리"만 골라서 부족분 채우기
     * - categoryTargets 기준으로 DAY별로 FOOD / SPOT 등 필수 카테고리 먼저 맞춰줌
     * - 이 단계에서는 FOOD도 허용 (필수 채우는 용도이므로)
     */
    public void fillRequiredOnlyFromNoise(
            List<DayPlan> dayPlans,
            List<TravelPlaceSearchResult> noise,
            List<String> requiredCategories) {

        log.info("▷ 필수 카테고리 noise 보정 시작");

        for (DayPlan day : dayPlans) {

            for (String category : requiredCategories) {

                int required = day.getTarget().getTarget(category);
                int current = day.getCountByCategory(category);

                if (required == 0 || current >= required)
                    continue;

                int need = required - current;

                while (need > 0 && !day.isFull()) {
                    int idx = findClosestNoiseIndex(day, noise, category);
                    if (idx == -1)
                        break;

                    TravelPlaceSearchResult best = noise.remove(idx);
                    addSinglePlaceCluster(day, best);
                    need--;
                }
            }
        }

        log.info("▷ 필수 카테고리 noise 보정 완료");
    }

    /**
     * 남은 노이즈로 Day의 빈 슬롯 채우기
     * - 이 단계에서는 FOOD는 더 이상 추가하지 않는다.
     * (이미 필수 FOOD는 앞단에서 채운 뒤라고 가정)
     */
    public void fillAnyFromNoise(List<DayPlan> dayPlans,
            List<TravelPlaceSearchResult> noise) {

        log.info("▷ 남은 noise로 빈 슬롯 채우기 (FOOD 제외)");

        for (DayPlan day : dayPlans) {
            while (!day.isFull() && !noise.isEmpty()) {
                int idx = findClosestNoiseIndex(day, noise, null);
                if (idx == -1)
                    break;

                TravelPlaceSearchResult best = noise.remove(idx);
                addSinglePlaceCluster(day, best);
            }
        }
    }

    /**
     * Day의 마지막 장소 기준으로 noise 중 가장 가까운 장소의 index 반환
     * - category가 null이면 전체 noise 대상이지만,
     * 이때는 FOOD 카테고리는 스킵한다. (추가 FOOD 방지)
     * - category가 지정되면 해당 카테고리만 필터링
     * - 후보가 없으면 -1 반환
     */
    private int findClosestNoiseIndex(
            DayPlan day,
            List<TravelPlaceSearchResult> noise,
            String category) {

        if (noise.isEmpty())
            return -1;

        // 1) 후보 필터링
        List<Integer> candidateIndexes = new ArrayList<>();

        for (int i = 0; i < noise.size(); i++) {
            TravelPlaceSearchResult p = noise.get(i);
            String cat = p.getTravelPlaces().getNormalizedCategory();

            if (category != null) {
                // 필수 카테고리 보정 단계: 지정 카테고리만 사용
                if (!category.equals(cat))
                    continue;
            } else {
                // 아무 카테고리나 채우는 단계:
                // FOOD는 더 이상 추가하지 않는다.
                if (CategoryNames.FOOD.equals(cat)) {
                    continue;
                }
            }

            candidateIndexes.add(i);
        }

        if (candidateIndexes.isEmpty())
            return -1;

        // 2) Day 마지막 장소 가져오기
        TravelPlaceSearchResult last = day.getLastPlace();

        // Day가 비어있으면 단순히 noise 첫 번째 후보 반환
        if (last == null)
            return candidateIndexes.get(0);

        double baseLat = last.getTravelPlaces().getLat();
        double baseLng = last.getTravelPlaces().getLng();

        // 3) 가장 가까운 noise 찾기
        int bestIndex = -1;
        double minDist = Double.MAX_VALUE;

        for (int idx : candidateIndexes) {

            TravelPlaceSearchResult p = noise.get(idx);

            double d = GeoUtils.haversine(
                    baseLat,
                    baseLng,
                    p.getTravelPlaces().getLat(),
                    p.getTravelPlaces().getLng());

            if (d < minDist) {
                minDist = d;
                bestIndex = idx;
            }
        }

        return bestIndex;
    }

    /** 단일 장소를 1-place 클러스터로 추가 */
    private void addSinglePlaceCluster(DayPlan day, TravelPlaceSearchResult place) {
        day.addCluster(
                ClusterResult.builder()
                        .clusterNumber(day.getClusters().size() + 1)
                        .places(new ArrayList<>(List.of(place)))
                        .build());
    }

    // ==================== ClusterInfo (기존) ====================

    public record ClusterInfo(
            List<TravelPlaceSearchResult> cluster,
            Map<String, Long> categoryCount,
            boolean assigned) {

        public boolean hasCategory(String category) {
            return categoryCount.getOrDefault(category, 0L) > 0;
        }

        public long getCategoryCount(String category) {
            return categoryCount.getOrDefault(category, 0L);
        }

        public ClusterInfo markAsAssigned() {
            return new ClusterInfo(cluster, categoryCount, true);
        }
    }
}
