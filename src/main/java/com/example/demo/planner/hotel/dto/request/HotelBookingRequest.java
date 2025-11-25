package com.example.demo.planner.hotel.dto.request;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
public class HotelBookingRequest {
    private Long id;
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
}
