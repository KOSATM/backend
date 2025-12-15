package com.example.demo.planner.plan.dto.entity;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record GeneratedTravelPlan(
        int duration,
        String pace,
        List<GeneratedDay> days,
        LocalDate startDate,
        LocalDate endDate) {
    public record GeneratedDay(
            int dayIndex,
            LocalDate dayDate,
            List<GeneratedPlace> places) {
    }

    public record GeneratedPlace(
            String title,
            String placeName,
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            double lat,
            double lng,
            String address,
            String category,
            String firstImage,
            String firstImage2
            ) {
    }
}