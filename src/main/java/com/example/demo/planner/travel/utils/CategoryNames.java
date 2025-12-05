package com.example.demo.planner.travel.utils;

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
    public static final List<String> ALL = List.of(SPOT, FOOD, CAFE, EVENT, SHOPPING, STAY, ETC);

    // 1. FOOD 최우선 → 끼니 보장
    // 2. SPOT 필수 → 핵심 관광지
    // 3. CAFE 선택 → 휴식 공간
    // 4. EVENT 선택 → 특별한 경험
    // 5. SHOPPING 선택 → 쇼핑
    public static final List<String> REQUIRED = List.of(
            FOOD, // 최우선
            SPOT // 필수
    );

    // 기타 카테고리
    public static final List<String> OPTIONAL = List.of(
            CAFE, EVENT, SHOPPING, ETC);
    // STAY는 숙소 관리이므로 일정에서 제외
}
