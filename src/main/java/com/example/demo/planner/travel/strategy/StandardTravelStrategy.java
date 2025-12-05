package com.example.demo.planner.travel.strategy;

public class StandardTravelStrategy implements TravelPlanStrategy {

    @Override
    public DayRequirement getDayRequirement(int day, int duration) {
        DayRequirement req = new DayRequirement();

        if (day == 1) { // 도착일
            req.setMinFood(1);
            req.setMinSpot(1);
            req.setMaxFood(1);
            req.setMinOptional(1);
            req.setMaxPlaces(3); // ★ 도착일은 가볍게 3~4개
            return req;
        }

        if (day == duration) { // 출발일
            req.setMinFood(1);
            req.setMaxFood(1);
            req.setMinSpot(1);
            req.setMinOptional(1);
            req.setMaxPlaces(3);
            return req;
        }

        // 중간 날짜
        req.setMinFood(3);
        req.setMaxFood(3);
        req.setMinSpot(3);
        req.setMinOptional(1);
        req.setMaxPlaces(7);
        return req;
    }

}