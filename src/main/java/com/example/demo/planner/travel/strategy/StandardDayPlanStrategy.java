package com.example.demo.planner.travel.strategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.demo.planner.travel.dto.DayTarget;
import com.example.demo.planner.travel.utils.CategoryNames;

@Component
public class StandardDayPlanStrategy implements DayPlanStrategy {

    // FOOD는 고정값
    private static final int FOOD_PER_FULL_DAY = 3; // 중간날: 아침/점심/저녁
    private static final int FOOD_PER_LIGHT_DAY = 2; // 도착일/출발일: 점심/저녁
    private static final int MAX_PLACES_PER_DAY = 7;

    @Override
    public List<DayTarget> createDayTargets(int duration) {

        if (duration == 1) {
            return List.of(createOneDayTarget());
        }

        if (duration == 2) {
            return List.of(
                    createArrivalDayTarget(),
                    createDepartureDayTarget());
        }

        List<DayTarget> targets = new ArrayList<>();

        // 첫날 (도착)
        targets.add(createArrivalDayTarget());

        // 중간날
        for (int i = 1; i < duration - 1; i++) {
            targets.add(createFullDayTarget());
        }

        // 마지막날 (출발)
        targets.add(createDepartureDayTarget());

        return targets;
    }

    /** 1일 여행 */
    private DayTarget createOneDayTarget() {
        int maxPlaces = 5;
        int fixedFood = 2;

        // 1일 여행이면: FOOD 2개 + SPOT 3개(최대한)
        Map<String, Integer> t = new HashMap<>();
        t.put(CategoryNames.FOOD, fixedFood);
        t.put(CategoryNames.SPOT, maxPlaces - fixedFood); // 나머지 전부 SPOT

        return new DayTarget(t, maxPlaces);
    }

    /** 도착일 */
    private DayTarget createArrivalDayTarget() {
        int maxPlaces = 4;
        int fixedFood = FOOD_PER_LIGHT_DAY; // 2개
        int remaining = maxPlaces - fixedFood; // 2개

        Map<String, Integer> t = new HashMap<>();
        t.put(CategoryNames.FOOD, fixedFood);
        // 도착일은 SPOT도 필수로 2개 채우기
        t.put(CategoryNames.SPOT, remaining);

        return new DayTarget(t, maxPlaces);
    }

    /** 중간날 */
    private DayTarget createFullDayTarget() {
        int maxPlaces = MAX_PLACES_PER_DAY; // 7
        int fixedFood = FOOD_PER_FULL_DAY; // 3

        // 네가 말한대로: FOOD 3 + SPOT 3 = 6개 필수
        int requiredSpot = 3;

        Map<String, Integer> t = new HashMap<>();
        t.put(CategoryNames.FOOD, fixedFood);
        t.put(CategoryNames.SPOT, requiredSpot);
        // 나머지 1자리는 DayTarget에 명시하지 않음 → “아무 카테고리”로 채움

        return new DayTarget(t, maxPlaces);
    }

    /** 출발일 */
    private DayTarget createDepartureDayTarget() {
        int maxPlaces = 3;
        int fixedFood = 1;
        int remaining = maxPlaces - fixedFood; // 2개

        Map<String, Integer> t = new HashMap<>();
        t.put(CategoryNames.FOOD, fixedFood);
        // 출발일도 SPOT 1개는 기본으로 깔고
        t.put(CategoryNames.SPOT, 1);
        // 남은 1개는 DayTarget에 명시하지 않음 → 아무거나

        return new DayTarget(t, maxPlaces);
    }
}
