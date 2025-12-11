package com.example.demo.planner.plan.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.example.demo.planner.plan.dto.entity.PlanScheduleRow;

import lombok.Data;

@Data
public class PlanDetailResponse {

    private Long planId;
    private BigDecimal budget;
    private String startDate;
    private String endDate;

    private List<Day> days;

    @Data
    public static class Day {
        private Integer dayIndex;
        private String date;
        private List<Schedule> schedules;
    }

    @Data
    public static class Schedule {
        private Long placeId;
        private String title;
        private String startAt;
        private String endAt;
        private String placeName;
        private String address;
        private BigDecimal expectedCost;
        private String normalizedCategory;
        private String firstImage;
        private String firstImage2;
        private Boolean isEnded;
    }

    /**
     * DB JOIN 결과(row) → API Response 변환
     */
    public static PlanDetailResponse fromRows(List<PlanScheduleRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return null;
        }

        PlanScheduleRow first = rows.get(0);

        PlanDetailResponse response = new PlanDetailResponse();
        response.setPlanId(first.getPlanId());
        response.setBudget(first.getBudget());
        response.setStartDate(first.getStartDate().toString());
        response.setEndDate(first.getEndDate().toString());

        Map<Long, List<PlanScheduleRow>> byDay = rows.stream().collect(Collectors.groupingBy(
                PlanScheduleRow::getDayId,
                LinkedHashMap::new,
                Collectors.toList()));

        List<Day> days = new ArrayList<>();

        for (List<PlanScheduleRow> dayRows : byDay.values()) {
            PlanScheduleRow base = dayRows.get(0);

            Day day = new Day();
            day.setDayIndex(base.getDayIndex());
            day.setDate(base.getPlanDate().toString());

            List<Schedule> schedules = dayRows.stream().map(r -> {
                Schedule s = new Schedule();
                s.setPlaceId(r.getPlaceId());
                s.setTitle(r.getPlaceTitle());

                s.setStartAt(toTimeString(r.getStartAt()));
                s.setEndAt(toTimeString(r.getEndAt()));

                s.setPlaceName(r.getPlaceName());
                s.setAddress(r.getAddress());
                s.setExpectedCost(r.getExpectedCost());
                s.setNormalizedCategory(r.getNormalizedCategory());
                s.setFirstImage(r.getFirstImage());
                s.setFirstImage2(r.getFirstImage2());
                s.setIsEnded(r.getIsEnded());
                return s;
            }).toList();

            day.setSchedules(schedules);
            days.add(day);
        }

        response.setDays(days);
        return response;
    }

    // 시간 포맷을 위한 변수
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private static String toTimeString(OffsetDateTime dt) {
        if (dt == null)
            return null;

        return dt.atZoneSameInstant(KST)
                .toLocalTime()
                .format(TIME_FMT);
    }
}