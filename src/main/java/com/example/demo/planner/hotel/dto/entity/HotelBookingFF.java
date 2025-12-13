package com.example.demo.planner.hotel.dto.entity;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;

public class HotelBookingFF {

    @Data
    public static class HotelBookingFFResponse {
        private Long id;
        private Long userId;

        private String hotelName;
        private String roomType;
        private LocalDate checkinDate;
        private LocalDate checkoutDate;

        @JsonIgnore
        private OffsetDateTime createdAt;
    }

}
