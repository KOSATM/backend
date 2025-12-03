package com.example.demo.planner.travel.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.example.demo.planner.travel.dto.response.TravelPlaceSearchResult;

public class CategoryUtils {

    /* 검색 결과를 normalizedCategory 기준으로 그룹핑한다. (null은 ETC로 처리) */
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

    /* 카테고리별 개수 출력 (디버깅용) */
    public static void printCategoryCount(Map<String, List<TravelPlaceSearchResult>> categorized) {
        categorized.forEach((key, value) -> System.out.println(key + " = " + value.size()));
    }

    /* 카테고리별 상위 N개만 가져오기 */
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

    // 최종 후보 리스트 만들기
    public static List<TravelPlaceSearchResult> flatten(
            Map<String, List<TravelPlaceSearchResult>> categorized) {

        return categorized.values().stream()
                .flatMap(List::stream)
                .toList();
    }

    /**
     * 리스트에서 카테고리별 개수 계산 (Map 반환)
     */
    public static Map<String, Long> countByCategory(List<TravelPlaceSearchResult> places) {
        return places.stream()
                .collect(Collectors.groupingBy(
                        r -> Optional.ofNullable(r.getTravelPlaces().getNormalizedCategory())
                                .orElse(CategoryNames.ETC),
                        Collectors.counting()));
    }
    
    /**
     * 특정 카테고리 개수만 계산
     */
    public static int countCategory(List<TravelPlaceSearchResult> places, String category) {
        return (int) places.stream()
                .filter(p -> category.equals(p.getTravelPlaces().getNormalizedCategory()))
                .count();
    }

    /**
     * 특정 카테고리만 추출
     */
    public static List<TravelPlaceSearchResult> filterByCategory(
            List<TravelPlaceSearchResult> places, 
            String category) {
        return places.stream()
                .filter(p -> category.equals(p.getTravelPlaces().getNormalizedCategory()))
                .toList();
    }

     /**
     * 특정 카테고리 제외하고 추출
     */
    public static List<TravelPlaceSearchResult> excludeCategory(
            List<TravelPlaceSearchResult> places, 
            String category) {
        return places.stream()
                .filter(p -> !category.equals(p.getTravelPlaces().getNormalizedCategory()))
                .toList();
    }


    /**
     * 여러 카테고리만 추출
     */
    public static List<TravelPlaceSearchResult> filterByCategories(
            List<TravelPlaceSearchResult> places, 
            List<String> categories) {
        Set<String> categorySet = new HashSet<>(categories);
        return places.stream()
                .filter(p -> categorySet.contains(p.getTravelPlaces().getNormalizedCategory()))
                .toList();
    }

}