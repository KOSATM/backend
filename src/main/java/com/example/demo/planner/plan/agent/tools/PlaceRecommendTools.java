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

import com.example.demo.common.chat.intent.dto.SeoulRegion;
import com.example.demo.planner.plan.agent.common.PlanToolSupport;
import com.example.demo.planner.plan.dao.PlanSnapshotDao;
import com.example.demo.planner.plan.dto.entity.PlanSnapshot;
import com.example.demo.planner.plan.dto.response.PlanSnapshotContent;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component("placeRecommendTools")
@RequiredArgsConstructor
@Slf4j
public class PlaceRecommendTools {

    private final EmbeddingModel embeddingModel;
    private final JdbcTemplate jdbcTemplate;
    private final PlanSnapshotDao planSnapshotDao;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PlanToolSupport support;

    // 요청 단위 상태 (스레드 세이프 필요 없음: Tool 호출 단위)
    private PlanSnapshotContent currentPlanSnapshot;

    // ===============================
    // 메인 추천 Tool
    // ===============================
    @Tool(name = "recommendPlace", description = """
            사용자의 여행 일정 또는 지역 요청을 참고하여
            동선을 고려한 여행지를 추천합니다.

            IMPORTANT:
            - 일정은 수정하지 않습니다.
            - 장소를 추가하거나 교체하지 않습니다.
            - 추천 후보만 제공합니다.
            """)
    public String recommendPlace(
            @ToolParam(description = "추천 요청 문장") String query,
            ToolContext toolContext) {

        String conversationId = getConversationId(toolContext);

        loadCurrentPlan(toolContext);

        if (!validateDate(query)) {
            return "요청하신 날짜는 현재 여행 일정 범위를 벗어납니다.";
        }

        SeoulRegion region = SeoulRegion.fromUserInput(query);

        log.info("📍 여행지 추천 요청 (region={}, query={})",
                region != null ? region.name() : "일정 기반",
                query);

        return dbSearch(query, region, conversationId);
    }

    // ===============================
    // 추천 목록 다시 보여주기
    // ===============================
    @Tool(name = "showLastRecommendations", description = """
            가장 최근에 추천된 장소 목록을 다시 보여줍니다.
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
            .toList()
        );
    }

    // ===============================
    // DB 검색 (핵심 로직)
    // ===============================
    private String dbSearch(
            String query,
            SeoulRegion region,
            String conversationId) {

        try {
            float[] vector = getQueryVector(query);
            String vectorStr = Arrays.toString(vector).replace(" ", "");

            // 1. 일정에 이미 포함된 장소 title 제외
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

            // 2. 지역 조건
            String regionClause = region == null
                    ? ""
                    : " AND zone_id = '" + region.getZoneId() + "'";

            // 3. SQL (이전 추천 결과는 제외 ❌)
            String sql = """
                    SELECT id, title, address, tel, first_image2, description,
                           (embedding <=> ?::vector) AS distance
                    FROM travel_places
                    WHERE 1=1
                    %s
                    %s
                    ORDER BY (embedding <=> ?::vector) + (random() * 0.03)
                    LIMIT 20
                    """.formatted(
                    excludeTitleClause,
                    regionClause);

            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, vectorStr, vectorStr);

            if (results.isEmpty()) {
                return "조건에 맞는 새로운 추천 장소를 찾지 못했습니다.";
            }

            // 4. 마지막 추천 목록 저장 (전체 20개)
            support.setLastRecommendations(conversationId, results);

            // 5. 사용자에게는 상위 5개만 보여줌
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
    // 렌더링
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
