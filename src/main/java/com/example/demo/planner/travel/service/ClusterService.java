package com.example.demo.planner.travel.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.planner.travel.dto.DayPlan;
import com.example.demo.planner.travel.dto.DayTarget;
import com.example.demo.planner.travel.dto.response.ClusterResult;
import com.example.demo.planner.travel.dto.response.DayPlanResult;
import com.example.demo.planner.travel.dto.response.TravelPlaceSearchResult;
import com.example.demo.planner.travel.service.DayPlanAllocator.ClusterInfo;
import com.example.demo.planner.travel.strategy.DayPlanStrategy;
import com.example.demo.planner.travel.utils.CategoryNames;

import lombok.extern.slf4j.Slf4j;

/**
 * 클러스터 서비스 (파사드)
 * - 전체 흐름 조율
 * - 다른 서비스 조합
 */
@Service
@Slf4j
public class ClusterService {

    private final DayPlanStrategy dayPlanStrategy;
    private final ClusterProcessor processor;
    private final DayPlanAllocator allocator;

    public ClusterService(
            DayPlanStrategy dayPlanStrategy,
            ClusterProcessor processor,
            DayPlanAllocator allocator) {
        this.dayPlanStrategy = dayPlanStrategy;
        this.processor = processor;
        this.allocator = allocator;
    }

    /**
     * 클러스터 점수 정렬
     */
    public void sortByScore(List<ClusterResult> clusters) {
        processor.sortByScore(clusters);
    }

    /**
     * 클러스터 필터링
     */
    public List<ClusterResult> filter(List<ClusterResult> clusters) {
        return processor.filter(clusters);
    }

    /**
     * 클러스터를 Day별로 분할
     * - 전체 프로세스 조율
     *
     * 최종 플로우:
     *  1) 클러스터에서 필수 카테고리(예: FOOD, SPOT) 우선 배치
     *  2) 남은 클러스터를 이용해 기타 카테고리 배치
     *  3) 노이즈에서 필수 카테고리 부족분만 거리 기준으로 보정
     *  4) 남은 노이즈로 Day의 빈 슬롯 채우기 (FOOD는 더 이상 추가하지 않음)
     */
    public List<DayPlanResult> splitIntoDays(
            List<ClusterResult> clusters,
            List<TravelPlaceSearchResult> noise,
            int duration) {

        log.info("▷▷ Day 분할 시작: {}개 클러스터 → {}일", clusters.size(), duration);

        // 1. Day별 목표 생성
        List<DayTarget> dayTargets = dayPlanStrategy.createDayTargets(duration);

        // 2. DayPlan 객체 초기화
        List<DayPlan> dayPlans = createDayPlans(duration, dayTargets);

        // 3. 클러스터 분석 (정상 클러스터만)
        List<ClusterInfo> clusterInfos = allocator.analyzeAll(clusters);

        // 4. 필수 카테고리 배치 (클러스터 기반, 우선순위 순서대로)
        for (String category : CategoryNames.REQUIRED) {
            allocator.distributeByCategory(clusterInfos, dayPlans, category);
        }

        // 5. 기타 카테고리 배치 (클러스터에서 남은 것들)
        allocator.distributeOthers(clusterInfos, dayPlans);

        // 6. noise 리스트 준비 (mutable)
        List<TravelPlaceSearchResult> noiseList = new ArrayList<>();
        if (noise != null && !noise.isEmpty()) {
            noiseList.addAll(noise);
        }

        // 7. noise 기반 필수 카테고리(FOOD, SPOT 등) 부족분 보정
        allocator.fillRequiredOnlyFromNoise(dayPlans, noiseList, CategoryNames.REQUIRED);

        // 8. 남은 noise로 Day의 빈 슬롯 채우기 (여기서는 FOOD는 더 이상 추가하지 않음)
        allocator.fillAnyFromNoise(dayPlans, noiseList);

        // 9. 결과 로깅
        logDayPlanSummary(dayPlans);

        // 10. DTO 변환
        return convertToDayPlanResults(dayPlans);
    }

    // ==================== Private 메서드 ====================

    /**
     * DayPlan 객체 생성
     */
    private List<DayPlan> createDayPlans(int duration, List<DayTarget> dayTargets) {
        List<DayPlan> dayPlans = new ArrayList<>();
        for (int i = 0; i < duration; i++) {
            dayPlans.add(new DayPlan(i + 1, dayTargets.get(i)));
        }
        return dayPlans;
    }

    /**
     * DayPlan → DayPlanResult 변환
     */
    private List<DayPlanResult> convertToDayPlanResults(List<DayPlan> dayPlans) {
        return dayPlans.stream()
                .map(this::convertSingleDay)
                .toList();
    }

    /**
     * 단일 DayPlan 변환
     */
    private DayPlanResult convertSingleDay(DayPlan dayPlan) {
        return DayPlanResult.builder()
                .dayNumber(dayPlan.getDayNumber())
                .clusters(dayPlan.getClusters())
                .build();
    }

    /**
     * Day별 요약 로그
     */
    private void logDayPlanSummary(List<DayPlan> dayPlans) {
        for (DayPlan day : dayPlans) {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Day %d: ", day.getDayNumber()));

            // 모든 필수 카테고리 출력
            for (String category : CategoryNames.REQUIRED) {
                sb.append(String.format("%s=%d, ", category, day.getCountByCategory(category)));
            }

            // 기타 카테고리 출력
            sb.append(String.format("ETC=%d, 총 %d개",
                    day.getCountByCategory(CategoryNames.ETC),
                    day.getTotalCount()));

            log.info(sb.toString());
        }
    }

    /**
     * 클러스터 거리 정렬
     */
    public void sortByDistance(List<ClusterResult> clusters) {
        processor.sortByDistance(clusters);
    }
}
