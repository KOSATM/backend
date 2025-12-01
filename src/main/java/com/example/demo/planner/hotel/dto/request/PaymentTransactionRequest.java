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
public class PaymentTransactionRequest {
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
}
