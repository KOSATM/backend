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

/**
 * 여행 체크리스트 생성 에이전트
 * 
 * 역할:
 * - LLM(Large Language Model)을 활용하여 여행지별 실용적인 팁 생성
 * - Google Custom Search를 통한 최신 정보 검색 및 반영
 * - 당일 활용 가능한 할인/무료 조건, 규칙 등의 정보 제공
 * 
 * 동작 원리:
 * 1. 여행 일정에서 방문 장소 정보 조회
 * 2. LLM에게 인터넷 검색 Tool 제공
 * 3. LLM이 각 장소별로 검색 쿼리 생성 및 Tool 호출
 * 4. 검색 결과를 기반으로 5개의 팁 생성
 * 5. JSON 응답 파싱 및 반환
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChecklistAgent {
    
    // ========================================
    // 상수 정의
    // ========================================
    /**
     * 시스템 프롬프트
     * LLM의 역할과 응답 형식을 정의합니다.
     * 변경 없이 모든 요청에 동일하게 사용됩니다.
     */
    private static final String CHECKLIST_SYSTEM_PROMPT = """
        당신은 여행 정보 전문가입니다.
        infoSearch 도구를 반드시 사용해서 각 장소의 최신 정보를 검색하세요.
        
        반환 형식: 반드시 JSON만 응답하세요 (한국어 내용, 영어 키)
        {
          "title": "Must-Know Travel Tips",
          "items": [
            "장소명: 팁",
            "장소명: 팁",
            "장소명: 팁",
            "장소명: 팁",
            "장소명: 팁"
          ]
        }
        """;
    
    // ========================================
    // 의존성 주입
    // ========================================
    private final ChatClient.Builder chatClientBuilder;          // LLM 호출을 위한 ChatClient 빌더
    private final ChecklistTravelDayDao checklistTravelDayDao;   // DB에서 여행 일정 조회
    private final ObjectMapper objectMapper;                     // JSON 직렬화/역직렬화
    private final InternetSearchTool internetSearchTool;         // Google Custom Search API 호출
    
    /**
     * 여행 체크리스트 생성 메인 메서드
     * 
     * @param planId 여행 계획 ID
     * @param dayIndex 여행 일차 (1부터 시작)
     * @return 생성된 체크리스트 (5개의 팁 포함)
     */
    public ChecklistItemResponse generateChecklist(Long planId, Integer dayIndex) {
        log.info("📋 체크리스트 생성 시작 - planId: {}, dayIndex: {}", planId, dayIndex);
        
        // ========================================
        // STEP 1: 여행 일정과 장소 정보 조회
        // ========================================
        log.debug("STEP 1: DB에서 여행 일정 조회 중...");
        TravelDayResponse travelDay = checklistTravelDayDao.getTravelDay(planId, dayIndex);
        
        // 유효성 검사: 여행 일정이나 장소가 없으면 null 반환
        if (travelDay == null || travelDay.getPlaces() == null || travelDay.getPlaces().isEmpty()) {
            log.warn("⚠️ 장소를 찾을 수 없음 - planId: {}, dayIndex: {}", planId, dayIndex);
            return null;
        }
        
        log.info("📊 여행 일정 정보 - 제목: {}, 날짜: {}", travelDay.getDayTitle(), travelDay.getPlanDate());
        log.info("📍 총 방문 장소 수: {}", travelDay.getPlaces().size());
        
        // ========================================
        // STEP 2: 장소 상세 정보 로깅 (디버깅용)
        // ========================================
        log.debug("STEP 2: 방문 장소 상세 정보 출력");
        logPlaceDetails(travelDay.getPlaces());
        
        // ========================================
        // STEP 3: LLM 호출 - 체크리스트 생성
        // ========================================
        log.debug("STEP 3: LLM 호출 시작");
        log.info("🤖 인터넷 검색 Tool과 함께 LLM 호출 중...");
        
        ChatClient chatClient = chatClientBuilder.build();
        
        try {
            // LLM 호출: 시스템 프롬프트 + 동적 유저 프롬프트
            String llmResponse = chatClient.prompt()
                .system(CHECKLIST_SYSTEM_PROMPT)
                .user(buildUserPrompt(travelDay))
                .tools(new ChecklistTools())  // infoSearch Tool 등록
                .call()
                .content();
            
            log.info("✅ LLM 호출 완료 - 응답 길이: {} 문자", llmResponse.length());
            log.debug("📄 전체 응답: {}", llmResponse);
            
            // ========================================
            // STEP 4: JSON 응답 파싱
            // ========================================
            log.debug("STEP 4: JSON 응답 파싱 중...");
            return parseJsonResponse(llmResponse);
            
        } catch (Exception e) {
            log.error("❌ LLM 호출 실패 - 에러: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 유저 프롬프트 생성 메서드
     * 
     * 동적으로 여행 일정 정보를 포함한 프롬프트를 생성합니다.
     * 각 여행마다 다른 장소와 일정이 포함되므로 동적 생성이 필요합니다.
     * 
     * @param travelDay 여행 일정 정보
     * @return 생성된 유저 프롬프트
     */
    private String buildUserPrompt(TravelDayResponse travelDay) {
        // 장소 리스트 생성 (예: "- 경복궁\n- 명동")
        String placeList = travelDay.getPlaces().stream()
            .map(p -> "- " + p.getPlaceName())
            .collect(Collectors.joining("\n"));
        
        return """
            방문 날짜: %s
            여행 일정: %s
            
            방문 장소:
            %s
            
            중요: infoSearch 도구를 반드시 사용해서 각 장소를 검색하세요.
            
            각 장소마다:
            1. "[장소명] 입장료 할인 무료 조건" 검색
            2. "[장소명] 당일 방문 팁" 검색
            3. "[장소명] 현재 운영 규칙" 검색
            4. "[장소명] 촬영 규칙" 검색
            5. "[장소명] 준비물" 검색
            
            검색 결과를 바탕으로 정확히 5개의 팁을 생성하세요.
            
            팁 작성 기준:
            - 검색에서 확인된 정보만 사용 (LLM의 학습 데이터 사용 금지)
            - 할인/무료 조건이 우선순위
            - 당일에 실제로 활용 가능한 내용만
            - 추측이나 일반적인 조언 제외
            - 교통/숙박 정보 제외
            
            예시:
            {
              "title": "Must-Know Travel Tips",
              "items": [
                "경복궁: 한복 입으면 입장료 무료, 일반인 3,000원",
                "N서울타워: 저녁 6시 일몰+야경 동시 감상, 맑은 날씨 필수",
                "한강공원: 돗자리 깔고 앉을 수 있음, 모기 방충제 필수",
                "박물관: 목요일 야간 개방(20시까지), 현장 구매 10% 할인",
                "명동: 신용카드 결제 시 할인, 오후 2-3시 피크 타임"
              ]
            }
            
            지금 infoSearch 도구를 사용해서 각 장소를 검색한 후 답변하세요.
            """.formatted(
                travelDay.getPlanDate(),
                travelDay.getDayTitle(),
                placeList
            );
    }
    
    /**
     * 장소 상세정보 로깅 메서드
     * 
     * 디버깅 목적으로 방문할 각 장소의 상세 정보를 로그에 출력합니다.
     * 
     * @param places 여행 장소 리스트
     */
    private void logPlaceDetails(java.util.List<TravelDayResponse.PlaceDto> places) {
        StringBuilder placeDetails = new StringBuilder();
        for (TravelDayResponse.PlaceDto place : places) {
            placeDetails.append("\n[").append(place.getPlaceName()).append("]")
                .append("\n  활동명: ").append(place.getPlaceTitle())
                .append("\n  주소: ").append(place.getAddress())
                .append("\n  방문시간: ").append(place.getStartAt()).append(" ~ ").append(place.getEndAt())
                .append("\n  좌표: ").append(place.getLat()).append(", ").append(place.getLng())
                .append("\n  예상비용: ").append(place.getExpectedCost()).append("\n");
        }
        log.info("📋 장소 상세정보:{}", placeDetails.toString());
    }
    
    /**
     * JSON 응답 파싱 메서드
     * 
     * LLM 응답의 JSON을 추출하고 파싱하는 작업을 담당합니다.
     * 마크다운 코드블록(```json ... ```)이 포함될 수 있으므로 처리해줍니다.
     * 
     * @param llmResponse LLM에서 받은 원본 응답 문자열
     * @return 파싱된 체크리스트 응답 객체
     */
    private ChecklistItemResponse parseJsonResponse(String llmResponse) {
        log.info("🔍 JSON 응답 파싱 중...");
        
        try {
            // ========================================
            // 1단계: 응답 유효성 검사
            // ========================================
            if (llmResponse == null || llmResponse.trim().isEmpty()) {
                log.error("❌ LLM 응답이 비어있음");
                return null;
            }
            
            log.info("📝 원본 응답: {}", llmResponse);
            
            // ========================================
            // 2단계: JSON 추출 (마크다운 코드블록 제거)
            // ========================================
            // LLM이 ```json ... ``` 형식으로 감싸서 응답할 수 있으므로 제거
            String cleanJson = llmResponse
                .replaceAll("```json\\s*", "")  // ```json 제거
                .replaceAll("```\\s*", "")      // ``` 제거
                .replaceAll("```", "")          // 남은 ``` 제거
                .trim();
            
            log.info("📝 정제된 응답: {}", cleanJson);
            
            // ========================================
            // 3단계: JSON 범위 찾기
            // ========================================
            // 응답에 다른 텍스트가 포함될 수 있으므로 { }를 찾아서 추출
            int startIdx = cleanJson.indexOf('{');     // 첫 번째 { 찾기
            int endIdx = cleanJson.lastIndexOf('}');   // 마지막 } 찾기
            
            if (startIdx < 0 || endIdx <= startIdx) {
                log.error("❌ 유효한 JSON 구조를 찾을 수 없음");
                return null;
            }
            
            // 실제 JSON 부분만 추출
            cleanJson = cleanJson.substring(startIdx, endIdx + 1);
            log.info("🧹 최종 JSON: {}", cleanJson);
            
            // ========================================
            // 4단계: JSON 직렬화
            // ========================================
            // JSON 문자열을 ChecklistItemResponse 객체로 변환
            ChecklistItemResponse result = objectMapper.readValue(cleanJson, ChecklistItemResponse.class);
            
            if (result == null) {
                log.error("❌ JSON 파싱 실패");
                return null;
            }
            
            // ========================================
            // 5단계: 결과 검증
            // ========================================
            int itemCount = result.getItems() != null ? result.getItems().size() : 0;
            log.info("✅ 성공적으로 {} 개 항목 파싱됨", itemCount);
            
            if (itemCount == 0) {
                log.warn("⚠️ 응답에 항목이 없음");
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("❌ JSON 파싱 에러: {}", e.getMessage(), e);
            return null;
        }
    }
    
    // ========================================
    // Tool 클래스 - LLM이 호출 가능한 도구
    // ========================================
    /**
     * ChecklistTools 클래스
     * 
     * LLM(ChatGPT, Claude 등)이 호출할 수 있는 Tool을 정의합니다.
     * 
     * 역할:
     * - infoSearch() 메서드: 여행지 정보를 Google Custom Search로 검색
     * - LLM은 필요할 때 이 메서드를 자동으로 호출
     * 
     * 예시:
     * 1. LLM: "경복궁 입장료 정보를 검색해야겠다"
     * 2. LLM이 infoSearch("경복궁 입장료 할인") 자동 호출
     * 3. Google Search API에서 결과 받음
     * 4. 검색 결과를 기반으로 팁 생성
     */
    public class ChecklistTools {
        /**
         * 인터넷 검색 Tool
         * 
         * @param query 검색 쿼리 (예: "경복궁 입장료 할인")
         * @return Google Custom Search 결과 (상위 3개 결과)
         * 
         * 동작 원리:
         * 1. LLM이 자동으로 이 메서드를 호출
         * 2. InternetSearchTool.googleSearch()를 통해 Google Custom Search API 호출
         * 3. 검색 결과를 텍스트로 반환
         * 4. LLM이 결과를 분석해서 팁 작성
         */
        @Tool(description = "여행지 정보를 인터넷에서 검색합니다")
        public String infoSearch(@ToolParam(description = "검색 쿼리") String query) {
            log.info("🔍 검색 중: {}", query);
            String result = internetSearchTool.googleSearch(query);
            log.info("📊 검색 결과 수신 완료");
            return result;
        }
    }
}
