package com.example.demo.planner.travel.service.allocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.planner.travel.dto.DayPlan;
import com.example.demo.planner.travel.dto.response.ClusterResult;
import com.example.demo.planner.travel.dto.response.TravelPlaceSearchResult;
import com.example.demo.planner.travel.service.DayPlanAllocator.ClusterInfo;
import com.example.demo.planner.travel.utils.CategoryUtils;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DayPlanAllocationService {

    /** 필수 카테고리 배치 */
    public void distributeCategory(
            List<ClusterInfo> clusterInfos,
            List<DayPlan> dayPlans,
            String category) {

        long available = clusterInfos.stream()
                .filter(c -> !c.assigned())
                .mapToLong(c -> c.getCategoryCount(category))
                .sum();

        for (DayPlan day : dayPlans) {
            int target = day.getTarget().getTarget(category);
            if (target == 0) continue;

            if (available < target) target = (int) available;

            allocateCategoryToDay(clusterInfos, day, category, target);

            available = clusterInfos.stream()
                    .filter(c -> !c.assigned())
                    .mapToLong(c -> c.getCategoryCount(category))
                    .sum();

            if (available == 0) break;
        }
    }

    /** 기타 배치 */
    public void distributeOthers(List<ClusterInfo> clusterInfos, List<DayPlan> dayPlans) {

        List<ClusterInfo> unassigned = clusterInfos.stream()
                .filter(c -> !c.assigned())
                .toList();

        for (ClusterInfo c : unassigned) {
            DayPlan day = findMostEmptyDay(dayPlans);
            if (day == null) break;
            allocateClusterToDay(day, c);
        }
    }

    // ------------------------- 내부 메서드 -------------------------

    private void allocateCategoryToDay(
            List<ClusterInfo> infos, DayPlan day,
            String category, int need) {

        while (day.getCountByCategory(category) < need) {

            ClusterInfo cluster = findBestCluster(infos, category);
            if (cluster == null || day.isFull()) break;

            int remain = need - day.getCountByCategory(category);
            extractCategoryPlaces(infos, day, cluster, category, remain);
        }
    }

    private void extractCategoryPlaces(
            List<ClusterInfo> infos, DayPlan day,
            ClusterInfo cluster, String category, int need) {

        List<TravelPlaceSearchResult> categoryPlaces =
                CategoryUtils.filterByCategory(cluster.cluster(), category);

        int idx = infos.indexOf(cluster);

        if (categoryPlaces.size() <= need) {

            day.addCluster(ClusterResult.builder()
                    .clusterNumber(day.getClusters().size() + 1)
                    .places(categoryPlaces)
                    .build());

            List<TravelPlaceSearchResult> left =
                    CategoryUtils.excludeCategory(cluster.cluster(), category);

            updateCluster(infos, idx, left);

        } else {
            List<TravelPlaceSearchResult> pick = categoryPlaces.stream()
                    .limit(need)
                    .toList();

            day.addCluster(ClusterResult.builder()
                    .clusterNumber(day.getClusters().size() + 1)
                    .places(new ArrayList<>(pick))
                    .build());

            List<TravelPlaceSearchResult> left = new ArrayList<>(cluster.cluster());
            left.removeAll(pick);

            updateCluster(infos, idx, left);
        }
    }

    private void allocateClusterToDay(DayPlan day, ClusterInfo cluster) {
        int remain = day.getTarget().getMaxTotal() - day.getTotalCount();
        if (remain <= 0) return;

        List<TravelPlaceSearchResult> sorted =
                cluster.cluster().stream()
                        .sorted(TravelPlaceSearchResult.BY_SCORE_DESC)
                        .toList();

        List<TravelPlaceSearchResult> pick =
                sorted.size() <= remain ? sorted : sorted.subList(0, remain);

        day.addCluster(
                ClusterResult.builder()
                        .clusterNumber(day.getClusters().size() + 1)
                        .places(new ArrayList<>(pick))
                        .build()
        );
    }

    private void updateCluster(List<ClusterInfo> infos, int idx, List<TravelPlaceSearchResult> left) {
        if (left.isEmpty()) {
            infos.set(idx, infos.get(idx).markAsAssigned());
        } else {
            infos.set(idx,
                    new ClusterInfo(left,
                            CategoryUtils.countByCategory(left),
                            false));
        }
    }

    private ClusterInfo findBestCluster(List<ClusterInfo> infos, String category) {
        return infos.stream()
                .filter(c -> !c.assigned())
                .filter(c -> c.hasCategory(category))
                .max(Comparator.comparing(c -> c.getCategoryCount(category)))
                .orElse(null);
    }

    private DayPlan findMostEmptyDay(List<DayPlan> plans) {
        return plans.stream()
                .filter(d -> !d.isFull())
                .min(Comparator.comparing(DayPlan::getTotalCount))
                .orElse(null);
    }
}
