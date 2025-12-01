package com.example.demo.planner.hotel.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class TripPlanRequest {
    // 여행 정보 json을 받아오는 request dto 차후 입력 데이터 구조가 바뀌면 수정 필요

    private Long userId;
    private BigDecimal budget;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
    
    private String preferences;  // 사용자 요청사항

    private List<TripDayRequest> days;

    @Data
    public static class TripDayRequest {
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate date;
        private String title;
        private List<TripScheduleRequest> schedules;
    }

    @Data
    public static class TripScheduleRequest {
        private String title;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime startAt;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime endAt;

        private String placeName;
        private String address;
        private double lat;
        private double lng;
        private long expectedCost;
    }
}
