package com.example.demo.supporter.checklist.agent;

import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.example.demo.common.tools.InternetSearchTool;
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
    private final InternetSearchTool internetSearchTool;
    
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
        
        // 3) LLM 호출 + InternetSearchTool 연동
        ChatClient chatClient = chatClientBuilder.build();
        String llmResponse = chatClient.prompt()
            .system("""
                당신은 여행 정보 전문가입니다.
                infoSearch 도구를 반드시 사용해서 각 장소의 최신 정보를 검색하고,
                검색 결과만을 기반으로 팁을 생성하세요.
                
                반환 형식: 반드시 JSON 형식으로만 응답하세요
                {
                  "title": "꼭 알아야 할 여행 팁",
                  "items": [
                    "장소명: 구체적인 팁",
                    ...
                  ]
                }
                
                규칙:
                1. 정확히 5개의 항목만 생성
                2. 각 항목은 "장소명: 팁" 형식 (예: "경복궁: 한복 입으면 입장료 무료")
                3. 마크다운, 이모지 절대 금지
                4. JSON 외의 다른 텍스트는 포함하지 마세요
                """)
            .user("""
                방문 날짜: """ + travelDay.getPlanDate() + """
                여행 일정: """ + travelDay.getDayTitle() + """
                
                방문 장소들:
                """ + travelDay.getPlaces().stream()
                    .map(p -> p.getPlaceName())
                    .collect(Collectors.joining(", ")) + """
                
                ⚠️ 중요 지시사항:
                
                1. 각 장소마다 반드시 infoSearch 도구로 검색하세요:
                   - "[장소명] 입장료 할인 무료 조건"
                   - "[장소명] 당일 방문 팁"
                   - "[장소명] 현재 운영 규칙"
                   - "[장소명] 촬영 규칙 제한"
                
                2. 검색 결과를 바탕으로만 팁을 생성하세요
                   (LLM의 추측이 아닌 실제 정보만 사용)
                
                3. 당일에 실제로 활용 가능한 정보만 포함:
                   ✅ 할인/무료 조건 (검색 확인)
                   ✅ 규칙/주의사항 (검색 확인)
                   ✅ 준비물 (검색 확인)
                   ✅ 오픈 시간/최적 방문 시간 (검색 확인)
                   ✅ 예약 요구사항 (검색 확인)
                
                4. 절대 포함하면 안 되는 것:
                   ❌ "아마도", "~일 것 같습니다" 같은 추측
                   ❌ 검색하지 않은 정보
                   ❌ 계절별 정보 (당일과 맞지 않으면)
                   ❌ 교통/숙박 정보
                   ❌ 일반적인 조언
                
                5. 응답 형식:
                   - JSON만 응답 (다른 텍스트 금지)
                   - 정확히 5개 항목
                   - 각 항목은 "장소명: 구체적인 팁" 형식
                
                예시 (이 수준으로 작성):
                {
                  "title": "꼭 알아야 할 여행 팁",
                  "items": [
                    "경복궁: 한복 입으면 입장료 무료, 일반인 3,000원",
                    "N서울타워: 날씨 맑은 날 가야 야경 잘 보임, 저녁 6시 일몰+야경 동시 감상",
                    "한강공원: 돗자리 깔고 앉을 수 있음, 모기 방충제 필수",
                    "박물관: 목요일 야간 개방(20시까지), 현장 구매 시 10% 할인"
                  ]
                }
                
                지금 당신의 차례입니다. 필수: infoSearch 도구를 사용해서 각 장소 정보를 검색한 후 답변하세요.
                """)
            .tools(new ChecklistTools())
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
    
    // Tool 클래스 - LLM이 호출 가능
    public class ChecklistTools {
        @Tool(description = "여행지 정보를 인터넷에서 검색합니다")
        public String infoSearch(@ToolParam(description = "검색 쿼리") String query) {
            log.info("🔍 Searching for: {}", query);
            String result = internetSearchTool.googleSearch(query);
            log.info("📊 Search result received");
            return result;
        }
    }
}
