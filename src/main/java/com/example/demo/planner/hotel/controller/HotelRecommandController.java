package com.example.demo.planner.hotel.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
     * userId로 활성 여행 계획을 조회하고 LLM이 추천하는 호텔을 반환한다.
     */
    @GetMapping("/recommend")
    public Map<String, Object> recommendHotel(
            @RequestParam(name = "userId") Long userId,
            @RequestParam(name = "preferences", required = false) String userPreferences) {
        
        log.info("🔍 Hotel recommendation request - userId: {}, preferences: {}", userId, userPreferences);
        
        int adults = 2;
        int children = 0;
        String guestName = "Guest";
        String guestEmail = "guest@example.com";
        String guestPhone = "+82-10-0000-0000";
        
        // userPreferences가 null이면 빈 문자열로 처리
        String preferences = userPreferences != null ? userPreferences : "";
        
        try {
            // TripPlanRequest는 더미 객체 (userId만 필요)
            TripPlanRequest tripPlan = new TripPlanRequest();
            tripPlan.setUserId(userId);
            
            List<HotelBookingRequest> recommendations = hotelBookingAgent.createBookingFromItinerary(
                userId,
                tripPlan,
                adults,
                children,
                guestName,
                guestEmail,
                guestPhone,
                preferences
            );
            
            if (recommendations == null || recommendations.isEmpty()) {
                log.warn("⚠️ No hotels available for the given dates");
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "해당 날짜에 예약 가능한 호텔이 없습니다.");
                return response;
            }
            
            log.info("✅ Hotel recommendation successful - {} hotels", recommendations.size());
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "추천 숙소입니다. 예약을 진행하시겠습니까?");
            response.put("totalCount", recommendations.size());
            
            // 3개의 호텔 추천 정보
            List<Map<String, Object>> hotelSummaryList = new java.util.ArrayList<>();
            List<Map<String, Object>> bookingDataList = new java.util.ArrayList<>();
            
            for (int i = 0; i < recommendations.size(); i++) {
                HotelBookingRequest recommendation = recommendations.get(i);
                
                // 각 호텔의 요약 정보
                Map<String, Object> hotelSummary = new HashMap<>();
                hotelSummary.put("rank", i + 1);
                hotelSummary.put("hotelName", recommendation.getHotelName());
                hotelSummary.put("neighborhood", recommendation.getNeighborhood());
                hotelSummary.put("roomTypeName", recommendation.getRoomTypeName());
                hotelSummary.put("nights", recommendation.getNights() + "박");
                hotelSummary.put("checkInDate", recommendation.getCheckinDate().toLocalDate());
                hotelSummary.put("checkOutDate", recommendation.getCheckoutDate().toLocalDate());
                hotelSummary.put("guests", recommendation.getAdultsCount() + "명");
                if (recommendation.getChildrenCount() > 0) {
                    hotelSummary.put("children", recommendation.getChildrenCount() + "명");
                }
                
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
                
                // 호텔 편의시설
                Map<String, Object> facilities = new HashMap<>();
                if (recommendation.getHasFreeWifi() != null) {
                    facilities.put("WiFi", recommendation.getHasFreeWifi() ? "있음" : "없음");
                }
                if (recommendation.getHasParking() != null) {
                    facilities.put("주차", recommendation.getHasParking() ? "있음" : "없음");
                }
                if (recommendation.getIsPetFriendly() != null) {
                    facilities.put("반려동물", recommendation.getIsPetFriendly() ? "허용" : "불허");
                }
                if (recommendation.getIsFamilyFriendly() != null) {
                    facilities.put("가족친화", recommendation.getIsFamilyFriendly() ? "예" : "아니오");
                }
                if (recommendation.getHas24hFrontdesk() != null) {
                    facilities.put("24시간프론트", recommendation.getHas24hFrontdesk() ? "있음" : "없음");
                }
                if (recommendation.getNearMetro() != null && recommendation.getNearMetro()) {
                    facilities.put("지하철", "근처 (" + (recommendation.getMetroStationName() != null ? recommendation.getMetroStationName() : "") + ")");
                }
                if (recommendation.getAirportDistanceKm() != null) {
                    facilities.put("공항거리", recommendation.getAirportDistanceKm() + " km");
                }
                if (!facilities.isEmpty()) {
                    hotelSummary.put("facilities", facilities);
                }
                
                hotelSummaryList.add(hotelSummary);
                
                // 각 호텔의 booking 데이터 (DB 저장용)
                Map<String, Object> bookingData = new HashMap<>();
                bookingData.put("userId", recommendation.getUserId());
                bookingData.put("externalBookingId", recommendation.getExternalBookingId());
                bookingData.put("hotelId", recommendation.getHotelId());
                bookingData.put("roomTypeId", recommendation.getRoomTypeId());
                bookingData.put("ratePlanId", recommendation.getRatePlanId());
                bookingData.put("checkinDate", recommendation.getCheckinDate());
                bookingData.put("checkoutDate", recommendation.getCheckoutDate());
                bookingData.put("nights", recommendation.getNights());
                bookingData.put("adultsCount", recommendation.getAdultsCount());
                bookingData.put("childrenCount", recommendation.getChildrenCount());
                bookingData.put("currency", recommendation.getCurrency());
                bookingData.put("totalPrice", recommendation.getTotalPrice());
                bookingData.put("taxAmount", recommendation.getTaxAmount());
                bookingData.put("feeAmount", recommendation.getFeeAmount());
                bookingData.put("status", recommendation.getStatus());
                bookingData.put("paymentStatus", recommendation.getPaymentStatus());
                bookingData.put("guestName", recommendation.getGuestName());
                bookingData.put("guestEmail", recommendation.getGuestEmail());
                bookingData.put("guestPhone", recommendation.getGuestPhone());
                bookingData.put("providerBookingMeta", recommendation.getProviderBookingMeta());
                bookingData.put("bookedAt", recommendation.getBookedAt());
                bookingData.put("cancelledAt", recommendation.getCancelledAt());
                
                bookingDataList.add(bookingData);
            }
            
            response.put("hotelSummaryList", hotelSummaryList);
            response.put("bookingDataList", bookingDataList);
            
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
