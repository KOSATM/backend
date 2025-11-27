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
    
    // 객실 정보
    private String roomTypeName;  // 디럭스, 스위트 등
    private String bedType;       // 침대 타입
    
    // 요금제 정보
    private String ratePlanName;  // 요금제명
    private String mealPlan;      // 식사 플랜
    private Boolean includesBreakfast; // 조식 포함 여부

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
