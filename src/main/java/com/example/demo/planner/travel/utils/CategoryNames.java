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

    // 일정 최소 필수 카테고리
    public static final List<String> REQUIRED = List.of(SPOT, FOOD, CAFE);
}
