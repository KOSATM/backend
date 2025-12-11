package com.example.demo.planner.plan.dto.response;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

@Data
public class PlanSnapshotContent {
  private Long userId;
  private BigDecimal budget;
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
    private Double lat;
    private Double lng;
    private BigDecimal expectedCost;
    private String normalizedCategory;
    private String firstImage;
    private String firstImage2;
  }

  // public Plan toPlanEntity(List<DayPlanResult> dayPlans) {
  // Plan plan = new Plan();
  // plan.setUserId(userId);
  // plan.setBudget(budget);
  // plan.setStartDate(LocalDate.parse(startDate));
  // plan.setEndDate(LocalDate.parse(endDate));
  // plan.setIsEnded(false);
  // plan.setTitle("여행 계획"); // 또는 별도 생성
  // return plan;
  // }

  // public List<PlanDay> toPlanDayEntities(Long planId) {
  // List<PlanDay> list = new ArrayList<>();
  // for (PlanDay d : days) {
  // PlanDay entity = new PlanDay();
  // entity.setPlanId(planId);
  // entity.setDayIndex(/* index 계산 */);
  // entity.setPlanDate(LocalDate.parse(d.getDate()));
  // entity.setTitle(d.getTitle());
  // list.add(entity);
  // }
  // return list;
  // }

  // public List<PlanPlace> toPlanPlaceEntities(Map<String, Long> dayIdMap) {
  // List<PlanPlace> list = new ArrayList<>();
  // for (PlanDay d : days) {
  // Long dayId = dayIdMap.get(d.getDate());

  // for (PlanDayItem item : d.getSchedules()) {
  // PlanPlace place = new PlanPlace();
  // place.setDayId(dayId);
  // place.setTitle(item.getTitle());
  // place.setPlaceName(item.getPlaceName());
  // place.setAddress(item.getAddress());
  // place.setLat(item.getLat());
  // place.setLng(item.getLng());
  // place.setExpectedCost(item.getExpectedCost());
  // place.setStartAt(OffsetDateTime.parse(item.getStartAt()));
  // place.setEndAt(OffsetDateTime.parse(item.getEndAt()));
  // list.add(place);
  // }
  // }
  // return list;
  // }
}
