package com.example.demo.planner.travel.service.allocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.demo.planner.travel.cluster.GeoUtils;
import com.example.demo.planner.travel.dto.DayPlan;
import com.example.demo.planner.travel.dto.DayTarget;
import com.example.demo.planner.travel.dto.response.ClusterResult;
import com.example.demo.planner.travel.dto.response.TravelPlaceSearchResult;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DayPlanFillService {

    /** DayPlan 생성 후 부족한 카테고리 채움 */
    public void fillMissingFromLeftovers(
            List<DayPlan> dayPlans,
            Map<String, List<TravelPlaceSearchResult>> categorized) {

        log.info("▷ 부족한 필수 카테고리 보정 시작");

        Set<Long> used = dayPlans.stream()
                .flatMap(d -> d.getClusters().stream())
                .flatMap(c -> c.getPlaces().stream())
                .map(p -> p.getTravelPlaces().getId())
                .collect(Collectors.toSet());

        Map<String, List<TravelPlaceSearchResult>> leftovers = new HashMap<>();
        categorized.forEach((cat, list) -> {
            leftovers.put(cat, list.stream()
                    .filter(p -> !used.contains(p.getTravelPlaces().getId()))
                    .sorted(TravelPlaceSearchResult.BY_SCORE_DESC)
                    .toList());
        });

        for (DayPlan day : dayPlans) {

            DayTarget target = day.getTarget();
            Map<String, Integer> targetMap = target.getTargets();

            for (Map.Entry<String, Integer> entry : targetMap.entrySet()) {

                String category = entry.getKey();
                int need = entry.getValue() - day.getCountByCategory(category);
                if (need <= 0)
                    continue;

                List<TravelPlaceSearchResult> candidates = new ArrayList<>(leftovers.getOrDefault(category, List.of()));

                while (need > 0 && !day.isFull() && !candidates.isEmpty()) {

                    TravelPlaceSearchResult best = findClosestPlace(day, candidates);

                    addSinglePlaceCluster(day, best);

                    used.add(best.getTravelPlaces().getId());
                    candidates.remove(best);
                    need--;
                }

                leftovers.put(category, candidates);
            }
        }

        log.info("▷ 부족한 카테고리 보정 완료");
    }

    private TravelPlaceSearchResult findClosestPlace(
            DayPlan day,
            List<TravelPlaceSearchResult> candidates) {

        TravelPlaceSearchResult last = day.getLastPlace();
        if (last == null)
            return candidates.get(0);

        return candidates.stream()
                .min(Comparator.comparingDouble(c -> GeoUtils.haversine(
                        last.getTravelPlaces().getLat(),
                        last.getTravelPlaces().getLng(),
                        c.getTravelPlaces().getLat(),
                        c.getTravelPlaces().getLng())))
                .orElse(null);
    }

    private void addSinglePlaceCluster(DayPlan day, TravelPlaceSearchResult place) {
        day.addCluster(
                ClusterResult.builder()
                        .clusterNumber(day.getClusters().size() + 1)
                        .places(new ArrayList<>(List.of(place)))
                        .build());
    }
}
