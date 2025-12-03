package com.example.demo.planner.travel.agent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

import com.example.demo.common.chat.intent.dto.IntentCommand;
import com.example.demo.common.chat.pipeline.AiAgentResponse;
import com.example.demo.common.global.agent.AiAgent;
import com.example.demo.planner.travel.cluster.ClusterBundle;
import com.example.demo.planner.travel.cluster.DbscanClusterer;
import com.example.demo.planner.travel.dao.TravelDao;
import com.example.demo.planner.travel.dto.response.ClusterResult;
import com.example.demo.planner.travel.dto.response.DayPlanResult;
import com.example.demo.planner.travel.dto.response.TravelPlaceSearchResult;
import com.example.demo.planner.travel.service.ClusterService;
import com.example.demo.planner.travel.service.TravelCategoryService;
import com.example.demo.planner.travel.utils.CategoryUtils;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class TravelPlannerAgent implements AiAgent {

        private final ChatClient chatClient;
        private final EmbeddingModel embeddingModel;
        private final SeedQueryAgent seedQueryAgent;
        private final DurationNormalizerAgent durationNormalizerAgent;
        private final TravelCategoryService travelCategoryService;
        private final TravelDao travelDao;
        private final ClusterService clusterService;

        public TravelPlannerAgent(ChatClient.Builder chatClientBuilder, EmbeddingModel embeddingModel,
                        SeedQueryAgent seedQueryAgent, DurationNormalizerAgent durationNormalizerAgent,
                        TravelCategoryService travelCategoryService, TravelDao travelDao,
                        ClusterService clusterService) {
                this.chatClient = chatClientBuilder.build();
                this.embeddingModel = embeddingModel;
                this.seedQueryAgent = seedQueryAgent;
                this.durationNormalizerAgent = durationNormalizerAgent;
                this.travelCategoryService = travelCategoryService;
                this.travelDao = travelDao;
                this.clusterService = clusterService;
        }

        @Override
        public AiAgentResponse execute(IntentCommand command) {

                log.info("▷▷ 1. TravelPlannerAgent 실행");
                Map<String, Object> arguments = command.getArguments();

                log.info("▷▷ 2. SeedQueryAgent() 실행");
                String seedQuery = seedQueryAgent.generateSeedQuery(arguments);
                log.info(">>" + seedQuery);
                log.info("▷▷ 3. DurationNormalizerAgent(정규화) 실행");
                String normalized = durationNormalizerAgent.durationNormalized(arguments);
                int duration = Integer.parseInt(normalized);
                log.info("duration: " + duration);

                float[] embeddingSeedQuery = embeddingModel.embed(seedQuery);
                log.info("▷▷ 4. 벡터 검색 실행");
                List<TravelPlaceSearchResult> searchByVectorResults = travelDao.searchByVector(embeddingSeedQuery, 100);

                Collections.shuffle(searchByVectorResults.subList(0, Math.min(80, searchByVectorResults.size())));
                searchByVectorResults = searchByVectorResults.subList(0, Math.min(60, searchByVectorResults.size()));

                log.info("▷▷ 5. 카테고리 그룹화 후 부족한 카테고리 보강");
                Map<String, List<TravelPlaceSearchResult>> categorized = travelCategoryService.fill(arguments, duration,
                                searchByVectorResults);

                log.info("▷▷ 6. 카테고리별 장소 클러스터링(군집화), 노이즈 장소 저장");
                DbscanClusterer clusterer = new DbscanClusterer(0.8, 3);
                List<TravelPlaceSearchResult> allPlaces = CategoryUtils.flatten(categorized);
                ClusterBundle bundle = clusterer.cluster(allPlaces);
                List<ClusterResult> clusters = bundle.clusters();
                List<TravelPlaceSearchResult> noise = new ArrayList<>(bundle.noise());
                // log.info(noise.toString());

                log.info("▷▷ 7. 클러스터 정렬 수행");
                clusterService.sortByScore(clusters);
                List<ClusterResult> filterClusters = clusterService.filter(clusters);
                List<TravelPlaceSearchResult> filteredOut = clusters.stream()
                                .filter(c -> !filterClusters.contains(c))
                                .flatMap(c -> c.getPlaces().stream())
                                .toList();
                noise.addAll(filteredOut);
                clusterService.sortByDistance(filterClusters);

                debugClusters(filterClusters);

                log.info("▷▷ 8. Day 분할");
                List<DayPlanResult> dayPlans = clusterService.splitIntoDays(filterClusters, noise, duration);

                debugDays(dayPlans);

                String systemPrompt = """

                                """;

                log.info("▷▷ 9. TravelPlannerAgent 종료");
                return AiAgentResponse.of("TravelPlannerAgent 실행 결과\n" + categorized.toString());
        }

        private void debugClusters(List<ClusterResult> clusters) {
                for (ClusterResult cluster : clusters) {
                        log.info("=== Cluster #{} ===", cluster.getClusterNumber());
                        for (TravelPlaceSearchResult r : cluster.getPlaces()) {
                                log.info("{} / {} / {}",
                                                r.getTravelPlaces().getTitle(),
                                                r.getTravelPlaces().getAddress(),
                                                r.getTravelPlaces().getNormalizedCategory());
                        }
                }
        }

        /**
         * ClusterResult 사용
         */
        private void debugDays(List<DayPlanResult> dayPlans) {
                for (DayPlanResult day : dayPlans) {
                        log.info("========================================");
                        log.info("============== Day {} ==============", day.getDayNumber());
                        log.info("========================================");

                        for (ClusterResult cluster : day.getClusters()) {
                                log.info("  === Cluster {} ===", cluster.getClusterNumber());
                                for (TravelPlaceSearchResult place : cluster.getPlaces()) {
                                        log.info("    [{}] {} / {}",
                                                        place.getTravelPlaces().getNormalizedCategory(),
                                                        place.getTravelPlaces().getTitle(),
                                                        place.getTravelPlaces().getAddress());
                                }
                        }
                        log.info("");
                }
        }
}