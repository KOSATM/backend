package com.example.demo.planner.plan.service.create;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

import com.example.demo.common.chat.intent.agent.ResponseAgent;
import com.example.demo.common.chat.intent.dto.IntentCommand;
import com.example.demo.common.chat.pipeline.AiAgentResponse;
import com.example.demo.common.global.agent.AiAgent;
import com.example.demo.planner.plan.agent.DurationNormalizerAgent;
import com.example.demo.planner.plan.agent.SeedQueryAgent;
import com.example.demo.planner.plan.agent.StartDateNormalizerAgent;
import com.example.demo.planner.plan.dao.PlanDao;
import com.example.demo.planner.plan.dao.PlanSnapshotDao;
import com.example.demo.planner.plan.dto.Cluster;
import com.example.demo.planner.plan.dto.ClusterBundle;
import com.example.demo.planner.plan.dto.ClusterPlace;
import com.example.demo.planner.plan.dto.TravelPlaceCandidate;
import com.example.demo.planner.plan.dto.entity.PlanSnapshot;
import com.example.demo.planner.plan.dto.response.DayPlanResult;
import com.example.demo.planner.plan.dto.response.PlanDetailResponse;
import com.example.demo.planner.plan.strategy.StandardTravelStrategy;
import com.example.demo.planner.plan.strategy.TravelPlanStrategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
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
    private final StartDateNormalizerAgent startDateNormalizerAgent;
    private final PlanAssemblerService planAssemblerService;
    private final PlanService planService;
    private final PlanSnapshotDao planSnapshotDao;
    private final ResponseAgent responseAgent;

    @Override
    public AiAgentResponse execute(IntentCommand command, Long userId) {

        log.info("▷▷ 1. TravelPlannerAgent 시작");

        Map<String, Object> arguments = command.getArguments();

        // 전략 선택
        TravelPlanStrategy strategy = selectStrategy(arguments);

        // Duration 정규화
        normalizeDuration(arguments);
        int duration = (int) arguments.get("duration");
        String location = (String) arguments.getOrDefault("location", "서울");

        int minSpot = strategy.getTotalMinSpot(duration);
        int minFood = strategy.getTotalMinFood(duration);

        //  변경됨: 멀티 SeedQuery 생성
        log.info("▷▷ 3. SeedQuery 생성 (Multi Query)");
        List<String> seedQueries = seedQueryAgent.generateMultiSeedQueries(arguments);
        seedQueries.forEach(q -> log.info("  SeedQuery: {}", q));

        //  변경됨: 멀티 벡터 검색 + 병합
        log.info("▷▷ 4. 벡터 검색 (Multi Search) & 병합");
        List<TravelPlaceCandidate> candidates = seedQueries.stream()
                .flatMap(q -> {
                    float[] embedding = embeddingModel.embed(q);
                    return searchAndShuffle(embedding).stream();
                })
                .distinct()
                .toList();

        //  중복 제거
        candidates = candidates.stream()
                .collect(Collectors.toMap(
                        c -> c.getTravelPlaces().getId(),
                        c -> c,
                        (c1, c2) -> c1))
                .values().stream()
                .collect(Collectors.toList());

        log.info("  멀티 검색 후보 총 {}개", candidates.size());

        // 방문 이력 제외
        List<String> visited = planDao.getUserLastVisitedPlaces(userId);
        if (visited != null && !visited.isEmpty()) {
            int beforeSize = candidates.size();
            candidates = candidates.stream()
                    .filter(c -> !visited.contains(c.getTravelPlaces().getTitle()))
                    .collect(Collectors.toList());

            log.info("  방문 이력 제외 - {}개 → {}개", beforeSize, candidates.size());
        }

        // 지역 필터링
        log.info("▷▷ 5. 지역 필터링");
        List<TravelPlaceCandidate> filtered = regionService.applyRegionPreference(
                candidates, location, duration);

        log.info("  필터 후: {}개 ({}개 제거)",
                filtered.size(), candidates.size() - filtered.size());

        // 카테고리 보강
        log.info("▷▷ 6. 카테고리 보강");
        Map<String, List<TravelPlaceCandidate>> categoryMap = categoryFillService.fill(filtered, arguments, minFood,
                minSpot);

        // 병합
        log.info("▷▷ 7. 카테고리 병합");
        List<TravelPlaceCandidate> merged = categoryFillService.merge(categoryMap);

        // 클러스터링
        log.info("▷▷ 8. KMeans 클러스터링");
        ClusterBundle clusters = kMeansClusterService.cluster(merged, duration);

        // Day 분할
        log.info("▷▷ 9. 일정 분배");
        List<DayPlanResult> dayPlans = daySplitService.split(clusters, duration, strategy, merged);

        // 저장
        log.info("▷▷ 10. 최종 일정 배치 후 저장");
        PlanDetailResponse response = planAssemblerService.createAndSavePlan(dayPlans, arguments, userId);

        log.info("▷▷ 11. TravelPlannerAgent 완료");

        printDayPlans(dayPlans);

        // planDao.
        
        PlanSnapshot snapshot = planSnapshotDao.selectLatestPlanSnapshotByUserId(userId);
        String snapshotJson = snapshot.getSnapshotJson();

        
        // return AiAgentResponse.of(buildResponse(dayPlans));
        return AiAgentResponse.ofData("일정이 생성되었습니다.", command.getRequiredUrl(), snapshotJson);

    }

    // ==================== Private 메서드 ====================
    // 로그
    private void logDuplicatePlaces(List<DayPlanResult> dayPlans) {
        Set<Long> used = new HashSet<>();

        for (DayPlanResult day : dayPlans) {
            for (ClusterPlace cp : day.getPlaces()) {
                long id = cp.getOriginal().getId();
                if (!used.add(id)) {
                    log.warn(" DUPLICATE DETECTED: placeId={} on day={}", id, day.getDayNumber());
                }
            }
        }
    }

    private TravelPlanStrategy selectStrategy(Map<String, Object> args) {
        // 추후 확장
        return new StandardTravelStrategy();
    }

    // 정규화
    private void normalizeDuration(Map<String, Object> arguments) {
        String duration = durationNormalizerAgent.normalized(arguments);
        String startDate = arguments.containsKey("startDate") != false ? startDateNormalizerAgent.normalized(arguments)
                : (LocalDate.now().plusDays(7)).toString();
        arguments.put("duration", Integer.parseInt(duration));
        arguments.put("startDate", startDate);
        return;
    }

    // 벡터 검색 & 셔플
    private List<TravelPlaceCandidate> searchAndShuffle(float[] embedding) {
        List<TravelPlaceCandidate> results = planDao.searchByVector(embedding, 100);
        int shuffleRange = Math.min(80, results.size());
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