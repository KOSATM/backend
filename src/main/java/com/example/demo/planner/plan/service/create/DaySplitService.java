package com.example.demo.planner.plan.service.create;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.planner.plan.dto.Cluster;
import com.example.demo.planner.plan.dto.ClusterBundle;
import com.example.demo.planner.plan.dto.ClusterPlace;
import com.example.demo.planner.plan.dto.response.DayPlanResult;
import com.example.demo.planner.plan.strategy.DayRequirement;
import com.example.demo.planner.plan.strategy.TravelPlanStrategy;
import com.example.demo.planner.plan.utils.CategoryNames;
import com.example.demo.planner.plan.utils.GeoUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class DaySplitService {

    public List<DayPlanResult> split(ClusterBundle bundle,
            int duration,
            TravelPlanStrategy strategy) {

        log.info("=== [DaySplit] 일정 생성 시작 ===");

        List<Cluster> clusters = bundle.getClusters();
        List<DayPlanResult> results = new ArrayList<>();

        int day = 1;

        for (Cluster cluster : clusters) {

            DayRequirement req = strategy.getDayRequirement(day, duration);

            DayPlanResult dayPlan = makeDayPlan(cluster, req, day);

            results.add(dayPlan);

            day++;

            if (day > duration)
                break;
        }

        log.info("=== [DaySplit] 완료: {}일 일정 생성 ===", results.size());
        return results;
    }

    private DayPlanResult makeDayPlan(Cluster cluster, DayRequirement req, int dayNumber) {

        log.info("---- Day {} 일정 생성 ----", dayNumber);

        DayPlanResult result = new DayPlanResult();
        result.setDayNumber(dayNumber);

        List<ClusterPlace> source = new ArrayList<>(cluster.getPlaces());

        // 1) SPOT
        addCategory(source, result, CategoryNames.SPOT, req.getMinSpot());

        // 2) FOOD
        addCategory(source, result, CategoryNames.FOOD, req.getMinFood());

        // FOOD는 이후 절대 추가되지 않도록 source에서 제거
        source.removeIf(p -> p.getOriginal().getNormalizedCategory().equals(CategoryNames.FOOD));

        // 3) OPTIONAL
        addOptional(source, result, req.getMinOptional());

        // 4) 남은 장소 중 일부 추가
        while (!source.isEmpty() && result.getPlaces().size() < req.getMaxPlaces()) {
            result.getPlaces().add(source.remove(0));
        }

        // 5) 동선 정렬
        sortByDistance(result);

        return result;
    }

    private void addCategory(List<ClusterPlace> source,
            DayPlanResult result,
            String category,
            int count) {

        if (count <= 0)
            return;

        List<ClusterPlace> matched = source.stream()
                .filter(p -> p.getOriginal().getNormalizedCategory().equals(category))
                .toList();

        int take = Math.min(count, matched.size());

        for (int i = 0; i < take; i++) {
            result.getPlaces().add(matched.get(i));
            source.remove(matched.get(i));
        }

        if (take < count) {
            log.warn("카테고리 {} 필요 {}개 중 {}개만 확보됨", category, count, take);
        }
    }

    private void addOptional(List<ClusterPlace> source,
            DayPlanResult result,
            int count) {

        if (count <= 0)
            return;

        for (String cat : CategoryNames.OPTIONAL) {

            List<ClusterPlace> matched = source.stream()
                    .filter(p -> p.getOriginal().getNormalizedCategory().equals(cat))
                    .toList();

            if (!matched.isEmpty()) {
                result.getPlaces().add(matched.get(0));
                source.remove(matched.get(0));
                return;
            }
        }

        log.warn("[Optional] 선택 카테고리를 찾지 못함");
    }

    private void sortByDistance(DayPlanResult result) {

        if (result.getPlaces().size() <= 2)
            return;

        List<ClusterPlace> sorted = new ArrayList<>();
        ClusterPlace cur = result.getPlaces().get(0);

        sorted.add(cur);

        List<ClusterPlace> remain = new ArrayList<>(result.getPlaces());
        remain.remove(cur);

        while (!remain.isEmpty()) {
            ClusterPlace next = remain.get(0);
            double bestDist = dist(cur, next);

            for (ClusterPlace p : remain) {
                double d = dist(cur, p);
                if (d < bestDist) {
                    bestDist = d;
                    next = p;
                }
            }

            sorted.add(next);
            remain.remove(next);
            cur = next;
        }

        result.setPlaces(sorted);
    }

    private double dist(ClusterPlace a, ClusterPlace b) {
        return GeoUtils.haversine(
                a.getLat(), a.getLng(),
                b.getLat(), b.getLng());
    }
}
