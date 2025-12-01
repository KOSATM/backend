package com.example.demo.planner.travel.agent;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.demo.common.chat.intent.dto.IntentCommand;
import com.example.demo.common.chat.pipeline.AiAgentResponse;
import com.example.demo.common.global.agent.AiAgent;
import com.example.demo.planner.travel.dao.TravelDao;
import com.example.demo.planner.travel.dto.response.TravelPlaceSearchResult;
import com.example.demo.planner.travel.utils.CategoryRequirementChecker;
import com.example.demo.planner.travel.utils.CategoryUtils;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class TravelPlannerAgent implements AiAgent {

        private ChatClient chatClient;
        private EmbeddingModel embeddingModel;
        private SeedQueryAgent seedQueryAgent;
        private DurationNormalizerAgent durationNormalizerAgent;

        @Autowired
        private TravelDao travelDao;

        public TravelPlannerAgent(ChatClient.Builder chatClientBuilder, EmbeddingModel embeddingModel,
                        SeedQueryAgent seedQueryAgent, DurationNormalizerAgent durationNormalizerAgent) {
                this.chatClient = chatClientBuilder.build();
                this.embeddingModel = embeddingModel;
                this.seedQueryAgent = seedQueryAgent;
                this.durationNormalizerAgent = durationNormalizerAgent;
        }

        @Override
        public AiAgentResponse execute(IntentCommand command) {

                log.info("▷ 1. TravelPlannerAgent 실행");

                String systemPrompt = """

                                """;

                log.info("▷ 2. SeedQueryAgent 실행");
                String seedQuery = seedQueryAgent.generateSeedQuery(command.getArguments());
                log.info(">>"+seedQuery);
                log.info("▷ 3. DurationNormalizerAgent 실행");
                String normalized = durationNormalizerAgent.durationNormalized(command.getArguments());
                int duration = Integer.parseInt(normalized);

                float[] embeddingSeedQuery = embeddingModel.embed(seedQuery);

                List<TravelPlaceSearchResult> travelPlaceSearchResults = travelDao.searchByVector(embeddingSeedQuery,
                                60);

                Map<String, List<TravelPlaceSearchResult>> categorized = CategoryUtils
                                .categorize(travelPlaceSearchResults);

                CategoryUtils.printCategoryCount(categorized);
                Map<String, Integer> categoryCounts = CategoryRequirementChecker.countByCategory(categorized);

                if (!CategoryRequirementChecker.isEnough(categoryCounts, duration)) {
                        List<String> missing = CategoryRequirementChecker.findMissingCategories(categoryCounts, duration);

                        // → 부족한 카테고리 보강 로직으로 이동
                        log.info(missing.toString());

                        
                }


                // CategoryUtils.printCategoryCount(categorized);

                log.info("▷ ?. TravelPlannerAgent 종료");
                return AiAgentResponse.of("TravelPlannerAgent 실행 결과\n" + categoryCounts);
        }
}
