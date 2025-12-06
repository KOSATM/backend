package com.example.demo.planner.plan.utils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class DateTimeUtil {

    private static final ZoneOffset KST = ZoneOffset.of("+09:00");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * @param baseDate   여행 시작 날짜 (예: 2025-01-20)
     * @param dayIndex   1부터 시작
     * @param timeStr    "15:00" 형식의 시간
     */
    public static OffsetDateTime toOffsetDateTime(LocalDate baseDate, int dayIndex, String timeStr) {
        LocalDate date = baseDate.plusDays(dayIndex - 1);
        LocalTime time = LocalTime.parse(timeStr, TIME_FORMATTER);

        return OffsetDateTime.of(date, time, KST);
    }

    public static OffsetTime toOffsetDate(String timeStr) {
        LocalTime time = LocalTime.parse(timeStr, TIME_FORMATTER);
        return OffsetTime.of(time, KST);
    }
}