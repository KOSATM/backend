package com.example.demo.planner.hotel.agent;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
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

    private final ChatClient chatClient;
    private final HotelCandidateService hotelCandidateService;
    private final ObjectMapper objectMapper;

    // 🔴 여기가 진짜 중요: 이 생성자 하나만 존재해야 함
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

    public HotelBookingRequest createBookingFromItinerary(
            TripPlanRequest tripPlan,
            int adults,
            int children,
            String guestName,
            String guestEmail,
            String guestPhone
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
            List<HotelRatePlanCandidate> candidates =
                    hotelCandidateService.findCandidates(checkin, checkout, adults, children);

            if (candidates == null || candidates.isEmpty()) {
                log.warn("HotelBookingAgent - no candidates for given itinerary");
                return null;
            }

            log.info("📊 Found {} hotel candidates from DB", candidates.size());

            // 로깅: DB 조회 데이터 확인
            log.info("🏨 Found {} candidates from DB", candidates.size());
            if (candidates.isEmpty()) {
                throw new RuntimeException("No hotel candidates found");
            }
            
            // 첫 번째 호텔 정보 로깅
            HotelRatePlanCandidate firstHotel = candidates.get(0);
            log.info("🏨 First hotel: id={}, name={}, price={}, lat={}, lng={}", 
                firstHotel.getHotelId(), 
                firstHotel.getHotelName(), 
                firstHotel.getTotalPrice(),
                firstHotel.getLatitude(),
                firstHotel.getLongitude());
            
            String candidatesJson = objectMapper.writeValueAsString(candidates);
            
            log.info("📋 Candidates JSON length: {} chars", candidatesJson.length());

            // 2) LLM 호출
            log.info("🤖 Calling LLM to select best hotel...");
            String llmResultJson = chatClient.prompt()
                    .system("""
                        주어진 호텔 후보 목록에서 하나를 선택해 JSON으로 반환하세요.
                        
                        선택 기준:
                        1. 거리: 여행 일정의 장소들과 가까운 호텔
                        2. 가격: 합리적인 가격
                        3. 평점: 높은 별점
                        
                        반환값: JSON 객체만 반환하세요 (마크다운 없음)
                        """)
                    .user(u -> u.text("""
                        candidates: """ + candidatesJson + """
                        
                        userId: """ + tripPlan.getUserId() + """
                        checkinDate: """ + checkin.toString() + """
                        checkoutDate: """ + checkout.toString() + """
                        nights: """ + nights + """
                        adultsCount: """ + adults + """
                        childrenCount: """ + children + """
                        guestName: """ + guestName + """
                        guestEmail: """ + guestEmail + """
                        guestPhone: """ + guestPhone + """
                        
                        이 정보를 이용해 candidates에서 하나를 선택하고 아래 JSON을 작성하세요:
                        {
                          "userId": <userId>,
                          "externalBookingId": null,
                          "hotelId": <선택한 호텔의 hotelId>,
                          "roomTypeId": <선택한 호텔의 roomTypeId>,
                          "ratePlanId": <선택한 호텔의 ratePlanId>,
                          "checkinDate": <checkinDate>,
                          "checkoutDate": <checkoutDate>,
                          "nights": <nights>,
                          "adultsCount": <adultsCount>,
                          "childrenCount": <childrenCount>,
                          "currency": "KRW",
                          "totalPrice": <선택한 호텔의 totalPrice>,
                          "taxAmount": <선택한 호텔의 taxAmount>,
                          "feeAmount": <선택한 호텔의 feeAmount>,
                          "status": "PENDING",
                          "paymentStatus": "PENDING",
                          "guestName": <guestName>,
                          "guestEmail": <guestEmail>,
                          "guestPhone": <guestPhone>,
                          "providerBookingMeta": "selected",
                          "bookedAt": <checkinDate>,
                          "cancelledAt": null
                        }
                        """))
                    .call()
                    .content();

            // 3) JSON → DTO
            log.info("📝 Raw LLM response: {}", llmResultJson);
            
            // 마크다운 코드블록 제거
            String cleanJson = llmResultJson
                    .replaceAll("```json\\s*", "")
                    .replaceAll("```\\s*", "")
                    .replaceAll("```", "")
                    .trim();
            
            log.info("🧹 Cleaned JSON: {}", cleanJson);
            
            HotelBookingRequest bookingRequest =
                    objectMapper.readValue(cleanJson, HotelBookingRequest.class);
            
            log.info("✅ Parsed hotel: hotelId={}, roomTypeId={}, ratePlanId={}", 
                bookingRequest.getHotelId(), 
                bookingRequest.getRoomTypeId(), 
                bookingRequest.getRatePlanId());

            log.info("✅ LLM selected hotel: id={}, ratePlan={}", 
                bookingRequest.getHotelId(), bookingRequest.getRatePlanId());
            
            // 선택된 호텔 정보 찾기
            HotelRatePlanCandidate selectedHotel = candidates.stream()
                .filter(h -> h.getHotelId().equals(bookingRequest.getHotelId()) &&
                           h.getRoomTypeId().equals(bookingRequest.getRoomTypeId()) &&
                           h.getRatePlanId().equals(bookingRequest.getRatePlanId()))
                .findFirst()
                .orElse(null);
            
            // 호텔 정보 저장
            if (selectedHotel != null) {
                log.info("🏨 Selected Hotel: {}, Price: {}, Location: {}", 
                    selectedHotel.getHotelName(), 
                    selectedHotel.getTotalPrice(),
                    selectedHotel.getNeighborhood());
                
                // 호텔 상세 정보 저장
                String hotelDetail = "호텔: " + selectedHotel.getHotelName() + 
                                    " | 객실: " + selectedHotel.getRoomTypeName() +
                                    " | 침대: " + selectedHotel.getBedType() +
                                    " | 요금제: " + selectedHotel.getRatePlanName() +
                                    (selectedHotel.getIncludesBreakfast() != null && selectedHotel.getIncludesBreakfast() ? 
                                     " | 조식: 포함" : "");
                bookingRequest.setProviderBookingMeta(hotelDetail);
            }

            // 최소한의 보정
            if (bookingRequest.getUserId() == null) {
                bookingRequest.setUserId(tripPlan.getUserId());
            }
            if (bookingRequest.getAdultsCount() == null) {
                bookingRequest.setAdultsCount(adults);
            }
            if (bookingRequest.getChildrenCount() == null) {
                bookingRequest.setChildrenCount(children);
            }
            if (bookingRequest.getCheckinDate() == null) {
                bookingRequest.setCheckinDate(checkin);
            }
            if (bookingRequest.getCheckoutDate() == null) {
                bookingRequest.setCheckoutDate(checkout);
            }
            if (bookingRequest.getNights() == null) {
                bookingRequest.setNights((int) nights);
            }
            if (bookingRequest.getStatus() == null) {
                bookingRequest.setStatus("PENDING");
            }
            if (bookingRequest.getPaymentStatus() == null) {
                bookingRequest.setPaymentStatus("PENDING");
            }
            
            // 호텔 정보 추가
            if (selectedHotel != null) {
                bookingRequest.setHotelName(selectedHotel.getHotelName());
                bookingRequest.setNeighborhood(selectedHotel.getNeighborhood());
                bookingRequest.setRoomTypeName(selectedHotel.getRoomTypeName());
                bookingRequest.setRatePlanName(selectedHotel.getRatePlanName());
                bookingRequest.setHasFreeWifi(selectedHotel.getHasFreeWifi());
                bookingRequest.setHasParking(selectedHotel.getHasParking());
                bookingRequest.setIsPetFriendly(selectedHotel.getIsPetFriendly());
                bookingRequest.setIsFamilyFriendly(selectedHotel.getIsFamilyFriendly());
                bookingRequest.setHas24hFrontdesk(selectedHotel.getHas24hFrontdesk());
                bookingRequest.setNearMetro(selectedHotel.getNearMetro());
                bookingRequest.setMetroStationName(selectedHotel.getMetroStationName());
                bookingRequest.setAirportDistanceKm(selectedHotel.getAirportDistanceKm());
            }

            return bookingRequest;

        } catch (Exception e) {
            log.error("HotelBookingAgent error", e);
            throw new RuntimeException("Failed to create booking from itinerary", e);
        }
    }
}

