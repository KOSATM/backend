package com.example.demo.planner.hotel.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.demo.planner.hotel.dao.PaymentTransactionDao;
import com.example.demo.planner.hotel.dto.entity.PaymentTransaction;
import com.example.demo.planner.hotel.dto.request.PaymentTransactionRequest;
import com.example.demo.planner.hotel.dto.response.PaymentTransactionResponse;

@Service
public class PaymentTransactionService {
    
    @Autowired
    private PaymentTransactionDao paymentTransactionDao;
    
    // Create
    public Long savePaymentTransaction(PaymentTransactionRequest request) {
        PaymentTransactionRequest paymentTransaction = PaymentTransactionRequest.builder()
            .hotelBookingId(request.getHotelBookingId())
            .paymentMethod(request.getPaymentMethod())
            .providerPaymentId(request.getProviderPaymentId())
            .amount(request.getAmount())
            .currency(request.getCurrency())
            .status(request.getStatus())
            .requestedAt(request.getRequestedAt())
            .completedAt(request.getCompletedAt())
            .cancelledAt(request.getCancelledAt())
            .rawResponse(request.getRawResponse())
            .build();
        paymentTransactionDao.insertPaymentTransaction(paymentTransaction);
        return null;
    }
    
    // Read
    public PaymentTransactionResponse getPaymentTransaction(Long id) {
        PaymentTransaction paymentTransaction = paymentTransactionDao.selectPaymentTransactionById(id);
        if (paymentTransaction == null) {
            return null;
        }
        return new PaymentTransactionResponse(
            paymentTransaction.getId(), paymentTransaction.getHotelBookingId(), paymentTransaction.getPaymentMethod(),
            paymentTransaction.getProviderPaymentId(), paymentTransaction.getAmount(), paymentTransaction.getCurrency(),
            paymentTransaction.getStatus(), paymentTransaction.getRequestedAt(), paymentTransaction.getCompletedAt(),
            paymentTransaction.getCancelledAt(), paymentTransaction.getRawResponse(), paymentTransaction.getCreatedAt(),
            paymentTransaction.getUpdatedAt()
        );
    }
    
    // Update
    public void updatePaymentTransaction(Long id, PaymentTransactionRequest request) {
        PaymentTransactionRequest paymentTransaction = PaymentTransactionRequest.builder()
            .hotelBookingId(request.getHotelBookingId())
            .paymentMethod(request.getPaymentMethod())
            .providerPaymentId(request.getProviderPaymentId())
            .amount(request.getAmount())
            .currency(request.getCurrency())
            .status(request.getStatus())
            .requestedAt(request.getRequestedAt())
            .completedAt(request.getCompletedAt())
            .cancelledAt(request.getCancelledAt())
            .rawResponse(request.getRawResponse())
            .build();
        paymentTransactionDao.updatePaymentTransaction(paymentTransaction);
    }
    
    // Delete
    public void deletePaymentTransaction(Long id) {
        paymentTransactionDao.deletePaymentTransactionById(id);
    }
}
