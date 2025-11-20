package com.example.demo.dto.payment;

import java.time.OffsetDateTime;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class PaymentTransaction {
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
