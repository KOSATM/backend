package com.example.demo.supporter.checklist.dto.response;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import lombok.Data;

@Data
public class TravelDayResponse {
    private Long dayId;
    private Long planId;
    private Long userId;
    private Integer dayIndex;
    private String dayTitle;
    private LocalDate planDate;
    private List<PlaceDto> places;

    @Data
    public static class PlaceDto {
        private Long placeId;
        private String placeTitle;
        private OffsetDateTime startAt;
        private OffsetDateTime endAt;
        private String placeName;
        private String address;
        private Double lat;
        private Double lng;
        private Long expectedCost;
    }
}
