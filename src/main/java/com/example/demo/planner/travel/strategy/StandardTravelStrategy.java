package com.example.demo.planner.travel.strategy;

public class StandardTravelStrategy implements TravelPlanStrategy {

    @Override
    public DayRequirement getDayRequirement(int day, int duration) {
        DayRequirement req = new DayRequirement();

        // 도착일
        if (day == 1) {
            req.setMinFood(1);
            req.setMinSpot(1);
            req.setMinOptional(1);  // CAFE/SHOPPING/EVENT/ETC 중 1개
            return req;
        }

        // 출발일
        if (day == duration) {
            req.setMinFood(1);
            req.setMinSpot(1);
            req.setMinOptional(1); // 여기서는 SHOPPING 가능성 높임
            return req;
        }

        // 중간일
        req.setMinFood(3);
        req.setMinSpot(3);
        req.setMinOptional(1); // "선택카테고리 1개" 규칙
        return req;
    }
}