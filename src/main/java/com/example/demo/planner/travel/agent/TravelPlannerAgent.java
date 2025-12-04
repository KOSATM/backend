package com.example.demo.planner.travel.agent;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

import com.example.demo.common.chat.intent.dto.IntentCommand;
import com.example.demo.common.chat.pipeline.AiAgentResponse;
import com.example.demo.common.global.agent.AiAgent;
import com.example.demo.planner.travel.dao.TravelDao;
import com.example.demo.planner.travel.dto.TravelPlaceCandidate;
import com.example.demo.planner.travel.service.CategoryFillService;
import com.example.demo.planner.travel.service.RegionService;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class TravelPlannerAgent implements AiAgent {

        private final ChatClient chatClient;
        private final EmbeddingModel embeddingModel;
        private final SeedQueryAgent seedQueryAgent;
        private final DurationNormalizerAgent durationNormalizerAgent;
        private final TravelDao travelDao;
        private final RegionService regionService;
        private final CategoryFillService categoryFillService;

        public TravelPlannerAgent(ChatClient.Builder chatClientBuilder, EmbeddingModel embeddingModel,
                        SeedQueryAgent seedQueryAgent, DurationNormalizerAgent durationNormalizerAgent,
                        TravelDao travelDao, RegionService regionService, CategoryFillService categoryFillService) {
                this.chatClient = chatClientBuilder.build();
                this.embeddingModel = embeddingModel;
                this.seedQueryAgent = seedQueryAgent;
                this.durationNormalizerAgent = durationNormalizerAgent;
                this.travelDao = travelDao;
                this.regionService = regionService;
                this.categoryFillService = categoryFillService;
        }

        @Override
        public AiAgentResponse execute(IntentCommand command) {
                Map<String, Object> arguments = command.getArguments();

                log.info("▷▷ 1. TravelPlannerAgent 실행");
                log.info("▷▷ 2. DurationNormalizerAgent(정규화) 실행");
                String normalized = durationNormalizerAgent.durationNormalized(arguments);
                int duration = Integer.parseInt(normalized);
                String location = (String) arguments.get("location");
                log.info("duration: " + duration);

                log.info("▷▷ 2. SeedQueryAgent() 실행");
                String seedQuery = seedQueryAgent.generateSeedQuery(arguments);
                log.info(">>" + seedQuery);

                float[] embeddingSeedQuery = embeddingModel.embed(seedQuery);
                log.info("▷▷ 4. 유사도 검색, 상위 50개 셔플");
                List<TravelPlaceCandidate> initialCandidates = candidates(embeddingSeedQuery);

                log.info("▷▷ 5. 지역 가중치 및 반경 필터링 후 여행지 압축(100->80)");
                List<TravelPlaceCandidate> regionFilteredCandidates = regionService
                                .applyRegionPreference(initialCandidates, location, duration);

                // categoryFillService.

                // Collections.shuffle(searchByVectorResults.subList(0, Math.min(80,
                // searchByVectorResults.size())));
                // searchByVectorResults = searchByVectorResults.subList(0, Math.min(60,
                // searchByVectorResults.size()));

                log.info("▷▷ 5. 카테고리 테스트 시작");
                Map<String, List<TravelPlaceCandidate>> categoryMap = categoryFillService.fill(
                                regionFilteredCandidates,
                                3, // 예: Food min=3
                                3 // 예: Spot min=3
                );

                return AiAgentResponse.of("TravelPlannerAgent 실행 결과\n");
        }

        private List<TravelPlaceCandidate> candidates(float[] embeddingSeedQuery) {
                List<TravelPlaceCandidate> candidates = travelDao.searchByVector(embeddingSeedQuery, 100);
                int shuffleRange = Math.min(50, candidates.size());
                Collections.shuffle(candidates.subList(0, shuffleRange));
                return candidates;
        }

}