package com.example.demo.planner.travel.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import lombok.Data;

@Data
public class TravelPlanSnapshotContent {
  private Long userId;
  private Double budget;
  private String startDate;
  private String endDate;
  private List<TravelDay> days;

  @Data
  public static class TravelDay {
    private String date;
    private String title;
    private List<TravelItem> schedules;
  }

  @Data
  public static class TravelItem {
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
