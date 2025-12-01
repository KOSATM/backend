package com.example.demo.supporter.checklist.agent;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import com.example.demo.supporter.checklist.dao.ChecklistTravelDayDao;
import com.example.demo.supporter.checklist.dto.response.ChecklistItemResponse;
import com.example.demo.supporter.checklist.dto.response.TravelDayResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChecklistAgent {
    
    private final ChatClient.Builder chatClientBuilder;
    private final ChecklistTravelDayDao checklistTravelDayDao;
    private final ObjectMapper objectMapper;
    
    public ChecklistItemResponse generateChecklist(Long planId, Integer dayIndex) {
        log.info("📋 Generating checklist for planId: {}, dayIndex: {}", planId, dayIndex);
        
        // 1) 여행 일정과 장소 조회
        TravelDayResponse travelDay = checklistTravelDayDao.getTravelDay(planId, dayIndex);
        
        if (travelDay == null || travelDay.getPlaces() == null || travelDay.getPlaces().isEmpty()) {
            log.warn("⚠️ No places found for planId: {}, dayIndex: {}", planId, dayIndex);
            return null;
        }
        
        log.info("📊 Travel day info - title: {}, date: {}", travelDay.getDayTitle(), travelDay.getPlanDate());
        log.info("📍 Total places: {}", travelDay.getPlaces().size());
        
        // 2) 장소 정보 상세 로깅
        StringBuilder placeDetails = new StringBuilder();
        for (TravelDayResponse.PlaceDto place : travelDay.getPlaces()) {
            placeDetails.append("\n[").append(place.getPlaceName()).append("]")
                .append("\n  제목: ").append(place.getPlaceTitle())
                .append("\n  주소: ").append(place.getAddress())
                .append("\n  시간: ").append(place.getStartAt()).append(" ~ ").append(place.getEndAt())
                .append("\n  위치: ").append(place.getLat()).append(", ").append(place.getLng())
                .append("\n  예상비용: ").append(place.getExpectedCost()).append("\n");
        }
        log.info("📋 Place Details:{}", placeDetails.toString());
        
        // 2) 장소명 추출
        String placeNames = travelDay.getPlaces().stream()
            .map(place -> {
                log.debug("  - Place: {}", place.getPlaceName());
                return place.getPlaceName();
            })
            .collect(Collectors.joining(", "));
        
        log.info("🏙️ Extracted place names: {}", placeNames);
        
        // 3) LLM 호출
        ChatClient chatClient = chatClientBuilder.build();
        String llmResponse = chatClient.prompt()
            .system("""
                여행지에 대한 실용적이고 유용한 정보 5가지만 생성하세요.
                
                반환 형식: JSON
                {
                  "title": "꼭 알아야 할 여행 팁",
                  "items": [
                    "장소명: 구체적인 팁 (정보의 출처나 가성비를 포함)",
                    ...
                  ]
                }
                
                각 항목은 "장소명: 팁" 형식이고, 정확히 5개만 생성하세요.
                마크다운 없이 JSON만 반환하세요.
                """)
            .user("""
                방문 날짜: """ + travelDay.getPlanDate() + """
                여행 일정: """ + travelDay.getDayTitle() + """
                
                다음 장소들을 방문합니다:
                """ + travelDay.getPlaces().stream()
                    .map(p -> "- " + p.getPlaceName() + 
                        (p.getAddress() != null ? " (" + p.getAddress() + ")" : "") +
                        (p.getLat() != null && p.getLng() != null ? 
                            "\n  좌표: " + String.format("%.4f, %.4f", p.getLat(), p.getLng()) : "") +
                        "\n  일정: " + p.getStartAt() + " ~ " + p.getEndAt() +
                        (p.getPlaceTitle() != null ? "\n  활동: " + p.getPlaceTitle() : "") +
                        (p.getExpectedCost() != null && p.getExpectedCost() > 0 ?
                            "\n  예상비용: " + p.getExpectedCost() + "원" : ""))
                    .collect(Collectors.joining("\n")) + """
                
                위 장소들에 대해 '당일 활용 가능한 실용적인 팁' TOP 5를 생성하세요.
                
                중요: 날짜를 기반으로 당일에 활용할 수 있는 정보만 포함하세요!
                
                포함할 수 있는 정보:
                ✅ 금전 혜택 (할인, 무료 조건 등) - 당일 적용 가능한 것만
                ✅ 방문/촬영 규칙 (예: 플래시 금지, 삼각대 금지, 드론 비행 가능)
                ✅ 현장 규칙 (예: 신발 벗어야 함, 짐 맡김 필수)
                ✅ 필수 준비물 (예: 우산, 편한 신발, 선크림)
                ✅ 당일 주의사항 (예: 계단 많음, 높이공포증 주의, 체력 요구)
                ✅ 당일 특별 경험 (예: 야간 투어 예약, 특별 가이드, 당일 체험 프로그램)
                ✅ 편의시설 정보 (예: 짐 보관함, 휠체어 접근성, 화장실 위치)
                ✅ 사진 명소 (예: 최고의 뷰 포인트, 인생샷 스팟)
                ✅ 방문 시간대 활용 팁 (예: 오후 2시가 한산, 저녁 6시 야경 최고 등)
                ✅ 당일 구매/예약 정보 (예: 현장 구매 가능, 앱 할인 적용, 카드 할인 등)
                
                절대 포함하면 안 되는 것:
                ❌ 계절별 정보 (봄에는, 여름에는, 겨울에는 등)
                ❌ 시즌 이벤트 (축제, 벚꽃 시즌 등 - 당일과 맞지 않으면 제외)
                ❌ 교통, 버스, 지하철 정보
                ❌ 숙박 정보
                ❌ "즐거운 여행" 같은 주관적 평가
                
                팁 예시 (당일 활용 중심):
                - "경복궁: 한복 입으면 입장료 무료, 일반인 3,000원, 오전 9시 오픈 직후 가면 한산"
                - "N서울타워: 저녁 6시 도착 시 일몰 + 야경 동시 감상 가능, 맑은 날씨 확인 필수"
                - "한강공원: 오후 시간대 피크 타임, 돗자리 깔고 앉을 수 있음, 모기 방충제 필수"
                - "박물관: 목요일 야간 개방(20시까지), 당일 인터넷 예약 시 10% 할인"
                - "카페 밀집 지역: 평일 오전 10-12시 한산, 신용카드 결제 시 5% 할인"
                
                정확히 5개의 '당일에 바로 활용 가능한' 실질적인 정보만 생성하세요.
                
                추가 주의사항:
                ⚠️ 정보의 정확성이 중요합니다!
                ⚠️ 확실하지 않은 정보는 생성하지 마세요 (예: 할인율, 가격 등)
                ⚠️ 알려진 사실에 기반한 정보만 포함하세요
                ⚠️ "2024년 기준" 같은 시간 제한이 있으면 명시하세요
                """)
            .call()
            .content();
        
        log.info("🤖 LLM generated response (length: {})", llmResponse.length());
        log.debug("📄 Full response: {}", llmResponse);
        
        // 4) JSON 파싱
        try {
            String cleanJson = llmResponse
                .replaceAll("```json\\s*", "")
                .replaceAll("```\\s*", "")
                .replaceAll("```", "")
                .trim();
            
            int startIdx = cleanJson.indexOf('{');
            int endIdx = cleanJson.lastIndexOf('}');
            
            if (startIdx >= 0 && endIdx > startIdx) {
                cleanJson = cleanJson.substring(startIdx, endIdx + 1);
            }
            
            log.info("🧹 Cleaned JSON: {}", cleanJson);
            
            ChecklistItemResponse result = objectMapper.readValue(cleanJson, ChecklistItemResponse.class);
            
            log.info("✅ Generated {} checklist items", 
                result.getItems() != null ? result.getItems().size() : 0);
            
            return result;
            
        } catch (Exception e) {
            log.error("❌ Error parsing LLM response", e);
            return null;
        }
    }
}
