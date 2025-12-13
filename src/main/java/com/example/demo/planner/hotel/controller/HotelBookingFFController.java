package com.example.demo.planner.hotel.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.planner.hotel.dto.entity.HotelBookingFF.HotelBookingFFResponse
;
import com.example.demo.planner.hotel.service.HotelBookingFFService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/hotel-ff")
@RequiredArgsConstructor
public class HotelBookingFFController {

    private final HotelBookingFFService hotelBookingFFService;

    /**
     * 특정 유저의 호텔 예약 조회
     * GET /api/hotel-bookings/{userId}
     */
    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> getHotelBooking(@PathVariable("userId") Long userId) {
        Map<String, Object> response = new HashMap<>();

        try {
            HotelBookingFFResponse booking = hotelBookingFFService.getBookingByUserId(userId);

            response.put("success", true);
            response.put("data", booking);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 특정 유저의 호텔 예약 생성 또는 수정 (Upsert)
     * PUT /api/hotel-bookings/{userId}
     */
    @PutMapping("insert/{userId}")
    public ResponseEntity<Map<String, Object>> saveOrUpdateHotelBooking(
            @PathVariable("userId") Long userId,
            @RequestBody HotelBookingFFResponse request
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            // path의 userId를 강제 주입
            request.setUserId(userId);

            HotelBookingFFResponse saved = hotelBookingFFService.saveOrUpdateBooking(request);

            response.put("success", true);
            response.put("data", saved);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException | IllegalStateException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 예약 삭제 (PK 기준)
     * DELETE /api/hotel-bookings/id/{id}
     */
    @DeleteMapping("/id/{id}")
    public ResponseEntity<Map<String, Object>> deleteHotelBooking(@PathVariable("id") Long id) {
        Map<String, Object> response = new HashMap<>();

        try {
            hotelBookingFFService.deleteBookingById(id);

            response.put("success", true);
            response.put("message", "예약이 삭제되었습니다.");
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
}
