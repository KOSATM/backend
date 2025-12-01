package com.example.demo.planner.travel.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.example.demo.planner.travel.dto.response.TravelPlaceSearchResult;

public class CategoryUtils {

    /**
     * 검색 결과를 normalizedCategory 기준으로 그룹핑한다.
     * (null은 ETC로 처리)
     */
    public static Map<String, List<TravelPlaceSearchResult>> categorize(
            List<TravelPlaceSearchResult> items) {

        Map<String, List<TravelPlaceSearchResult>> result = items.stream()
                .collect(Collectors.groupingBy(
                        r -> Optional.ofNullable(r.getTravelPlaces().getNormalizedCategory())
                                .orElse(CategoryNames.ETC)));

        // 빠진 카테고리는 빈 리스트 채워줌
        CategoryNames.ALL.forEach(cat -> result.putIfAbsent(cat, new ArrayList<>()));

        return result;
    }

    /**
     * 카테고리별 개수 출력 (디버깅용)
     */
    public static void printCategoryCount(Map<String, List<TravelPlaceSearchResult>> categorized) {
        categorized.forEach((key, value) -> System.out.println(key + " = " + value.size()));
    }

    /**
     * 카테고리별 상위 N개만 가져오기
     */
    public static Map<String, List<TravelPlaceSearchResult>> topN(
            Map<String, List<TravelPlaceSearchResult>> categorized,
            int n) {

        Map<String, List<TravelPlaceSearchResult>> top = new HashMap<>();

        for (String key : CategoryNames.ALL) {
            List<TravelPlaceSearchResult> list = categorized.getOrDefault(key, List.of());

            // score 기준 정렬 + 상위 n개
            List<TravelPlaceSearchResult> sliced = list.stream()
                    .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                    .limit(n)
                    .toList();

            top.put(key, sliced);
        }
        return top;
    }
}