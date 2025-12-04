package com.example.demo.planner.travel.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.demo.planner.travel.dto.TravelPlaceCandidate;
import com.example.demo.planner.travel.utils.CategoryNames;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CategoryFillService {

    public Map<String, List<TravelPlaceCandidate>> fill(
            List<TravelPlaceCandidate> candidates,
            int minFood,
            int minSpot) {

        log.info("=== [1] 카테고리 분류 시작 ===");
        Map<String, List<TravelPlaceCandidate>> map = initCategoryMap();

        for (TravelPlaceCandidate c : candidates) {
            String cat = c.getNormalizedCategory();
            if (!map.containsKey(cat)) {
                log.warn("Unknown category: {}", cat);
                continue;
            }
            map.get(cat).add(c);
            log.debug("분류됨 → {} / {}", c.getTravelPlaces().getTitle(), cat);
        }

        printCategoryCount(map);

        log.info("=== [2] 필수 카테고리 보강 ===");

        strengthen(map, CategoryNames.FOOD, minFood);
        strengthen(map, CategoryNames.SPOT, minSpot);

        printCategoryCount(map);

        return map;
    }

    private Map<String, List<TravelPlaceCandidate>> initCategoryMap() {
        Map<String, List<TravelPlaceCandidate>> map = new HashMap<>();
        for (String cat : CategoryNames.ALL) {
            map.put(cat, new ArrayList<>());
        }
        return map;
    }

    private void strengthen(Map<String, List<TravelPlaceCandidate>> map,
            String category,
            int minCount) {
        int current = map.get(category).size();
        if (current >= minCount) {
            log.info("[{}] 충분 → {}개 (필요 {})", category, current, minCount);
            return;
        }

        int lacking = minCount - current;
        log.warn("[{}] 부족 → {}개 부족", category, lacking);

        // 아직 보강 로직은 구현 안함 (테스트 단계)
        // fill logic 추가 가능
    }

    private void printCategoryCount(Map<String, List<TravelPlaceCandidate>> map) {
        log.info("=== 카테고리 카운트 ===");
        for (String cat : CategoryNames.ALL) {
            log.info("{} = {}", cat, map.get(cat).size());
        }
    }
}
