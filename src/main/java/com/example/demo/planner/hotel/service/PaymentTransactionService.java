package com.example.demo.planner.hotel.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.demo.planner.hotel.dao.PaymentTransactionDao;
import com.example.demo.planner.hotel.dto.entity.PaymentTransaction;

@Service
public class PaymentTransactionService {
    
    @Autowired
    private PaymentTransactionDao paymentTransactionDao;
    
    public Long savePaymentTransaction(PaymentTransaction paymentTransaction) {
        paymentTransactionDao.insertPaymentTransaction(paymentTransaction);
        return paymentTransaction.getId();
    }
    
    public PaymentTransaction getPaymentTransaction(Long id) {
        return paymentTransactionDao.selectPaymentTransactionById(id);
    }
    
    public void updatePaymentTransaction(PaymentTransaction paymentTransaction) {
        paymentTransactionDao.updatePaymentTransaction(paymentTransaction);
    }
    
    public void deletePaymentTransaction(Long id) {
        paymentTransactionDao.deletePaymentTransactionById(id);
    }
}
