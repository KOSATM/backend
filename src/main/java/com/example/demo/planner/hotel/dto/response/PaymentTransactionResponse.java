package com.example.demo.planner.hotel.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import lombok.Getter;
import lombok.AllArgsConstructor;

@Getter
@AllArgsConstructor
public class PaymentTransactionResponse {
    private Long id;
    private Long hotelBookingId;
    private String paymentMethod;
    private String providerPaymentId;
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
