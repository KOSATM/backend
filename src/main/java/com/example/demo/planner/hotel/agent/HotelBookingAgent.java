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
            List<HotelRatePlanCandidate> candidates =
                    hotelCandidateService.findCandidates(checkin, checkout, adults, children);

            if (candidates == null || candidates.isEmpty()) {
                log.warn("HotelBookingAgent - no candidates for given itinerary");
                return null;
            }

            String tripPlanJson = objectMapper.writeValueAsString(tripPlan);
            String candidatesJson = objectMapper.writeValueAsString(candidates);

            // 2) LLM 호출
            String llmResultJson = chatClient.prompt()
                    .system("""
                        너는 사용자의 서울 여행 일정에 맞는 호텔을 하나 골라
                        hotel_bookings 테이블에 저장할 수 있는 예약 정보를 만드는 역할을 한다.

                        ## 입력 설명
                        - itineraryJson: 사용자의 전체 여행 일정 정보 (날짜, 방문 장소, 예산 등 포함)
                        - candidatesJson: DB에서 조회된 실제 호텔/객실/요금제 후보 리스트.
                          각 후보에는 hotelId, roomTypeId, ratePlanId, 호텔 이름, 위치, 총 가격, 평점 등이 포함되어 있다.

                        ## 선택 기준
                        1. 동선
                           - 일정에 나오는 장소들(명동, 경복궁, 북촌, 강남, 남산 등)과의 거리/접근성을 고려한다.
                        2. 예산
                           - tripPlan.budget 과 후보의 totalPrice 를 비교해서 예산을 크게 넘지 않도록 한다.
                        3. 호텔 평점/리뷰
                           - ratingScore, reviewCount, starRating(있다면)를 참고해서
                             "너무 후진 곳"은 피하면서 합리적인 곳을 고른다.

                        ## 출력 형식 (JSON만 반환)
                        아래 Java DTO 구조(HotelBookingRequest)에 맞게 JSON 객체 하나만 반환해라.

                        HotelBookingRequest:
                        {
                          "userId": <long>,
                          "externalBookingId": "<string 또는 null>",
                          "hotelId": <long>,
                          "roomTypeId": <long>,
                          "ratePlanId": <long>,
                          "checkinDate": "yyyy-MM-dd'T'HH:mm:ssXXX",
                          "checkoutDate": "yyyy-MM-dd'T'HH:mm:ssXXX",
                          "nights": <int>,
                          "adultsCount": <int>,
                          "childrenCount": <int>,
                          "currency": "<3-letter, 예: 'KRW'>",
                          "totalPrice": <number>,
                          "taxAmount": <number>,
                          "feeAmount": <number>,
                          "status": "PENDING",
                          "paymentStatus": "PENDING",
                          "guestName": "<게스트 이름>",
                          "guestEmail": "<게스트 이메일>",
                          "guestPhone": "<게스트 전화번호>",
                          "providerBookingMeta": "<JSON 또는 간단한 텍스트 설명>",
                          "bookedAt": "yyyy-MM-dd'T'HH:mm:ssXXX",
                          "cancelledAt": null
                        }

                        ### 중요:
                        - JSON 이외의 텍스트(설명, 주석 등)는 절대 출력하지 마라.
                        - 날짜/시간 형식은 반드시 위에 적은 ISO-8601 형식을 지켜라.
                        - userId, 인원 수, 게스트 정보는 사용자 입력 값(tripPlan, adults, children, guest*)를 그대로 사용해라.
                        """)
                    .user(u -> u
                            .text("itineraryJson:")
                            .text(tripPlanJson)
                            .text("candidatesJson:")
                            .text(candidatesJson)
                            .text("adults: " + adults)
                            .text("children: " + children)
                            .text("guestName: " + guestName)
                            .text("guestEmail: " + guestEmail)
                            .text("guestPhone: " + guestPhone)
                            .text("checkin: " + checkin.toString())
                            .text("checkout: " + checkout.toString())
                            .text("nights: " + nights)
                            .text("userId: " + tripPlan.getUserId())
                    )
                    .call()
                    .content();

            // 3) JSON → DTO
            HotelBookingRequest bookingRequest =
                    objectMapper.readValue(llmResultJson, HotelBookingRequest.class);

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

            return bookingRequest;

        } catch (Exception e) {
            log.error("HotelBookingAgent error", e);
            throw new RuntimeException("Failed to create booking from itinerary", e);
        }
    }
}
