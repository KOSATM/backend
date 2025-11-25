package com.example.demo.planner.hotel.dao;

import org.apache.ibatis.annotations.Mapper;
import com.example.demo.planner.hotel.dto.entity.PaymentTransaction;

@Mapper
public interface PaymentTransactionDao {
    PaymentTransaction selectPaymentTransactionById(Long id);
    void insertPaymentTransaction(PaymentTransaction paymentTransaction);
    void updatePaymentTransaction(PaymentTransaction paymentTransaction);
    void deletePaymentTransactionById(Long id);
}
