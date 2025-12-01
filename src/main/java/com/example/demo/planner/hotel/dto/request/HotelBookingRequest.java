package com.example.demo.planner.hotel.dto.request;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotelBookingRequest {
    private Long userId;
    private String externalBookingId;
    private Long hotelId;
    private Long roomTypeId;
    private Long ratePlanId;
    private OffsetDateTime checkinDate;
    private OffsetDateTime checkoutDate;
    private Integer nights;
    private Integer adultsCount;
    private Integer childrenCount;
    private String currency;
    private BigDecimal totalPrice;
    private BigDecimal taxAmount;
    private BigDecimal feeAmount;
    private String status;
    private String paymentStatus;
    private String guestName;
    private String guestEmail;
    private String guestPhone;
    private String providerBookingMeta;
    private OffsetDateTime bookedAt;
    private OffsetDateTime cancelledAt;
    
    // 호텔 정보
    private String hotelName;
    private String neighborhood;
    private String roomTypeName;
    private String ratePlanName;
    
    // 호텔 편의시설
    private Boolean hasFreeWifi;
    private Boolean hasParking;
    private Boolean isPetFriendly;
    private Boolean isFamilyFriendly;
    private Boolean has24hFrontdesk;
    private Boolean nearMetro;
    private String metroStationName;
    private BigDecimal airportDistanceKm;
}
