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
            List<HotelRatePlanCandidate> candidates =
                    hotelCandidateService.findCandidates(checkin, checkout, adults, children);

            if (candidates == null || candidates.isEmpty()) {
                log.warn("HotelBookingAgent - no candidates for given itinerary");
                return null;
            }

            log.info("📊 Found {} hotel candidates from DB", candidates.size());
            
            HotelRatePlanCandidate firstHotel = candidates.get(0);
            log.info("🏨 First hotel: id={}, name={}, price={}, lat={}, lng={}", 
                firstHotel.getHotelId(), 
                firstHotel.getHotelName(), 
                firstHotel.getTotalPrice(),
                firstHotel.getLatitude(),
                firstHotel.getLongitude());
            
            String candidatesJson = objectMapper.writeValueAsString(candidates);
            
            log.info("📋 Candidates JSON length: {} chars", candidatesJson.length());

            // 2) LLM 호출 - 3개 추천
            log.info("🤖 Calling LLM to select top 3 hotels...");
            String llmResultJson = chatClient.prompt()
                    .system("""
                        주어진 호텔 후보 목록에서 TOP 3개를 선택해 JSON 배열로 반환하세요.
                        
                        선택 기준 (우선순위):
                        1. 사용자 요청사항: 반드시 모든 조건을 만족해야 함
                        2. 거리: 여행 일정의 장소들과 가까운 호텔
                        3. 가격: 합리적인 가격
                        4. 평점: 높은 별점
                        
                        중요: 사용자 요청사항이 있으면 그 조건을 만족하는 호텔만 선택하세요!
                        
                        반환값: JSON 배열로 3개의 객체를 반환하세요 (마크다운 없음)
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
                        """ + (userPreferences != null && !userPreferences.isEmpty() ? 
                            "사용자 요청사항: " + userPreferences + "\n사용자 요청사항을 반드시 만족하는 호텔만 선택하세요.\n" : "") + """
                        
                        TOP 3개 호텔을 선택하고 JSON 배열로 반환하세요.
                        """))
                    .call()
                    .content();

            // 3) JSON → DTO
            log.info("📝 Raw LLM response: {}", llmResultJson);
            
            String cleanJson = llmResultJson
                    .replaceAll("```json\\s*", "")
                    .replaceAll("```\\s*", "")
                    .replaceAll("```", "")
                    .trim();
            
            // 첫 번째 [ 찾기
            int startIdx = cleanJson.indexOf('[');
            int endIdx = cleanJson.lastIndexOf(']');
            
            if (startIdx >= 0 && endIdx > startIdx) {
                cleanJson = cleanJson.substring(startIdx, endIdx + 1);
            }
            
            log.info("🧹 Cleaned JSON: {}", cleanJson);
            
            // JSON 배열 파싱
            List<HotelBookingRequest> bookingRequestList = objectMapper.readValue(
                cleanJson, 
                objectMapper.getTypeFactory().constructCollectionType(List.class, HotelBookingRequest.class)
            );
            
            if (bookingRequestList == null || bookingRequestList.isEmpty()) {
                log.warn("LLM returned empty list");
                return null;
            }
            
            log.info("✅ Parsed {} hotels", bookingRequestList.size());
            
            // 각 호텔에 정보 추가
            for (HotelBookingRequest bookingRequest : bookingRequestList) {
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
                
                // Guest 정보 설정
                if (bookingRequest.getGuestName() == null) {
                    bookingRequest.setGuestName(guestName);
                }
                if (bookingRequest.getGuestEmail() == null) {
                    bookingRequest.setGuestEmail(guestEmail);
                }
                if (bookingRequest.getGuestPhone() == null) {
                    bookingRequest.setGuestPhone(guestPhone);
                }
                if (bookingRequest.getBookedAt() == null) {
                    bookingRequest.setBookedAt(checkin);
                }
                
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
                    
                    String hotelDetail = "호텔: " + selectedHotel.getHotelName() + 
                                        " | 객실: " + selectedHotel.getRoomTypeName() +
                                        " | 침대: " + selectedHotel.getBedType() +
                                        " | 요금제: " + selectedHotel.getRatePlanName() +
                                        (selectedHotel.getIncludesBreakfast() != null && selectedHotel.getIncludesBreakfast() ? 
                                         " | 조식: 포함" : "");
                    bookingRequest.setProviderBookingMeta(hotelDetail);
                    
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
            }
            
            return bookingRequestList;

        } catch (Exception e) {
            log.error("HotelBookingAgent error", e);
            throw new RuntimeException("Failed to create booking from itinerary", e);
        }
    }
}

