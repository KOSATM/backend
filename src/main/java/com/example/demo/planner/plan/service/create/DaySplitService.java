package com.example.demo.planner.plan.service.create;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.example.demo.planner.plan.dto.Cluster;
import com.example.demo.planner.plan.dto.ClusterBundle;
import com.example.demo.planner.plan.dto.ClusterPlace;
import com.example.demo.planner.plan.dto.TravelPlaceCandidate;
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

    /**
     * @param bundle           KMeans 등으로 만들어진 클러스터 묶음
     * @param duration         전체 여행 일수
     * @param strategy         날짜별 최소 카테고리 요구량 전략
     * @param globalCandidates 전역 후보 (CategoryFill 후 병합 리스트)
     */
    public List<DayPlanResult> split(ClusterBundle bundle,
            int duration,
            TravelPlanStrategy strategy,
            List<TravelPlaceCandidate> globalCandidates) {

        log.info("=== [DaySplit] 일정 생성 시작 ===");

        List<Cluster> clusters = bundle.getClusters();
        List<DayPlanResult> results = new ArrayList<>();

        // 전체 일정 중복 방지용
        Set<Long> usedIds = new HashSet<>();

        int day = 1;

        for (Cluster cluster : clusters) {

            DayRequirement req = strategy.getDayRequirement(day, duration);

            DayPlanResult dayPlan = makeDayPlan(
                    cluster,
                    req,
                    day,
                    globalCandidates,
                    usedIds);

            results.add(dayPlan);

            day++;
            if (day > duration)
                break;
        }

        // 한 번 더 검증용 로그
        Set<Long> check = new HashSet<>();
        for (DayPlanResult d : results) {
            for (ClusterPlace cp : d.getPlaces()) {
                if (!check.add(cp.getOriginal().getId())) {
                    log.warn("DUPLICATE DETECTED id={} in day={}", cp.getOriginal().getId(), d.getDayNumber());
                }
            }
        }

        log.info("=== [DaySplit] 완료: {}일 일정 생성 ===", results.size());
        return results;
    }

    private DayPlanResult makeDayPlan(Cluster cluster,
            DayRequirement req,
            int dayNumber,
            List<TravelPlaceCandidate> globalCandidates,
            Set<Long> usedIds) {

        log.info("---- Day {} 일정 생성 ----", dayNumber);

        DayPlanResult result = new DayPlanResult();
        result.setDayNumber(dayNumber);

        // 클러스터 내 장소 복사
        List<ClusterPlace> source = new ArrayList<>(cluster.getPlaces());

        // 이미 다른 날에 사용한 장소는 여기서 제거
        source.removeIf(cp -> usedIds.contains(cp.getOriginal().getId()));

        double cx = cluster.getCenterLat();
        double cy = cluster.getCenterLng();

        // 1) SPOT (부족하면 전역에서 보강)
        addCategoryWithFallback(
                result,
                source,
                CategoryNames.SPOT,
                req.getMinSpot(),
                globalCandidates,
                usedIds,
                cx,
                cy);

        // 2) FOOD (부족하면 전역에서 보강)
        addCategoryWithFallback(
                result,
                source,
                CategoryNames.FOOD,
                req.getMinFood(),
                globalCandidates,
                usedIds,
                cx,
                cy);

        // FOOD는 이후 절대 추가되지 않도록 source에서 제거
        source.removeIf(p -> p.getOriginal().getNormalizedCategory().equals(CategoryNames.FOOD));

        // 3) OPTIONAL (기존 로직 유지 – source 안에서만 선택)
        addOptional(source, result, req.getMinOptional());

        // 4) 남은 장소 중 일부 추가 (최대 개수까지 채우기)
        while (!source.isEmpty() && result.getPlaces().size() < req.getMaxPlaces()) {
            ClusterPlace next = source.remove(0);
            result.getPlaces().add(next);
            usedIds.add(next.getOriginal().getId()); // 전역 중복 방지
        }

        // 5) 동선 정렬
        sortByDistance(result);

        return result;
    }

    /**
     * 클러스터 내에서 먼저 채우고, 부족하면 전역 globalCandidates에서 보강하는 버전
     */
    private void addCategoryWithFallback(DayPlanResult result,
            List<ClusterPlace> source,
            String category,
            int requiredCount,
            List<TravelPlaceCandidate> globalCandidates,
            Set<Long> usedIds,
            double centerLat,
            double centerLng) {

        if (requiredCount <= 0)
            return;

        List<ClusterPlace> selected = new ArrayList<>();

        // 1) 클러스터 내에서 먼저 채우기
        for (ClusterPlace cp : new ArrayList<>(source)) {
            if (selected.size() >= requiredCount)
                break;

            if (!category.equals(cp.getOriginal().getNormalizedCategory()))
                continue;
            if (usedIds.contains(cp.getOriginal().getId()))
                continue;

            selected.add(cp);
            source.remove(cp);
            usedIds.add(cp.getOriginal().getId());
        }

        int have = selected.size();
        if (have < requiredCount) {
            int need = requiredCount - have;
            log.warn("카테고리 {} 필요 {}개 중 {}개만 클러스터에서 확보 → {}개 전역 보강 시도",
                    category, requiredCount, have, need);

            List<ClusterPlace> fallback = searchGlobalFallback(
                    category,
                    need,
                    globalCandidates,
                    usedIds,
                    centerLat,
                    centerLng);

            for (ClusterPlace cp : fallback) {
                selected.add(cp);
                usedIds.add(cp.getOriginal().getId());

                // 핵심 수정: fallback으로 들어온 장소는 source(남은 장소)에서도 제거
                source.removeIf(s -> s.getOriginal().getId().equals(cp.getOriginal().getId()));
            }

            log.info("카테고리 {} 전역 보강 결과 → 총 {}개 확보", category, selected.size());
        }

        result.getPlaces().addAll(selected);
    }

    /**
     * 전역 후보(globalCandidates)에서 아직 사용되지 않은 같은 카테고리 장소를
     * 클러스터 중심과의 거리 기준으로 가까운 순서대로 가져옴.
     */
    private List<ClusterPlace> searchGlobalFallback(String category,
            int need,
            List<TravelPlaceCandidate> globalCandidates,
            Set<Long> usedIds,
            double centerLat,
            double centerLng) {

        List<ClusterPlace> result = new ArrayList<>();

        globalCandidates.stream()
                .filter(c -> category.equals(c.getNormalizedCategory()))
                .filter(c -> !usedIds.contains(c.getTravelPlaces().getId()))
                .sorted((a, b) -> {
                    double da = GeoUtils.haversine(
                            centerLat, centerLng,
                            a.getTravelPlacesLat(), a.getTravelPlacesLng());
                    double db = GeoUtils.haversine(
                            centerLat, centerLng,
                            b.getTravelPlacesLat(), b.getTravelPlacesLng());
                    return Double.compare(da, db);
                })
                .limit(need)
                .forEach(c -> {
                    ClusterPlace cp = new ClusterPlace(c, centerLat, centerLng);
                    result.add(cp);
                });

        if (result.size() < need) {
            log.warn("전역 보강에서도 카테고리 {} {}개만 확보 (요청: {})",
                    category, result.size(), need);
        }

        return result;
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
