package com.example.demo.planner.plan.service.create;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

import com.example.demo.common.chat.intent.dto.IntentCommand;
import com.example.demo.common.chat.pipeline.AiAgentResponse;
import com.example.demo.common.global.agent.AiAgent;
import com.example.demo.planner.plan.agent.DurationNormalizerAgent;
import com.example.demo.planner.plan.agent.SeedQueryAgent;
import com.example.demo.planner.plan.dao.PlanDao;
import com.example.demo.planner.plan.dto.Cluster;
import com.example.demo.planner.plan.dto.ClusterBundle;
import com.example.demo.planner.plan.dto.ClusterPlace;
import com.example.demo.planner.plan.dto.TravelPlaceCandidate;
import com.example.demo.planner.plan.dto.response.DayPlanResult;
import com.example.demo.planner.travel.strategy.StandardTravelStrategy;
import com.example.demo.planner.travel.strategy.TravelPlanStrategy;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class TravelPlannerService implements AiAgent {

    private final EmbeddingModel embeddingModel;
    private final SeedQueryAgent seedQueryAgent;
    private final DurationNormalizerAgent durationNormalizerAgent;
    private final PlanDao planDao;
    private final KMeansClusterService kMeansClusterService;
    private final CategoryFillService categoryFillService;
    private final DaySplitService daySplitService;
    private final RegionService regionService;

    public TravelPlannerService(EmbeddingModel embeddingModel, SeedQueryAgent seedQueryAgent,
            DurationNormalizerAgent durationNormalizerAgent, PlanDao planDao,
            KMeansClusterService kMeansClusterService,
            CategoryFillService categoryFillService, DaySplitService daySplitService, RegionService regionService) {
        this.embeddingModel = embeddingModel;
        this.seedQueryAgent = seedQueryAgent;
        this.durationNormalizerAgent = durationNormalizerAgent;
        this.planDao = planDao;
        this.kMeansClusterService = kMeansClusterService;
        this.categoryFillService = categoryFillService;
        this.daySplitService = daySplitService;
        this.regionService = regionService;
    }

    @Override
    public AiAgentResponse execute(IntentCommand command) {
        log.info("▷▷ 1. TravelPlannerAgent 시작");

        Map<String, Object> arguments = command.getArguments();

        // 전략 선택
        TravelPlanStrategy strategy = selectStrategy(arguments);
        log.info("  선택된 전략: {}", strategy.getClass().getSimpleName());

        // Duration 정규화
        log.info("▷▷ 2. Duration 정규화");
        int duration = normalizeDuration(arguments);
        String location = (String) arguments.getOrDefault("location", "서울");

        int minSpot = strategy.getTotalMinSpot(duration);
        int minFood = strategy.getTotalMinFood(duration);
        log.info("  필요량 - FOOD: {}개, SPOT: {}개", minFood, minSpot);

        // Seed Query 생성
        log.info("▷▷ 3. SeedQuery 생성");
        String seedQuery = seedQueryAgent.generateSeedQuery(arguments);
        log.info("  SeedQuery: {}", seedQuery);

        // 벡터 검색
        log.info("▷▷ 4. 벡터 검색 & 셔플");
        float[] embedding = embeddingModel.embed(seedQuery);
        List<TravelPlaceCandidate> candidates = searchAndShuffle(embedding);
        log.info("  초기 후보: {}개", candidates.size());

        // 지역 필터링
        log.info("▷▷ 5. 지역 필터링");
        List<TravelPlaceCandidate> filtered = regionService
                .applyRegionPreference(candidates, location, duration);
        log.info("  필터 후: {}개 ({}개 제거)",
                filtered.size(), candidates.size() - filtered.size());

        // 카테고리 보강
        log.info("▷▷ 6. 카테고리 보강");
        Map<String, List<TravelPlaceCandidate>> categoryMap = categoryFillService.fill(filtered, minFood, minSpot);

        // 병합
        log.info("▷▷ 7. 카테고리 병합");
        List<TravelPlaceCandidate> merged = categoryFillService.merge(categoryMap);
        log.info("  병합 결과: {}개", merged.size());

        // 클러스터링
        log.info("▷▷ 8. KMeans 클러스터링");
        ClusterBundle clusters = kMeansClusterService.cluster(merged, duration);
        logClusterResults(clusters);
        logClusterInfoResult(clusters);

        // Day 분할
        log.info("▷▷ 9. 일정 분배");
        List<DayPlanResult> dayPlans = daySplitService.split(clusters, duration, strategy);
        // logDayPlans(dayPlans);

        log.info("▷▷ 10. 최종 일정 반환");

        log.info("▷▷ 11. TravelPlannerAgent 완료");

        printDayPlans(dayPlans);

        return AiAgentResponse.of(buildResponse(dayPlans));

    }

    // ==================== Private 메서드 ====================

    private TravelPlanStrategy selectStrategy(Map<String, Object> args) {
        // 추후 확장
        return new StandardTravelStrategy();
    }

    // Duration 정규화
    private int normalizeDuration(Map<String, Object> args) {
        String normalized = durationNormalizerAgent.durationNormalized(args);
        return Integer.parseInt(normalized);
    }

    // 벡터 검색 & 셔플
    private List<TravelPlaceCandidate> searchAndShuffle(float[] embedding) {
        List<TravelPlaceCandidate> results = planDao.searchByVector(embedding, 100);
        int shuffleRange = Math.min(50, results.size());
        Collections.shuffle(results.subList(0, shuffleRange));
        return results;
    }

    private void logClusterResults(ClusterBundle clusters) {
        log.info("  클러스터 수: {}", clusters.getClusters().size());
        for (Cluster c : clusters.getClusters()) {
            log.info("    Cluster {}: {}개 장소", c.getId(), c.getPlaces().size());
        }
    }

    private void logDayPlans(List<DayPlanResult> dayPlans) {
        log.info("=== 최종 일정 ===");
        for (DayPlanResult day : dayPlans) {
            log.info("  Day {}: {}개 장소", day.getDayNumber(), day.getPlaces().size());
        }
    }

    private void logClusterInfoResult(ClusterBundle clusters) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n===== KMeans 클러스터 결과 =====\n");
        sb.append("총 클러스터 수: ").append(clusters.getClusters().size()).append("\n");

        for (Cluster c : clusters.getClusters()) {
            sb.append("\n[Cluster ").append(c.getId()).append("]")
                    .append(" (중심: ").append(String.format("%.4f", c.getCenterLat()))
                    .append(", ").append(String.format("%.4f", c.getCenterLng())).append(")")
                    .append(" - 장소 수: ").append(c.getPlaces().size()).append("\n");

            // 카테고리별 개수 추가
            Map<String, Long> categoryCount = c.getPlaces().stream()
                    .collect(Collectors.groupingBy(
                            p -> p.getOriginal().getNormalizedCategory(),
                            Collectors.counting()));

            sb.append("  카테고리: ").append(categoryCount).append("\n");

            for (ClusterPlace p : c.getPlaces()) {
                sb.append("  - ")
                        .append(p.getOriginal().getTravelPlaces().getTitle())
                        .append(" (").append(p.getOriginal().getNormalizedCategory()).append(")\n");
            }
        }
    }

    // 응답 메시지 생성
    private String buildResponse(List<DayPlanResult> dayPlans) {
        return "총 " + dayPlans.size() + "일 일정이 생성되었습니다. " + "\n 화면 우측에 있는 일정을 보고 수정하시고 싶은 부분이 있다면 말씀해주세요!";
    }

    private void printDayPlans(List<DayPlanResult> dayPlans) {

        log.info("=== 최종 일정 ===");

        for (DayPlanResult day : dayPlans) {

            log.info("Day {}:", day.getDayNumber());

            day.getPlaces().forEach(p -> {
                String title = p.getOriginal().getTravelPlaces().getTitle();
                String cat = p.getOriginal().getNormalizedCategory();
                log.info(" - {} ({})", title, cat);
            });
        }
    }

}