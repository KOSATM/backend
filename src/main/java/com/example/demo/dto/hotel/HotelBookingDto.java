package com.example.demo.dto.hotel;

import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class HotelBookingDto {
    private Long id;
    private Long userId;
    private String externalBookingId;
    private Long hotelId;
    private Long roomTypeId;
    private Long ratePlanId;
    private LocalDate checkinDate;
    private LocalDate checkoutDate;
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
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
