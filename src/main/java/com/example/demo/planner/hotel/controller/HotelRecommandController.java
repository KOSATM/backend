package com.example.demo.planner.hotel.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.planner.hotel.agent.HotelBookingAgent;
import com.example.demo.planner.hotel.dto.request.HotelBookingRequest;
import com.example.demo.planner.hotel.dto.request.TripPlanRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/hotel")
@Slf4j
public class HotelRecommandController {

    private final HotelBookingAgent hotelBookingAgent;

    /**
     * 여행 일정을 받아서 LLM이 추천하는 호텔을 반환한다.
     */
    @PostMapping("/recommend")
    public Map<String, Object> recommendHotel(@RequestBody TripPlanRequest tripPlan) {
        log.info("🔍 Hotel recommendation request for trip: {} to {}", 
            tripPlan.getStartDate(), tripPlan.getEndDate());
        
        int adults = 2;
        int children = 0;
        String guestName = "Guest";
        String guestEmail = "guest@example.com";
        String guestPhone = "+82-10-0000-0000";
        
        try {
            HotelBookingRequest recommendation = hotelBookingAgent.createBookingFromItinerary(
                tripPlan,
                adults,
                children,
                guestName,
                guestEmail,
                guestPhone
            );
            
            if (recommendation == null) {
                log.warn("⚠️ No hotels available for the given dates");
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "해당 날짜에 예약 가능한 호텔이 없습니다.");
                return response;
            }
            
            log.info("✅ Hotel recommendation successful");
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("recommendation", recommendation);
            return response;
            
        } catch (Exception e) {
            log.error("❌ Error during hotel recommendation", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "호텔 추천 중 오류가 발생했습니다: " + e.getMessage());
            return response;
        }
    }
}
