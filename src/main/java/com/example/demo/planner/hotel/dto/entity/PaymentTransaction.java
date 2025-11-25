package com.example.demo.planner.hotel.dto.entity;

import java.time.OffsetDateTime;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class PaymentTransaction {
    private Long id;
    private Long hotelBookingId;
    private String paymentMethod;
    private String providerPaymentId; // 이거 Id로 할거면 Long으로 바꾸고 name으로 할거면 String..
    private BigDecimal amount;
    private String currency;
    private String status;
    private OffsetDateTime requestedAt;
    private OffsetDateTime completedAt;
    private OffsetDateTime cancelledAt;
    private String rawResponse;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
