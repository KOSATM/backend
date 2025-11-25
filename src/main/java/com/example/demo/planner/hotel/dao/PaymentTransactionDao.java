package com.example.demo.planner.hotel.dao;

import org.apache.ibatis.annotations.Mapper;
import com.example.demo.planner.hotel.dto.entity.PaymentTransaction;
import com.example.demo.planner.hotel.dto.request.PaymentTransactionRequest;

@Mapper
public interface PaymentTransactionDao {
    PaymentTransaction selectPaymentTransactionById(Long id);
    void insertPaymentTransaction(PaymentTransactionRequest request);
    void updatePaymentTransaction(PaymentTransactionRequest request);
    void deletePaymentTransactionById(Long id);
}
