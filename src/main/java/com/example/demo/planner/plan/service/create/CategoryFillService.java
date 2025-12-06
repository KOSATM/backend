package com.example.demo.planner.plan.service.create;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import com.example.demo.planner.plan.agent.SeedQueryAgent;
import com.example.demo.planner.plan.dao.PlanDao;
import com.example.demo.planner.plan.dto.TravelPlaceCandidate;
import com.example.demo.planner.travel.utils.CategoryNames;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class CategoryFillService {

    private final SeedQueryAgent seedQueryAgent;
    private final EmbeddingModel embeddingModel;
    private final PlanDao planDao;

    /* 카테고리별로 후보들을 분류하고, SPOT / FOOD 등 필수 카테고리가 최소 개수보다 부족한지 체크만 함. */
    public Map<String, List<TravelPlaceCandidate>> fill(
            List<TravelPlaceCandidate> candidates, Map<String, Object> arguments,
            int minFood,
            int minSpot) {

        log.info("=== [1] 카테고리 분류 시작 ===");

        Map<String, List<TravelPlaceCandidate>> map = initCategoryMap();

        // 후보들을 카테고리별로 분류
        for (TravelPlaceCandidate c : candidates) {
            String cat = c.getNormalizedCategory();

            if (!map.containsKey(cat)) {
                log.warn("Unknown category: {}", cat);
                continue;
            }

            map.get(cat).add(c);
        }

        printCategoryCount(map);

        log.info("=== [2] 필수 카테고리 개수 체크 ===");

        // SPOT / FOOD는 필수 → 부족하면 다음 단계에서 보강 필요성만 알려줌
        strengthen(map, arguments, CategoryNames.FOOD, minFood);
        strengthen(map, arguments, CategoryNames.SPOT, minSpot);
        arguments.remove("category");

        printCategoryCount(map);

        log.info("=== 카테고리 분배 완료 ===");
        for (String cat : map.keySet()) {
            log.info("Category {} → {}개", cat, map.get(cat).size());
        }

        return map;
    }

    /**
     * 카테고리 맵을 하나의 리스트로 병합
     */
    public List<TravelPlaceCandidate> merge(Map<String, List<TravelPlaceCandidate>> categoryMap) {
        List<TravelPlaceCandidate> merged = new ArrayList<>();

        for (String cat : CategoryNames.ALL) {
            merged.addAll(categoryMap.get(cat));
        }

        return merged;
    }

    /**
     * 카테고리 초기화
     */
    private Map<String, List<TravelPlaceCandidate>> initCategoryMap() {
        Map<String, List<TravelPlaceCandidate>> map = new HashMap<>();

        for (String cat : CategoryNames.ALL) {
            map.put(cat, new ArrayList<>());
        }

        return map;
    }

    /* 특정 카테고리가 최소 갯수보다 부족하면 로그로 알려줌 */
    private void strengthen(Map<String, List<TravelPlaceCandidate>> map, Map<String, Object> arguments,
            String category,
            int minCount) {

        List<TravelPlaceCandidate> currentList = map.get(category);
        int current = map.get(category).size();

        if (current >= minCount) {
            log.info("[{}] 충분 → {}개 (필요 {})", category, current, minCount);
            return;
        }

        int lacking = minCount - current;
        log.warn("[{}] 부족 → {}개 → 전역 DB 보강 시작", category, lacking);

        // 1) Seed Query 생성 (category 모드)
        arguments.put("category", category); // category 모드 유도
        String seedQuery = seedQueryAgent.generateSeedQuery(arguments);

        // 2) embedding 생성
        float[] embedding = embeddingModel.embed(seedQuery);

        List<Long> excludedIds = currentList.stream()
                .map(c -> c.getTravelPlaces().getId())
                .toList();

        // 4) 전역 DB에서 카테고리 보강 검색
        List<TravelPlaceCandidate> fetched = planDao.searchMissingCategoryByVector(
                Map.of("category", category, "embedding", embedding, "excludedIds", excludedIds, "limit", lacking));

        // 5) 보강 append
        currentList.addAll(fetched);

        log.info("[{}] 보강 완료 → {}개 추가, 최종 {}개",
                category,
                fetched.size(),
                currentList.size());
    }

    /**
     * 카테고리 개수 로그 출력
     */
    private void printCategoryCount(Map<String, List<TravelPlaceCandidate>> map) {
        log.info("=== 카테고리 카운트 ===");
        for (String cat : CategoryNames.ALL) {
            log.info("{} = {}", cat, map.get(cat).size());
        }
    }
}
