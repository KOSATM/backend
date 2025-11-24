package com.example.demo.planner.controller;

import com.example.demo.planner.dto.HotelBooking;
import com.example.demo.planner.service.HotelBookingService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/hotel-bookings")
public class HotelBookingController {
    
    @Autowired
    private HotelBookingService hotelBookingService;
    
    // 호텔 예약 저장
    @PostMapping
    public ResponseEntity<Map<String, Object>> createHotelBooking(@RequestBody() HotelBooking hotelBooking) {
        Long bookingId = hotelBookingService.saveHotelBooking(hotelBooking);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "호텔 예약이 저장되었습니다.");
        response.put("bookingId", bookingId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    // 호텔 예약 조회
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getHotelBooking(@PathVariable("id") Long id) {
        HotelBooking booking = hotelBookingService.getHotelBooking(id);
        
        Map<String, Object> response = new HashMap<>();
        if (booking != null) {
            response.put("success", true);
            response.put("data", booking);
        } else {
            response.put("success", false);
            response.put("message", "예약을 찾을 수 없습니다.");
        }
        
        return ResponseEntity.ok(response);
    }
    
    // 호텔 예약 업데이트
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateHotelBooking(@PathVariable("id") Long id, @RequestBody HotelBooking hotelBooking) {
        hotelBooking.setId(id);
        hotelBookingService.updateHotelBooking(hotelBooking);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "호텔 예약이 업데이트되었습니다.");
        
        return ResponseEntity.ok(response);
    }
    
    // 호텔 예약 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteHotelBooking(@PathVariable("id") Long id) {
        hotelBookingService.deleteHotelBooking(id);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "호텔 예약이 삭제되었습니다.");
        
        return ResponseEntity.ok(response);
    }
}
