package com.example.demo.planner.plan.utils;

import java.util.List;

public class CategoryNames {
        // 일정 생성/추천에서 사용할 카테고리 목록
        public static final String SPOT = "SPOT";
        public static final String FOOD = "FOOD";
        public static final String CAFE = "CAFE";
        public static final String EVENT = "EVENT";
        public static final String SHOPPING = "SHOPPING";
        public static final String STAY = "STAY";
        public static final String ETC = "ETC";

        // 전체 카테고리 집합
        public static final List<String> ALL = List.of(SPOT, FOOD, CAFE, EVENT, SHOPPING, ETC);
        public static final List<String> EXCLUDE_ETC = List.of(SPOT, FOOD, CAFE, EVENT, SHOPPING);

        public static final List<String> REQUIRED = List.of(
                        FOOD, // 최우선
                        SPOT // 필수
        );

        // 기타 카테고리
        public static final List<String> OPTIONAL = List.of(
                        CAFE, EVENT, SHOPPING, ETC);
        // STAY는 숙소 관리이므로 일정에서 제외

        public static String categoryLabel(String category) {
                return switch (category) {
                        case "SPOT" -> "📍 관광";
                        case "FOOD" -> "🍽 음식";
                        case "CAFE" -> "☕ 카페";
                        case "EVENT" -> "🎉 행사";
                        case "SHOPPING" -> "🛍 쇼핑";
                        case "STAY" -> "🏨 숙소";
                        default -> "🔹 기타";
                };
        }
}
