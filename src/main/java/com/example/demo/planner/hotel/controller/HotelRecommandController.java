package com.example.demo.planner.hotel.controller;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.planner.hotel.agent.HotelBookingAgent;
import com.example.demo.planner.hotel.dto.entity.HotelRatePlanCandidate;
import com.example.demo.planner.hotel.dto.request.HotelBookingRequest;
import com.example.demo.planner.hotel.dto.request.TripPlanRequest;
import com.example.demo.planner.hotel.service.HotelCandidateService;

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
            
            // 고객 친화적 요약 정보
            Map<String, Object> hotelSummary = new HashMap<>();
            hotelSummary.put("nights", recommendation.getNights() + "박");
            hotelSummary.put("checkInDate", recommendation.getCheckinDate().toLocalDate());
            hotelSummary.put("checkOutDate", recommendation.getCheckoutDate().toLocalDate());
            hotelSummary.put("guests", recommendation.getAdultsCount() + "명");
            if (recommendation.getChildrenCount() > 0) {
                hotelSummary.put("children", recommendation.getChildrenCount() + "명");
            }
            
            // 호텔 정보 (providerBookingMeta에서 추출)
            if (recommendation.getProviderBookingMeta() != null) {
                hotelSummary.put("hotelInfo", recommendation.getProviderBookingMeta());
            }
            hotelSummary.put("hotelId", recommendation.getHotelId());
            hotelSummary.put("roomTypeId", recommendation.getRoomTypeId());
            
            // 가격 정보
            Map<String, Object> priceInfo = new HashMap<>();
            priceInfo.put("roomPrice", recommendation.getTotalPrice());
            priceInfo.put("tax", recommendation.getTaxAmount() != null ? recommendation.getTaxAmount() : 0);
            priceInfo.put("fee", recommendation.getFeeAmount() != null ? recommendation.getFeeAmount() : 0);
            long totalPrice = (recommendation.getTotalPrice() != null ? recommendation.getTotalPrice().longValue() : 0) +
                            (recommendation.getTaxAmount() != null ? recommendation.getTaxAmount().longValue() : 0) +
                            (recommendation.getFeeAmount() != null ? recommendation.getFeeAmount().longValue() : 0);
            priceInfo.put("totalPrice", totalPrice);
            priceInfo.put("currency", recommendation.getCurrency() != null ? recommendation.getCurrency() : "KRW");
            hotelSummary.put("pricing", priceInfo);
            
            response.put("summary", hotelSummary);
            response.put("message", "추천 숙소입니다. 예약을 진행하시겠습니까?");
            
            // 기존의 상세한 계약정보도 포함
            response.put("bookingDetails", recommendation);
            
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
