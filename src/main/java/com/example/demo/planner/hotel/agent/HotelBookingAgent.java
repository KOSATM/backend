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
import com.example.demo.planner.plan.service.create.PlanService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class HotelBookingAgent {

    private ChatClient chatClient;
    private HotelCandidateService hotelCandidateService;
    private PlanService planService;
    private ObjectMapper objectMapper;
    private List<HotelRatePlanCandidate> candidates;

    @Autowired
    public HotelBookingAgent(
            ChatClient.Builder chatClientBuilder,
            HotelCandidateService hotelCandidateService,
            PlanService planService,
            ObjectMapper objectMapper
            
    ) {
        this.chatClient = chatClientBuilder.build();
        this.hotelCandidateService = hotelCandidateService;
        this.planService = planService;
        this.objectMapper = objectMapper;
    }

    public List<HotelBookingRequest> createBookingFromItinerary(
            Long userId,
            TripPlanRequest tripPlan,
            int adults,
            int children,
            String guestName,
            String guestEmail,
            String guestPhone,
            String userPreferences // 클라이언트가 입력해서 보내는 추가 요청사항
    ) {
        try {
            // 🔄 활성 Plan 정보 조회 (최우선)
            log.info("🔍 활성 Plan 정보 조회 중: userId={}", userId);
            com.example.demo.planner.plan.dto.response.PlanDetail activePlan = null;
            String planContext = "";
            try {
                activePlan = planService.getLatestPlanDetail(userId);
                planContext = buildPlanContext(activePlan);
                log.info("✅ 활성 Plan 정보 조회 완료");
            } catch (Exception e) {
                log.warn("⚠️ 활성 Plan 조회 실패: {}", e.getMessage());
                planContext = "사용자의 활성 여행 계획 정보를 사용할 수 없습니다.";
                throw new RuntimeException("활성 Plan을 조회할 수 없습니다", e);
            }

            // DB에서 조회한 Plan의 날짜 정보 사용
            LocalDate startDate = activePlan.getPlan().getStartDate();
            LocalDate endDate = activePlan.getPlan().getEndDate();
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
                            당신은 호텔 추천 전문가입니다.
                            사용자의 여행 일정에 맞는 호텔 3개를 반드시 JSON 형식으로만 추천하세요.
                            
                            [현재 여행 계획]
                            """ + planContext + """

                            [호텔 선택 기준]
                            1. 사용자 요청사항 필수 만족
                            2. 여행 일정 및 방문 장소와의 거리 고려
                            3. 거리가 가까운 호텔
                            4. 가격이 합리적
                            5. 평점이 높음

                            [필수 지시사항]
                            - getHotelCandidates 도구를 사용하여 호텔 목록을 먼저 조회하세요.
                            - 반드시 3개의 호텔을 선택하세요.
                            - 응답은 JSON 배열 형식ONLY로 반환하세요.
                            - 다른 설명이나 텍스트는 절대 포함하지 마세요.
                            
                            [JSON 응답 형식]
                            [
                              {"hotelId": 1, "roomTypeId": 2, "ratePlanId": 2},
                              {"hotelId": 3, "roomTypeId": 4, "ratePlanId": 5},
                              {"hotelId": 6, "roomTypeId": 7, "ratePlanId": 8}
                            ]
                            """)
                    .user("여행 일정: " + startDate + " ~ " + endDate +
                            (userPreferences != null && !userPreferences.isEmpty() ? "\n사용자 요청사항: " + userPreferences
                                    : ""))
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
                    checkin, checkout, nights);

            return bookingRequests;

        } catch (Exception e) {
            log.error("HotelBookingAgent error", e);
            throw new RuntimeException("Failed to create booking from itinerary", e);
        }
    }

    private List<HotelBookingRequest> parseSelectedHotels(
            String llmResponse,
            List<HotelRatePlanCandidate> candidates) {
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
            java.util.List<java.util.Map<String, Object>> selectedIds = objectMapper.readValue(cleanJson,
                    java.util.List.class);

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
            long nights) {
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
                    (candidate.getIncludesBreakfast() != null && candidate.getIncludesBreakfast() ? " | 조식: 포함" : "");
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

    private String buildPlanContext(com.example.demo.planner.plan.dto.response.PlanDetail planDetail) {
        if (planDetail == null || planDetail.getPlan() == null) {
            return "여행 계획 정보 없음";
        }

        StringBuilder context = new StringBuilder();
        com.example.demo.planner.plan.dto.entity.Plan plan = planDetail.getPlan();

        context.append("📅 여행 기간: ").append(plan.getStartDate()).append(" ~ ").append(plan.getEndDate()).append("\n");
        context.append("💰 예산: ").append(plan.getBudget()).append("\n");
        context.append("🎯 방문 장소 (좌표 포함):\n");

        if (planDetail.getDays() != null && !planDetail.getDays().isEmpty()) {
            for (com.example.demo.planner.plan.dto.response.PlanDayWithPlaces dayWithPlaces : planDetail.getDays()) {
                context.append("  📍 Day ").append(dayWithPlaces.getDay().getDayIndex()).append(" (")
                       .append(dayWithPlaces.getDay().getPlanDate()).append("): \n");
                
                if (dayWithPlaces.getPlaces() != null && !dayWithPlaces.getPlaces().isEmpty()) {
                    for (com.example.demo.planner.plan.dto.entity.PlanPlace place : dayWithPlaces.getPlaces()) {
                        String placeName = place.getPlaceName() != null ? place.getPlaceName() : place.getTitle();
                        context.append("     - ").append(placeName)
                               .append(" (위도: ").append(place.getLat())
                               .append(", 경도: ").append(place.getLng()).append(")\n");
                    }
                } else {
                    context.append("     - 계획된 장소 없음\n");
                }
            }
        }

        return context.toString();
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
