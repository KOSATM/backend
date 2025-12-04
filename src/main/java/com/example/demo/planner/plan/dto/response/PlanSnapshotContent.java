package com.example.demo.planner.plan.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import lombok.Data;

@Data
public class PlanSnapshotContent {
  private Long userId;
  private Double budget;
  private String startDate;
  private String endDate;
  private List<PlanDay> days;

  @Data
  public static class PlanDay {
    private String date;
    private String title;
    private List<PlanDayItem> schedules;
  }

  @Data
  public static class PlanDayItem {
    private String title;
    private String startAt; 
    private String endAt; 
    private String placeName;
    private String address;
    private double lat;
    private double lng;
    private BigDecimal expectedCost;
  }
}
