package com.example.demo.planner.hotel.agent;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.demo.planner.hotel.dto.entity.HotelRatePlanCandidate;
import com.example.demo.planner.hotel.dto.request.HotelBookingRequest;
import com.example.demo.planner.hotel.dto.request.TripPlanRequest;
import com.example.demo.planner.hotel.service.HotelCandidateService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class HotelBookingAgent {

    private ChatClient chatClient;
    private HotelCandidateService hotelCandidateService;
    private ObjectMapper objectMapper;
    private List<HotelRatePlanCandidate> candidates;

    @Autowired
    public HotelBookingAgent(
            ChatClient.Builder chatClientBuilder,
            HotelCandidateService hotelCandidateService,
            ObjectMapper objectMapper
    ) {
        this.chatClient = chatClientBuilder.build();
        this.hotelCandidateService = hotelCandidateService;
        this.objectMapper = objectMapper;
    }

    public List<HotelBookingRequest> createBookingFromItinerary(
            TripPlanRequest tripPlan,
            int adults,
            int children,
            String guestName,
            String guestEmail,
            String guestPhone,
            String userPreferences
    ) {
        try {
            LocalDate startDate = tripPlan.getStartDate();
            LocalDate endDate = tripPlan.getEndDate();
            long nights = ChronoUnit.DAYS.between(startDate, endDate);

            OffsetDateTime checkin = startDate.atStartOfDay().atOffset(ZoneOffset.ofHours(9));
            OffsetDateTime checkout = endDate.atStartOfDay().atOffset(ZoneOffset.ofHours(9));

            log.info("HotelBookingAgent - stay {} ~ {} ({} nights)", checkin, checkout, nights);

            // 1) DB 후보 조회
            log.info("🔍 Querying DB with: checkinDate={}, checkoutDate={}, adults={}, children={}", 
                checkin.toLocalDate(), checkout.toLocalDate(), adults, children);
            
            this.candidates = hotelCandidateService.findCandidates(checkin, checkout, adults, children);

            if (candidates == null || candidates.isEmpty()) {
                log.warn("HotelBookingAgent - no candidates for given itinerary");
                return null;
            }

            log.info("📊 Found {} hotel candidates from DB", candidates.size());

            // 2) LLM으로 호텔 선택 (Tool 사용)
            log.info("🤖 Calling LLM to select top 3 hotels...");
            String llmResponse = chatClient.prompt()
                    .system("""
                        사용자의 여행 일정에 맞는 호텔 3개를 추천하세요.
                        반드시 사용자의 요청사항을 만족하는 호텔만 선택하세요.
                        
                        선택 기준:
                        1. 사용자 요청사항 필수 만족
                        2. 거리가 가까운 호텔
                        3. 가격이 합리적
                        4. 평점이 높음
                        
                        getHotelCandidates 도구를 사용해서 호텔 목록을 조회하고 3개를 선택하세요.
                        선택한 호텔의 hotelId, roomTypeId, ratePlanId를 JSON 형식으로 반환하세요:
                        [
                          {"hotelId": 1, "roomTypeId": 2, "ratePlanId": 2},
                          ...
                        ]
                        """)
                    .user("여행 일정: " + startDate + " ~ " + endDate + 
                          (userPreferences != null && !userPreferences.isEmpty() ? 
                           "\n사용자 요청사항: " + userPreferences : ""))
                    .tools(new HotelSelectionTools())
                    .call()
                    .content();

            log.info("📝 LLM Response: {}", llmResponse);

            // 3) LLM이 선택한 호텔ID 파싱
            List<HotelBookingRequest> selectedHotels = parseSelectedHotels(llmResponse, candidates);
            
            if (selectedHotels == null || selectedHotels.isEmpty()) {
                log.warn("LLM selected no hotels");
                return null;
            }

            // 4) 선택된 호텔 정보 채우기
            List<HotelBookingRequest> bookingRequests = buildBookingRequests(
                selectedHotels, tripPlan, adults, children, guestName, guestEmail, guestPhone, 
                checkin, checkout, nights
            );

            return bookingRequests;

        } catch (Exception e) {
            log.error("HotelBookingAgent error", e);
            throw new RuntimeException("Failed to create booking from itinerary", e);
        }
    }

    private List<HotelBookingRequest> parseSelectedHotels(
            String llmResponse, 
            List<HotelRatePlanCandidate> candidates
    ) {
        try {
            // JSON 배열 추출
            String cleanJson = llmResponse
                    .replaceAll("```json\\s*", "")
                    .replaceAll("```\\s*", "")
                    .replaceAll("```", "")
                    .trim();
            
            int startIdx = cleanJson.indexOf('[');
            int endIdx = cleanJson.lastIndexOf(']');
            
            if (startIdx >= 0 && endIdx > startIdx) {
                cleanJson = cleanJson.substring(startIdx, endIdx + 1);
            }
            
            // LLM이 선택한 호텔 ID들 파싱
            @SuppressWarnings("unchecked")
            java.util.List<java.util.Map<String, Object>> selectedIds = 
                objectMapper.readValue(cleanJson, java.util.List.class);
            
            List<HotelRatePlanCandidate> selectedCandidates = new java.util.ArrayList<>();
            
            for (java.util.Map<String, Object> selected : selectedIds) {
                long hotelId = ((Number) selected.get("hotelId")).longValue();
                long roomTypeId = ((Number) selected.get("roomTypeId")).longValue();
                long ratePlanId = ((Number) selected.get("ratePlanId")).longValue();
                
                HotelRatePlanCandidate found = candidates.stream()
                    .filter(c -> c.getHotelId().equals(hotelId) &&
                               c.getRoomTypeId().equals(roomTypeId) &&
                               c.getRatePlanId().equals(ratePlanId))
                    .findFirst()
                    .orElse(null);
                
                if (found != null) {
                    selectedCandidates.add(found);
                    log.info("✅ Selected hotel: id={}, name={}", hotelId, found.getHotelName());
                }
            }
            
            // HotelBookingRequest로 변환 (임시)
            List<HotelBookingRequest> result = new java.util.ArrayList<>();
            for (HotelRatePlanCandidate candidate : selectedCandidates) {
                HotelBookingRequest req = new HotelBookingRequest();
                req.setHotelId(candidate.getHotelId());
                req.setRoomTypeId(candidate.getRoomTypeId());
                req.setRatePlanId(candidate.getRatePlanId());
                result.add(req);
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("Error parsing selected hotels", e);
            return null;
        }
    }

    private List<HotelBookingRequest> buildBookingRequests(
            List<HotelBookingRequest> selectedBookings,
            TripPlanRequest tripPlan,
            int adults,
            int children,
            String guestName,
            String guestEmail,
            String guestPhone,
            OffsetDateTime checkin,
            OffsetDateTime checkout,
            long nights
    ) {
        List<HotelBookingRequest> bookingRequests = new java.util.ArrayList<>();

        for (HotelBookingRequest selected : selectedBookings) {
            // 선택된 호텔의 전체 정보 찾기
            HotelRatePlanCandidate candidate = candidates.stream()
                .filter(c -> c.getHotelId().equals(selected.getHotelId()) &&
                           c.getRoomTypeId().equals(selected.getRoomTypeId()) &&
                           c.getRatePlanId().equals(selected.getRatePlanId()))
                .findFirst()
                .orElse(null);
            
            if (candidate == null) {
                continue;
            }

            HotelBookingRequest booking = new HotelBookingRequest();
            booking.setUserId(tripPlan.getUserId());
            booking.setHotelId(candidate.getHotelId());
            booking.setRoomTypeId(candidate.getRoomTypeId());
            booking.setRatePlanId(candidate.getRatePlanId());
            booking.setCheckinDate(checkin);
            booking.setCheckoutDate(checkout);
            booking.setNights((int) nights);
            booking.setAdultsCount(adults);
            booking.setChildrenCount(children);
            booking.setCurrency(candidate.getCurrency());
            booking.setTotalPrice(candidate.getTotalPrice());
            booking.setTaxAmount(candidate.getTaxAmount());
            booking.setFeeAmount(candidate.getFeeAmount());
            booking.setStatus("PENDING");
            booking.setPaymentStatus("PENDING");
            booking.setGuestName(guestName);
            booking.setGuestEmail(guestEmail);
            booking.setGuestPhone(guestPhone);
            booking.setBookedAt(checkin);

            // 호텔 정보 추가
            String hotelDetail = "호텔: " + candidate.getHotelName() + 
                                " | 객실: " + candidate.getRoomTypeName() +
                                " | 침대: " + candidate.getBedType() +
                                " | 요금제: " + candidate.getRatePlanName() +
                                (candidate.getIncludesBreakfast() != null && candidate.getIncludesBreakfast() ? 
                                 " | 조식: 포함" : "");
            booking.setProviderBookingMeta(hotelDetail);
            booking.setHotelName(candidate.getHotelName());
            booking.setNeighborhood(candidate.getNeighborhood());
            booking.setRoomTypeName(candidate.getRoomTypeName());
            booking.setRatePlanName(candidate.getRatePlanName());
            booking.setHasFreeWifi(candidate.getHasFreeWifi());
            booking.setHasParking(candidate.getHasParking());
            booking.setIsPetFriendly(candidate.getIsPetFriendly());
            booking.setIsFamilyFriendly(candidate.getIsFamilyFriendly());
            booking.setHas24hFrontdesk(candidate.getHas24hFrontdesk());
            booking.setNearMetro(candidate.getNearMetro());
            booking.setMetroStationName(candidate.getMetroStationName());
            booking.setAirportDistanceKm(candidate.getAirportDistanceKm());

            bookingRequests.add(booking);
        }

        return bookingRequests;
    }

    class HotelSelectionTools {
        
        @Tool(description = "호텔 후보 목록을 조회합니다")
        public String getHotelCandidates() {
            try {
                return objectMapper.writeValueAsString(candidates);
            } catch (Exception e) {
                return "[]";
            }
        }
    }
}

