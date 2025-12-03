package com.example.demo.planner.travel.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.demo.planner.travel.dto.response.TravelPlaceSearchResult;

public class CategoryRequirementChecker {

    // 1일 기준 최소 요구량
    private static final Map<String, Integer> BASE_MIN_REQUIRED = Map.of(
            CategoryNames.SPOT, 4,
            CategoryNames.FOOD, 3,
            CategoryNames.CAFE, 1);

    // 카테고리별 개수 계산
    public static Map<String, Integer> countByCategory(
            Map<String, List<TravelPlaceSearchResult>> categorized) {
        Map<String, Integer> result = new HashMap<>();

        categorized.forEach((category, list) -> result.put(category, list.size()));

        return result;
    }

    // duration 기반 요구량 계산
    private static Map<String, Integer> getRequiredCounts(int duration) {
        Map<String, Integer> required = new HashMap<>();
        BASE_MIN_REQUIRED.forEach((cat, base) -> {
            required.put(cat, base * duration);
        });
        return required;
    }

    // 부족한 카테고리 찾기
    public static List<String> findMissingCategories(
            Map<String, Integer> categoryCounts,
            int duration) {

        Map<String, Integer> required = getRequiredCounts(duration);
        List<String> missing = new ArrayList<>();

        for (var entry : required.entrySet()) {
            String category = entry.getKey();
            int requiredCount = entry.getValue();
            int actualCount = categoryCounts.getOrDefault(category, 0);

            if (actualCount < requiredCount) {
                missing.add(category);
            }
        }

        return missing;
    }

    public static boolean isEnough(
            Map<String, Integer> categoryCounts, int duration) {
        return findMissingCategories(categoryCounts, duration).isEmpty();
    }

    public static int getMinRequiredForCategory(String category, int duration) {
    int base = BASE_MIN_REQUIRED.getOrDefault(category, 0);
    return base * duration;
    }

    
}