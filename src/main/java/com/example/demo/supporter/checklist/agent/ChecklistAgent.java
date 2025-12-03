package com.example.demo.supporter.checklist.agent;

import java.time.OffsetDateTime;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import com.example.demo.supporter.checklist.dao.ChecklistTravelDayDao;
import com.example.demo.supporter.checklist.dto.entity.Checklist;
import com.example.demo.supporter.checklist.dto.entity.ChecklistItem;
import com.example.demo.supporter.checklist.dto.response.ChecklistItemResponse;
import com.example.demo.supporter.checklist.dto.response.TravelDayResponse;
import com.example.demo.supporter.checklist.service.ChecklistService;
import com.example.demo.supporter.checklist.service.ChecklistItemService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChecklistAgent {
    
    private final ChatClient.Builder chatClientBuilder;
    private final ChecklistTravelDayDao checklistTravelDayDao;
    private final ChecklistService checklistService;
    private final ChecklistItemService checklistItemService;
    private final ObjectMapper objectMapper;
    
    public ChecklistItemResponse generateChecklist(Long planId, Integer dayIndex, Long userId) {
        log.info("📋 Generating checklist for planId: {}, dayIndex: {}, userId: {}", planId, dayIndex, userId);
        
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
        
        // 3) LLM 호출
        ChatClient chatClient = chatClientBuilder.build();
        String llmResponse = chatClient.prompt()
            .system("""
                당신은 여행 정보 전문가입니다.
                여행객들을 위해 실용적이고 유용한 여행 팁을 생성해주세요.
                
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
                
                위 장소들을 방문할 때 도움이 될 만한 5가지 실용적인 팁을 생성해주세요.
                각 팁은 "장소명: 구체적인 팁" 형식으로 작성해주세요.
                
                예시:
                {
                  "title": "꼭 알아야 할 여행 팁",
                  "items": [
                    "경복궁: 한복 입으면 입장료 무료, 일반인 3,000원",
                    "N서울타워: 날씨 맑은 날 가야 야경 잘 보임, 저녁 6시 일몰+야경 동시 감상",
                    "한강공원: 돗자리 깔고 앉을 수 있음, 모기 방충제 필수",
                    "박물관: 목요일 야간 개방(20시까지), 현장 구매 시 10% 할인",
                    "명동: 쇼핑 후 커피는 필수"
                  ]
                }
                
                JSON 형식으로 응답해주세요.
                """)
            .call()
            .content();
        
        log.info("🤖 LLM generated response (length: {})", llmResponse.length());
        log.debug("📄 Full response: {}", llmResponse);
        
        // 4) JSON 파싱
        ChecklistItemResponse result = null;
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
            
            result = objectMapper.readValue(cleanJson, ChecklistItemResponse.class);
            
            log.info("✅ Generated {} checklist items", 
                result.getItems() != null ? result.getItems().size() : 0);
            
        } catch (Exception e) {
            log.error("❌ Error parsing LLM response", e);
            return null;
        }
        
        // 5) DB에 저장
        saveChecklistToDb(planId, dayIndex, result, travelDay.getUserId());
        
        return result;
    }
    
    /**
     * 생성된 체크리스트를 DB에 저장
     */
    private void saveChecklistToDb(Long planId, Integer dayIndex, ChecklistItemResponse llmResponse, Long userId) {
        try {
            log.info("💾 Saving checklist to DB - userId: {}, dayIndex: {}", userId, dayIndex);
            
            // 1) Checklist 생성
            Checklist checklist = Checklist.builder()
                .userId(userId)
                .dayIndex(dayIndex)
                .createdAt(OffsetDateTime.now())
                .build();
            
            Long checklistId = checklistService.create(checklist);
            log.info("✅ Checklist created with id: {}", checklistId);
            
            // 2) ChecklistItem 저장
            if (llmResponse.getItems() != null && !llmResponse.getItems().isEmpty()) {
                for (String item : llmResponse.getItems()) {
                    ChecklistItem checklistItem = ChecklistItem.builder()
                        .checklistId(checklistId)
                        .content(item)
                        .category("GENERAL")
                        .isChecked(false)
                        .createdAt(OffsetDateTime.now())
                        .build();
                    
                    checklistItemService.create(checklistItem);
                    log.debug("✅ ChecklistItem created - content: {}", item);
                }
                log.info("✅ Total {} items saved", llmResponse.getItems().size());
            }
            
            log.info("🎉 Checklist saved successfully to DB");
            
        } catch (Exception e) {
            log.error("❌ Error saving checklist to DB", e);
        }
    }
}