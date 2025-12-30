package com.example.demo.planner.plan.agent.tools;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.common.chat.intent.dto.SeoulRegion;
import com.example.demo.common.util.UserInputParser;
import com.example.demo.planner.plan.agent.common.PlanToolSupport;
import com.example.demo.planner.plan.dao.PlanSnapshotDao;
import com.example.demo.planner.plan.dto.entity.PlanSnapshot;
import com.example.demo.planner.plan.dto.response.PlanSnapshotContent;
import com.example.demo.planner.plan.service.action.PlanAddAction;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 개선된 PlaceRecommendTools
 * 
 * 주요 개선사항:
 * 1. Tool Description에 응답 가공 지침 추가
 * 2. 자연스러운 응답 예시 제공
 * 3. Agent가 자유롭게 표현하도록 유도
 */
@Component("placeRecommendTools")
@RequiredArgsConstructor
@Slf4j
public class PlaceRecommendTools {

    private final EmbeddingModel embeddingModel;
    private final PlanAddAction addAction;

    private final JdbcTemplate jdbcTemplate;
    private final PlanSnapshotDao planSnapshotDao;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PlanToolSupport support;

    private PlanSnapshotContent currentPlanSnapshot;

    // ===============================
    // 메인 추천 Tool
    // ===============================
    @Tool(name = "recommendPlace", description = """
            사용자가 장소를 탐색하거나 추천을 요청할 때 사용합니다.
            
            사용 예시:
            - "강남 근처 추천해줘"
            - "카페 좀 알려줘"
            - "맛집 어디가 좋아?"
            - "2일차에 갈 만한 곳 알려줘"
            
            ⚠️ 응답 처리 방법:
            이 Tool은 장소 정보를 기본 포맷으로 반환합니다.
            사용자에게 전달할 때는 다음처럼 자연스럽게 가공하세요:
            
            Tool 반환 예시:
            "추천 장소 목록입니다:
            
            1. 스타벅스 강남점
             - 주소: 서울 강남구...
             - 설명: 대형 카페
            
            2. 카페베네 역삼점
             - 주소: 서울 강남구...
             - 설명: 조용한 분위기"
            
            좋은 응답 (자연스럽게 가공):
            "강남 카페 찾아봤어요! ☕
            
            1. 스타벅스 강남점 - 넓고 쾌적해요
            2. 카페베네 역삼점 - 조용한 분위기
            
            어떤 곳이 마음에 드세요?"
            
            나쁜 응답 (그대로 복붙):
            "추천 장소 목록입니다: 1. 스타벅스..."
            
            핵심 규칙:
            - 장소 번호와 이름은 정확히 유지
            - 나머지는 친근하게 표현
            - 이모지는 적절히 사용 (1-2개)
            """)
    public String recommendPlace(
            @ToolParam(description = "추천 요청 문장") String query,
            ToolContext toolContext) {

        String conversationId = getConversationId(toolContext);

        support.clearAddCandidateState(conversationId);
        log.info("🧹 [recommendPlace] 장소 후보 상태 클리어");

        // 1. 현재 일정 로드
        loadCurrentPlan(toolContext);

        boolean hasPlanContext = currentPlanSnapshot != null;

        // 2. 날짜 검증
        if (hasPlanContext && !validateDate(query)) {
            return "요청하신 날짜는 현재 여행 일정 범위를 벗어납니다.";
        }

        // 3. 지역 추출
        SeoulRegion region = SeoulRegion.fromUserInput(query);

        if (hasPlanContext) {
            log.info("📍 일정 기반 추천 요청 (query={})", query);
        } else if (region != null) {
            log.info("📍 지역 기반 추천 요청 (region={}, query={})", region, query);
        } else {
            log.info("📍 일반 추천 요청 (query={})", query);
        }

        return dbSearch(query, region, conversationId);
    }

    // ===============================
    // 추천 목록 다시 보여주기
    // ===============================
    @Tool(name = "showLastRecommendations", description = """
            가장 최근에 추천된 장소 목록을 다시 보여줍니다.
            
            사용 시점:
            - "아까 추천해준 거 다시 보여줘"
            - "추천 목록 다시 확인할게"
            """)
    public String showLastRecommendations(ToolContext toolContext) {

        String conversationId = getConversationId(toolContext);

        List<Map<String, Object>> recs = support.getLastRecommendations(conversationId);

        if (recs == null || recs.isEmpty()) {
            return "아직 추천된 장소가 없습니다. 먼저 추천을 받아주세요.";
        }

        return formatRecommendationList(
                recs.stream()
                        .limit(5)
                        .toList());
    }

    // ===============================
    // 추천 장소 추가
    // ===============================
    @Transactional
    @Tool(description = """
            추천된 장소 목록에서 사용자가 선택한 번호의 장소를 일정에 추가합니다.
            
            사용 시점:
            - 추천 후 사용자가 "3번 추가해줘" 같은 숫자를 말할 때
            
            ⚠️ 응답 처리 방법:
            
            Tool 반환 예시:
            "1일차 3번째에 '설눈'을(를) 추가했습니다. (버전 5)"
            
            좋은 응답 (자연스럽게 가공):
            "설눈을 1일차 점심 시간에 추가했어요! ✨
            파스타가 유명하다던데, 맛있게 드세요!"
            
            또는:
            "1일차에 설눈 추가 완료! 🍝
            다른 곳도 더 추가할까요?"
            
            핵심 규칙:
            - 일차 번호 (1일차) 정확히 유지
            - 장소명 ('설눈') 절대 변경 금지
            - 위치(3번째) → "점심 시간", "맨 뒤" 등으로 자연스럽게 표현 가능
            - 버전 번호 → 굳이 언급 안 해도 됨 (기술적 정보)
            """)
    public String addRecommendedPlace(
            @ToolParam(description = "몇 일차인지 (1부터). 없으면 null", required = false) Integer dayIndex,

            @ToolParam(description = "몇 번째 위치인지 (1부터). 없으면 null", required = false) Integer position,

            @ToolParam(description = "추천 목록에서 선택한 번호 (1부터 시작)", required = true) Integer index,

            ToolContext toolContext) {

        log.info("🧩 addRecommendedPlace 호출: dayIndex={}, position={}, index={}",
                dayIndex, position, index);

        String conversationId = getConversationId(toolContext);
        Long planId = support.getPlanId(conversationId);

        // 1. 상태 확인
        PlanToolSupport.PendingSelectionType selectionType = support.getPendingSelectionType(conversationId);

        if (selectionType != PlanToolSupport.PendingSelectionType.RECOMMENDATION) {
            log.warn("⚠️ [상태 불일치] 현재 상태={}, 필요 상태=RECOMMENDATION", selectionType);
            return "추천 목록이 없습니다. 먼저 여행지 추천을 받아주세요.";
        }

        // 2. 추천 목록 확인
        List<Map<String, Object>> recs = support.getLastRecommendations(conversationId);
        if (recs == null || recs.isEmpty()) {
            return "먼저 여행지 추천을 받아주세요.";
        }

        // ✅ ToolParam 무시하고 사용자 발화에서 추출
        dayIndex = null;
        position = null;
        index = null;

        String userMessage = (String) toolContext.getContext().get("userMessage");

        // 3. 컨텍스트 복원
        PlanToolSupport.SelectionContext context = support.getSelectionContext(conversationId);
        if (context != null) {
            index = context.getIndex();
            dayIndex = context.getDayIndex();
            position = context.getPosition();
            log.info("📥 [컨텍스트 복원] index={}, dayIndex={}, position={}",
                    index, dayIndex, position);
        }

        // 4. 사용자 발화에서 추출
        Integer indexFromText = UserInputParser.parseIndex(userMessage);
        Integer dayFromText = UserInputParser.parseDayIndex(userMessage);
        Integer posFromText = UserInputParser.parsePosition(userMessage);

        if (indexFromText != null) {
            support.updateIndex(conversationId, indexFromText);
            index = indexFromText;
        }
        if (dayFromText != null) {
            support.updateDayIndex(conversationId, dayFromText);
            dayIndex = dayFromText;
        }
        if (posFromText != null) {
            support.updatePosition(conversationId, posFromText);
            position = posFromText;
        }

        // 5. index 검증
        if (index == null || index < 1 || index > recs.size()) {
            return String.format("추천 목록은 1번부터 %d번까지 있습니다.", recs.size());
        }

        Map<String, Object> selected = recs.get(index - 1);

        // 6. 부족한 값 질문
        if (dayIndex == null) {
            return String.format("""
                    %d번 '%s'을(를) 몇 일차에 추가할까요?
                    
                    예시: "2일차에 추가해줘"
                    """, index, selected.get("title"));
        }

        if (position == null) {
            return String.format("""
                    %d번 '%s'을(를) %d일차 몇 번째에 추가할까요?
                    
                    예시: "맨 뒤에", "첫 번째에"
                    """, index, selected.get("title"), dayIndex);
        }

        // 7. 실행
        Long placeId = ((Number) selected.get("id")).longValue();

        try {
            String newPlaceTitle = addAction.addPlaceFromRecommendation(
                    planId,
                    dayIndex,
                    position,
                    placeId);

            support.clearPendingSelection(conversationId);
            log.info("🧹 [추천 선택 완료] 상태 클리어");

            Integer versionNo = support.saveSnapshot(planId);

            // ✅ 기본 정보만 반환 (Agent가 자연스럽게 가공)
            return String.format(
                    "%d일차 %d번째에 '%s'을(를) 추가했습니다. (버전 %d)",
                    dayIndex,
                    position,
                    newPlaceTitle,
                    versionNo);

        } catch (Exception e) {
            log.error("❌ 추천 장소 추가 실패", e);
            return "추천 장소를 추가하는 중 오류가 발생했습니다: " + e.getMessage();
        }
    }

    // ===============================
    // DB 검색 (핵심 로직)
    // ===============================
    private String dbSearch(String query, SeoulRegion region, String conversationId) {
        try {
            float[] vector = getQueryVector(query);
            String vectorStr = Arrays.toString(vector).replace(" ", "");

            // 1. 일정에 이미 포함된 장소 제외
            Set<String> existingTitles = new HashSet<>();
            if (currentPlanSnapshot != null) {
                currentPlanSnapshot.getDays()
                        .forEach(day -> day.getSchedules()
                                .forEach(s -> existingTitles.add(s.getTitle())));
            }

            String excludeTitleClause = existingTitles.isEmpty()
                    ? ""
                    : " AND title NOT IN (" +
                            existingTitles.stream()
                                    .map(t -> "'" + t.replace("'", "''") + "'")
                                    .collect(Collectors.joining(","))
                            + ")";

            Set<Long> excludedIds = support.getRecommendedIds(conversationId);

            String excludeIdClause = excludedIds.isEmpty()
                    ? ""
                    : " AND id NOT IN (" +
                            excludedIds.stream()
                                    .map(String::valueOf)
                                    .collect(Collectors.joining(","))
                            + ")";

            // 2. 지역 조건
            String regionClause = region == null
                    ? ""
                    : " AND zone_id = '" + region.getZoneId() + "'";

            // 3. SQL
            String sql = """
                    SELECT id, title, address, tel, first_image2, description,
                           (embedding <=> ?::vector) AS distance
                    FROM travel_places
                    WHERE 1=1
                    %s
                    %s
                    %s
                    ORDER BY (embedding <=> ?::vector) + (random() * 0.10)
                    LIMIT 20
                    """.formatted(
                    excludeTitleClause,
                    regionClause,
                    excludeIdClause);

            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, vectorStr, vectorStr);

            if (results.isEmpty()) {
                return "조건에 맞는 새로운 추천 장소를 찾지 못했습니다.";
            }

            // 4. 마지막 추천 목록 저장
            support.setLastRecommendations(conversationId, results);
            support.addRecommendedIds(conversationId, results);

            // 5. 상위 5개만 표시
            List<Map<String, Object>> top5 = results.stream()
                    .limit(5)
                    .toList();

            return formatRecommendationList(top5);
        } catch (Exception e) {
            log.error("❌ 추천 DB 검색 실패", e);
            return "추천 검색 중 오류가 발생했습니다.";
        }
    }

    // ===============================
    // 현재 일정 로드
    // ===============================
    private void loadCurrentPlan(ToolContext toolContext) {
        try {
            Long userId = (Long) toolContext.getContext().get("userId");

            PlanSnapshot snapshot = planSnapshotDao.selectLatestPlanSnapshotByUserId(userId);

            if (snapshot == null) {
                currentPlanSnapshot = null;
                return;
            }

            currentPlanSnapshot = objectMapper.readValue(
                    snapshot.getSnapshotJson(),
                    PlanSnapshotContent.class);

        } catch (Exception e) {
            log.error("⚠️ 현재 여행 계획 조회 실패", e);
            currentPlanSnapshot = null;
        }
    }

    // ===============================
    // 날짜 검증
    // ===============================
    private boolean validateDate(String input) {
        if (currentPlanSnapshot == null) {
            return true;
        }

        LocalDate extracted = extractDateFromInput(input);
        if (extracted == null) {
            return true;
        }

        LocalDate start = LocalDate.parse(currentPlanSnapshot.getStartDate());
        LocalDate end = LocalDate.parse(currentPlanSnapshot.getEndDate());

        return !extracted.isBefore(start)
                && !extracted.isAfter(end);
    }

    // ===============================
    // Embedding
    // ===============================
    private float[] getQueryVector(String query) {
        EmbeddingResponse response = embeddingModel.embedForResponse(List.of(query));
        return response.getResult().getOutput();
    }

    // ===============================
    // 날짜 추출
    // ===============================
    private LocalDate extractDateFromInput(String input) {
        Pattern pattern = Pattern.compile("(\\d{4})[/-]?(\\d{2})[/-]?(\\d{2})");
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            try {
                return LocalDate.parse(
                        matcher.group(1) + "-" +
                                matcher.group(2) + "-" +
                                matcher.group(3),
                        DateTimeFormatter.ISO_DATE);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    // ===============================
    // 렌더링 (기본 포맷)
    // ===============================
    private String formatRecommendationList(List<Map<String, Object>> recs) {
        StringBuilder sb = new StringBuilder("추천 장소 목록입니다:\n\n");

        for (int i = 0; i < recs.size(); i++) {
            Map<String, Object> r = recs.get(i);
            sb.append(String.format(
                    "%d. %s\n - 주소: %s\n - 설명: %s\n\n",
                    i + 1,
                    r.get("title"),
                    r.get("address"),
                    r.getOrDefault("description", "")));
        }
        return sb.toString();
    }

    // ===============================
    // 공통
    // ===============================
    private String getConversationId(ToolContext toolContext) {
        return (String) toolContext.getContext().get("conversationId");
    }
}
