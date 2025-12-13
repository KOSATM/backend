package com.example.demo.planner.plan.dto.response;

import java.util.List;

import lombok.Data;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class PlanScheduleResult {

    private List<Day> days;

    @Data
    public static class Day {
        private Long id;
        private int dayIndex;
        private List<Item> items;
    }

    @Data
    public static class Item {
        private Long id;
        private String start;
        private String end;
        private int order;
    }
}