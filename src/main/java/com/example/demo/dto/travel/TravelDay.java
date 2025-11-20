package com.example.demo.dto.travel;

import lombok.Data;

@Data
public class TravelDay {
    private Long id;
    private Long tripPlanId;
    private Integer dayIndex;
    private String title;
    private String date;
}
