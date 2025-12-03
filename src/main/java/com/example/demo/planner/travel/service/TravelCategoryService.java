package com.example.demo.planner.travel.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.planner.travel.agent.SeedQueryAgent;
import com.example.demo.planner.travel.dao.TravelDao;
import com.example.demo.planner.travel.dto.response.TravelPlaceSearchResult;
import com.example.demo.planner.travel.utils.CategoryRequirementChecker;
import com.example.demo.planner.travel.utils.CategoryUtils;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TravelCategoryService {

    private final SeedQueryAgent seedQueryAgent;
    private final EmbeddingModel embeddingModel;    
    private final TravelDao travelDao;

    public TravelCategoryService(SeedQueryAgent seedQueryAgent, EmbeddingModel embeddingModel, TravelDao travelDao) {
        this.seedQueryAgent = seedQueryAgent;
        this.embeddingModel = embeddingModel;
        this.travelDao = travelDao;
    }

    public Map<String, List<TravelPlaceSearchResult>> fill(Map<String, Object> arguments, int duration,
            List<TravelPlaceSearchResult> initialResults) {
        Map<String, List<TravelPlaceSearchResult>> categorized = CategoryUtils.categorize(initialResults);
        while (true) {
            CategoryUtils.printCategoryCount(categorized);
            // 1. 카테고리 갯수 계산
            Map<String, Integer> categoryCounts = CategoryRequirementChecker.countByCategory(categorized);
            // 2. 현재 부족한 카테고리 찾기
            List<String> missing = CategoryRequirementChecker.findMissingCategories(categoryCounts,
                    duration);
            if (missing.isEmpty()) {
                log.info("모든 카테고리 충족, 반복 종료");
                break;
            }

            // 3. 부족한 카테고리 순차 보강
            log.info(missing.toString());
            for (String category : missing) {
                List<Long> excludedIds = (List<Long>) categorized.get(category).stream()
                        .map(item -> item.getTravelPlaces().getId()).toList();
                // seed query 생성
                arguments.put("category", category);
                String categorySeedQuery = seedQueryAgent.generateSeedQuery(arguments);
                float[] embeddingCategorySeedQuery = embeddingModel.embed(categorySeedQuery);
                log.info("카테고리: " + category);
                log.info("id 리스트: " + excludedIds.toString());

                // 검색 파라미터 구성
                Map<String, Object> params = new HashMap<>();
                params.put("category", category);
                params.put("embedding", embeddingCategorySeedQuery);
                params.put("limit", CategoryRequirementChecker
                        .getMinRequiredForCategory(category, duration));
                params.put("excludedIds", excludedIds);
                log.info(params.toString());
                List<TravelPlaceSearchResult> searchVectorByCategoryResults = travelDao
                        .searchMissingCategoryByVector(params);

                // 4. 결과 저장 (기존 categorized 에 추가)
                categorized.get(category).addAll(searchVectorByCategoryResults);

                log.info("카테고리 {} 보강 결과 {}개 추가", category, searchVectorByCategoryResults.size());
                // 추가한 카테고리 디버깅
                // searchVectorByCategoryResults.forEach(r ->
                // log.info("{} {}", r.getScore(), r.getTravelPlaces().getTitle()));
            }
        }
        return categorized;
    }

}
