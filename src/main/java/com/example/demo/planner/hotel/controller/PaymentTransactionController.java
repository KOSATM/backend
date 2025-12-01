package com.example.demo.planner.hotel.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.planner.hotel.dto.request.PaymentTransactionRequest;
import com.example.demo.planner.hotel.dto.response.PaymentTransactionResponse;
import com.example.demo.planner.hotel.service.PaymentTransactionService;

@RestController
@RequestMapping("/api/payment-transactions")
public class PaymentTransactionController {
    
    @Autowired
    private PaymentTransactionService paymentTransactionService;
    
    @PostMapping
    public ResponseEntity<Map<String, Object>> createPaymentTransaction(@RequestBody PaymentTransactionRequest request) {
        Long transactionId = paymentTransactionService.savePaymentTransaction(request);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "결제 정보가 저장되었습니다.");
        response.put("transactionId", transactionId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getPaymentTransaction(@PathVariable("id") Long id) {
        PaymentTransactionResponse transaction = paymentTransactionService.getPaymentTransaction(id);
        
        Map<String, Object> response = new HashMap<>();
        if (transaction != null) {
            response.put("success", true);
            response.put("data", transaction);
        } else {
            response.put("success", false);
            response.put("message", "결제 정보를 찾을 수 없습니다.");
        }
        
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updatePaymentTransaction(@PathVariable("id") Long id, @RequestBody PaymentTransactionRequest request) {
        paymentTransactionService.updatePaymentTransaction(id, request);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "결제 정보가 업데이트되었습니다.");
        
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deletePaymentTransaction(@PathVariable("id") Long id) {
        paymentTransactionService.deletePaymentTransaction(id);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "결제 정보가 삭제되었습니다.");
        
        return ResponseEntity.ok(response);
    }
}
