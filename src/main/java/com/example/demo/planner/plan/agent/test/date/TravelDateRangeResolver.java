package com.example.demo.planner.plan.agent.test.date;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TravelDateRangeResolver {

    /**
     * startDate + duration을 활용하여 여행 날짜 리스트 생성
     */
    public static List<LocalDate> resolve(LocalDate start, String durationText) {
        int days = parseDurationToDays(durationText);

        List<LocalDate> result = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            result.add(start.plusDays(i));
        }
        return result;
    }

    /**
     * "3일", "하루", "주말" → day count
     */
    private static int parseDurationToDays(String duration) {
        if (duration == null)
            return 1;

        duration = duration.trim();

        if (duration.equals("하루") || duration.equals("1일"))
            return 1;
        if (duration.equals("이틀") || duration.equals("2일"))
            return 2;
        if (duration.equals("사흘") || duration.equals("3일"))
            return 3;

        // N일
        if (duration.matches("\\d+일")) {
            return Integer.parseInt(duration.replace("일", ""));
        }

        // 주말 = 2일 (토/일)
        if (duration.contains("주말"))
            return 2;

        // default
        return 1;
    }
}