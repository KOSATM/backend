package com.example.demo.planner.plan.agent;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.example.demo.common.chat.intent.dto.IntentCommand;
import com.example.demo.common.chat.pipeline.AiAgentResponse;
import com.example.demo.common.chat.pipeline.UnifiedAgentResponse;
import com.example.demo.common.global.agent.AiAgent;
import com.example.demo.planner.plan.dao.PlanSnapshotDao;
import com.example.demo.planner.plan.dto.entity.PlanSnapshot;
import com.example.demo.planner.plan.dto.response.PlanSnapshotContent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class PlaceSuggestAgent implements AiAgent {
  private ChatClient chatClient;

  @Autowired
  private EmbeddingModel embeddingModel;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private PlanSnapshotDao planSnapshotDao;

  public PlaceSuggestAgent(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder
      .build();
  }

  @Override
  public AiAgentResponse execute(IntentCommand command, Long userId) {
    long start = System.nanoTime();
    log.info(command.toString());
    
    String originalUserMessage = command.getOriginalUserMessage();
    String commandArguments = command.getArguments().toString();
    
    // Step 1: Tool 실행 및 DB 결과 수집
    SuggestReferenceTools tools = new SuggestReferenceTools();
    
    String answer = chatClient.prompt()
        .system("""
            당신은 여행 전문가입니다.
            사용자의 질문에서 키워드를 추출하여 현재 사용자의 여행 계획을 참고한 후
            키워드에 관한 데이터베이스를 조회하고 사용자에게 여행 장소를 추천하세요.

            여행 계획 조회는 동선을 최적화하기 위함입니다.
            데이터베이스 조회 쿼리는 사용자의 질문과 동선(장소의 주소)을 참고하여 작성하세요.

            ⚠️ 중요 지시사항:
            1. 먼저 `getCurrentPlan` 도구를 사용하여 사용자의 현재 여행 계획을 조회하세요
            2. 사용자의 질문에 날짜 정보가 있으면 `validateDate` 도구를 사용하여 검증하세요
            3. `validateDate`가 false를 반환하면, 날짜가 여행 계획 범위를 벗어난다고 사용자에게 알리세요
            4. 날짜가 유효한 경우에만 `dbSearch` 도구를 사용하여 데이터베이스를 조회하세요
            5. 데이터베이스 조회는 `dbSearch` 도구를 사용하세요
            6. 사용자의 질문에 동선 정보가 있다면(예: 마지막 일정 장소 근처) 조회한 여행 계획을 참고하여 동선이 최적화되는 장소를 추천하세요

            ✅ 응답 방식:
            - dbSearch 도구에서 받은 JSON 배열을 절대 그대로 노출하지 마세요
            - 받은 JSON 데이터를 파싱하여 자연스러운 한국어 텍스트로 정리하세요
            - 각 장소를 다음 형식으로 소개하세요:
              "• [장소명]
               주소: [주소]
               전화: [전화번호 또는 미제공]
               설명: [설명]
               상세정보: [상세정보]"
            - 마크다운 형식(예: **굵게**, # 제목)은 사용하지 마세요
            - 순수 텍스트로만 응답하세요
            - 추천 장소 목록 앞에 친절한 인사말을 포함하세요
            - JSON, 배열 기호 [], 중괄호 {} 등 프로그래밍 형식을 절대 노출하지 마세요
            """)
        .user("""
            originalUserMessage: %s, commandArguments: %s
            """.formatted(originalUserMessage, commandArguments))
        .tools(tools)
        .toolContext(Map.of("userId", userId))
        .call()
        .content();
    
    // Step 2: 역할 분리 - LLM 설명 + DB 데이터 분리
    List<Map<String, Object>> placesData = tools.getLastSearchResult();
    
    // Step 3: UnifiedAgentResponse로 응답 (message + data 분리)
    UnifiedAgentResponse unifiedResponse = UnifiedAgentResponse.builder()
        .message(answer)
        .data(placesData)
        .changes(Map.of(
            "action", "place_suggested",
            "count", placesData.size(),
            "details", placesData.stream()
                .map(p -> p.get("title"))
                .toList()
        ))
        .build();
    
    // UnifiedAgentResponse → AiAgentResponse 변환 (호환성 유지)
    AiAgentResponse response = AiAgentResponse.builder()
        .message(unifiedResponse.getMessage())
        .data(unifiedResponse)
        .build();
    
    log.info("response: {}", response.toString());
    long end = System.nanoTime();
    log.info("실행시간: {}ms", (end - start) / 1_000_000);
    return response;
  }

  class SuggestReferenceTools {
    // 현재 계획을 저장하는 필드 (validateDate에서 사용)
    private PlanSnapshotContent currentPlanSnapshot = null;
    // 마지막 DB 검색 결과를 저장 (execute 메서드에서 접근)
    private List<Map<String, Object>> lastSearchResult = null;
    
    public List<Map<String, Object>> getLastSearchResult() {
      return lastSearchResult != null ? lastSearchResult : new ArrayList<>();
    }
    
    @Tool(description = "추천을 하기 전 현재 여행 계획을 조회합니다")
    public Object getCurrentPlan(ToolContext toolContext) throws JsonProcessingException {
      ObjectMapper objectMapper = new ObjectMapper();
      long start = System.nanoTime();
      
      try {
        PlanSnapshot planSnapshot = planSnapshotDao.selectLatestPlanSnapshotByUserId((Long) toolContext.getContext().get("userId"));
        
        if (planSnapshot == null) {
          log.warn("⚠️ 사용자의 여행 계획을 찾을 수 없습니다");
          return "저장된 여행 계획이 없습니다. 먼저 여행 계획을 생성해주세요.";
        }
        
        String snapshotJson = planSnapshot.getSnapshotJson();
        PlanSnapshotContent snapshotContent = objectMapper.readValue(snapshotJson, PlanSnapshotContent.class);
        
        // 현재 계획을 저장 (날짜 검증용)
        this.currentPlanSnapshot = snapshotContent;
        
        log.info("📅 현재 여행 계획 조회 완료");
        log.info("   기간: {} ~ {}", snapshotContent.getStartDate(), snapshotContent.getEndDate());
        log.info("   일정 수: {}일", snapshotContent.getDays().size());
        
        long end = System.nanoTime();
        log.info("⏱️ 계획 조회 시간: {}ms", (end - start) / 1_000_000);
        
        return snapshotContent;
        
      } catch (Exception e) {
        log.error("❌ 현재 여행 계획 조회 실패", e);
        return "여행 계획 조회 중 오류가 발생했습니다.";
      }
    }

    @Tool(description = "사용자의 입력 날짜가 여행 계획의 범위 내에 있는지 검증합니다")
    public boolean validateDate(String userInput) {
      log.info("🔍 날짜 검증 시작: {}", userInput);
      
      try {
        // 현재 계획이 없으면 검증 불가
        if (this.currentPlanSnapshot == null) {
          log.warn("⚠️ 현재 계획이 로드되지 않음");
          return true; // 계획이 없으면 검증 스킵
        }
        
        // 사용자 입력에서 날짜 추출
        LocalDate extractedDate = extractDateFromInput(userInput);
        
        if (extractedDate == null) {
          log.info("ℹ️ 사용자 입력에서 날짜 정보를 찾을 수 없습니다");
          return true; // 날짜 정보가 없으면 검증 스킵
        }
        
        // 여행 계획의 시작일과 종료일 파싱
        LocalDate planStartDate = LocalDate.parse(this.currentPlanSnapshot.getStartDate());
        LocalDate planEndDate = LocalDate.parse(this.currentPlanSnapshot.getEndDate());
        
        log.info("📅 검증 정보:");
        log.info("   여행 기간: {} ~ {}", planStartDate, planEndDate);
        log.info("   사용자 입력 날짜: {}", extractedDate);
        
        // 날짜 범위 검증
        boolean isValid = !extractedDate.isBefore(planStartDate) && !extractedDate.isAfter(planEndDate);
        
        if (isValid) {
          log.info("✅ 날짜 검증 성공");
        } else {
          log.warn("❌ 날짜가 여행 계획 범위를 벗어남");
          log.warn("   여행 기간: {} ~ {}", planStartDate, planEndDate);
          log.warn("   입력 날짜: {}", extractedDate);
        }
        
        return isValid;
        
      } catch (Exception e) {
        log.error("❌ 날짜 검증 중 오류 발생", e);
        return true; // 검증 실패 시 통과 (보수적 접근)
      }
    }

    @Tool(description = "자료를 찾기 위해 DB를 조회합니다")
    public String dbSearch(String query) {
      long start = System.nanoTime();
      log.info("검색 쿼리: {}", query);
      
      try {
        float[] vector = getQueryVector(query);
        String strVector = Arrays.toString(vector).replace(" ", "");
        
        String sql = """
            SELECT id, content_id, title, address, tel, first_image2, lat, lng, 
                   category_code, description, tags, detail_info, normalized_category, 
                   (embedding <=> ?::vector) AS distance
            FROM travel_places
            ORDER BY embedding <=> ?::vector
            LIMIT 20
            """;
        
        List<Map<String, Object>> res = jdbcTemplate.queryForList(sql, strVector, strVector);
        
        // 검색 결과 없음
        if (res.isEmpty()) {
          log.warn("검색 결과 없음: {}", query);
          return "해당 키워드와 관련된 검색 결과가 없습니다.";
        }
        
        // 첫 번째 결과의 거리 확인
        double minDistance = ((Number) res.get(0).get("distance")).doubleValue();
        log.info("최소 거리: {}", minDistance);
        
        if (minDistance > 0.7) {
          log.warn("거리 임계값 초과: {}", minDistance);
          return "해당 키워드와 관련된 검색 결과가 없습니다.";
        }
        
        // 결과 로깅
        res.forEach(map -> {
          String title = (String) map.get("title");
          double distance = ((Number) map.get("distance")).doubleValue();
          log.info("장소: {}, 거리: {}", title, distance);
        });
        
        long end = System.nanoTime();
        log.info("DB 검색 실행시간: {}ms", (end - start) / 1_000_000);
        
        // ✅ 역할 분리: DB 결과를 저장하고 JSON으로 LLM에 전달
        this.lastSearchResult = res;
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(res);
        
      } catch (Exception e) {
        log.error("DB 검색 실패: {}", e.getMessage(), e);
        return "검색 중 오류가 발생했습니다.";
      }
    }

    private float[] getQueryVector(String query) {
      long start = System.nanoTime();
      try {
        EmbeddingResponse response = embeddingModel.embedForResponse(List.of(query));
        float[] vector = response.getResult().getOutput();
        long end = System.nanoTime();
        log.info("임베딩 실행시간: {}ms", (end - start) / 1_000_000);
        return vector;
      } catch (Exception e) {
        log.error("임베딩 생성 실패: {}", e.getMessage(), e);
        throw new RuntimeException("임베딩 생성 실패", e);
      }
    }

    /**
     * 사용자 입력에서 날짜 추출
     * 지원 형식: "YYYY-MM-DD", "YYYY년 MM월 DD일", "MM월 DD일" 등
     */
    private LocalDate extractDateFromInput(String input) {
      log.debug("🔎 입력에서 날짜 추출: {}", input);
      
      // YYYY-MM-DD, YYYY/MM/DD, YYYYMMDD 형식 매칭
      Pattern datePattern = Pattern.compile("(\\d{4})[/-]?(\\d{2})[/-]?(\\d{2})");
      Matcher dateMatcher = datePattern.matcher(input);
      
      if (dateMatcher.find()) {
        String dateStr = dateMatcher.group(1) + "-" + dateMatcher.group(2) + "-" + dateMatcher.group(3);
        try {
          LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_DATE);
          log.debug("✅ 날짜 추출 성공: {}", date);
          return date;
        } catch (DateTimeParseException e) {
          log.debug("⚠️ 날짜 파싱 실패: {}", dateStr);
        }
      }
      
      // MM월 DD일 형식 (올해 기준)
      Pattern monthDayPattern = Pattern.compile("(\\d{1,2})월\\s*(\\d{1,2})일");
      Matcher monthDayMatcher = monthDayPattern.matcher(input);
      
      if (monthDayMatcher.find()) {
        try {
          int month = Integer.parseInt(monthDayMatcher.group(1));
          int day = Integer.parseInt(monthDayMatcher.group(2));
          
          if (this.currentPlanSnapshot != null) {
            // 여행 계획의 연도를 기반으로 날짜 구성
            LocalDate planStartDate = LocalDate.parse(this.currentPlanSnapshot.getStartDate());
            LocalDate date = LocalDate.of(planStartDate.getYear(), month, day);
            log.debug("✅ 월일 날짜 추출 성공: {}", date);
            return date;
          }
        } catch (Exception e) {
          log.debug("⚠️ 월일 날짜 파싱 실패", e);
        }
      }
      
      log.debug("⚠️ 입력에서 날짜를 찾을 수 없음");
      return null;
    }
  }
}