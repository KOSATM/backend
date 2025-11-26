package com.example.demo.planner.hotel.dto.entity;

import java.math.BigDecimal;

import lombok.Getter;

@Getter
public class HotelRatePlanCandidate {

    private Long hotelId;
    private Long roomTypeId;
    private Long ratePlanId;

    private String hotelName;
    private String hotelNameLocal;

    private String district;      // Jung-gu 등
    private String neighborhood;  // Myeong-dong 등
    private String addressLine1;

    private Double latitude;
    private Double longitude;

    private BigDecimal starRating;
    private BigDecimal ratingScore;
    private Integer reviewCount;

    private BigDecimal totalPrice;   // 전체 숙박 금액
    private BigDecimal taxAmount;
    private BigDecimal feeAmount;
    private String currency;        // KRW
}
