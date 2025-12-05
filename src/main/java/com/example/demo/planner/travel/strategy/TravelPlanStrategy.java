package com.example.demo.planner.travel.strategy;

public interface TravelPlanStrategy {
    DayRequirement getDayRequirement(int dayIndex, int duration);

    default int getTotalMinSpot(int duration) {
        int total = 0;
        for (int d = 1; d <= duration; d++) {
            total += getDayRequirement(d, duration).getMinSpot();
        }
        return total;
    }

    default int getTotalMinFood(int duration) {
        int total = 0;
        for (int d = 1; d <= duration; d++) {
            total += getDayRequirement(d, duration).getMinFood();
        }
        return total;
    }
}
