package com.example.demo.planner.plan.agent;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.example.demo.common.chat.intent.dto.SeoulRegion;
import com.example.demo.planner.plan.agent.test.date.DateParser;
import com.example.demo.planner.plan.agent.test.date.DurationParser;
import com.example.demo.planner.plan.dao.PlanDao;
import com.example.demo.planner.plan.dto.entity.GeneratedTravelPlan;
import com.example.demo.planner.plan.dto.entity.TravelPlaces;
import com.example.demo.planner.plan.utils.CategoryNames;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class TravelPlanAgent {

    private final ChatClient chatClient;
    // private final ChatMemory chatMemory;
    private final EmbeddingModel embeddingModel;
    private final PlanDao planDao;

    /**
     * 일정 강도 Enum
     */
    private enum Pace {
        RELAXED(3), // 널널: 3개
        NORMAL(5), // 보통: 5개
        TIGHT(7); // 빡빡: 7개

        private final int placesPerDay;

        Pace(int placesPerDay) {
            this.placesPerDay = placesPerDay;
        }

        public int getPlacesPerDay() {
            return placesPerDay;
        }

        public String getLabel() {
            return switch (this) {
                case RELAXED -> "널널";
                case NORMAL -> "보통";
                case TIGHT -> "빡빡";
            };
        }

        public static Pace fromString(String str) {
            if (str == null || str.trim().isEmpty()) {
                return NORMAL;
            }

            String normalized = str.toLowerCase().trim();

            if (normalized.contains("빡빡") || normalized.contains("tight") ||
                    normalized.contains("바쁜") || normalized.contains("많이")) {
                return TIGHT;
            }

            if (normalized.contains("널널") || normalized.contains("relaxed") ||
                    normalized.contains("여유") || normalized.contains("느긋")) {
                return RELAXED;
            }

            return NORMAL;
        }
    }

    public TravelPlanAgent(
            ChatClient.Builder chatClientBuilder,
            EmbeddingModel embeddingModel,
            PlanDao planDao) {

        this.embeddingModel = embeddingModel;
        this.planDao = planDao;
        // this.chatMemory = MessageWindowChatMemory.builder().maxMessages(20).build();
        this.chatClient = chatClientBuilder
                .defaultSystem("""
                        당신은 서울 여행 일정 생성 AI입니다.

                        # Tool
                        createSeoulTravelPlan(duration, style, location, pace)

                        # 중요 규칙

                        ⭐ duration만 있으면 즉시 Tool 호출!
                        - style, location, pace는 선택사항
                        - 없으면 null로 전달

                        # 파라미터 추출

                        1. **duration (필수):**
                           다양한 표현 인식:
                           - "1일" → 1, "당일치기" → 1, "하루" → 1
                           - "2일" → 2, "이틀" → 2
                           - "3일" → 3, "2박3일" → 3
                           - "5일" → 5

                           ⚠️ duration이 없을 때만 물어보기!

                        2. **style (선택):**
                           - "kpop", "힐링", "맛집" 등

                        3. **location (선택):**
                           - "강남", "강남, 홍대" 등

                        4. **pace (선택):**
                           - "빡빡하게", "널널하게"

                        # 예시

                        "1일 당일치기" → createSeoulTravelPlan(1, null, null, null)
                        "5일 장기 여행" → createSeoulTravelPlan(5, null, null, null)
                        "3일 kpop 서초" → createSeoulTravelPlan(3, "kpop", "서초", null)

                        # 절대 규칙
                        - "N일 뒤", "다음주", "이번주" 같은 표현은
                        → duration 이 아니라 startDate 로 해석한다

                        ✅ duration 있으면 → 즉시 Tool 호출
                        ❌ duration 없으면 → 물어보기
                        ❌ style/location 없다고 추가 질문 금지!

                        """)
                .defaultTools(this)
                // .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();

        log.info("✅ SeoulTravelPlanAgent 초기화 완료");

    }

    public String chat(String message) {
        log.info("💬 메시지: {}", message);
        return chatClient.prompt().user(message).call().content();
    }

    // =========================================================
    // ✅ 기존 Tool: 텍스트 일정 (네 코드 그대로 유지)
    // =========================================================
    @Tool(name = "createSeoulTravelPlan")
    public String createSeoulTravelPlan(
            @ToolParam(description = "여행 기간(일). 필수!") Integer duration,

            @ToolParam(description = "여행 스타일. 예: 'kpop', '힐링'", required = false) String style,

            @ToolParam(description = "선호 지역. 예: '강남', '강남, 홍대'", required = false) String location,

            @ToolParam(description = "일정 강도. '빡빡', '널널'. 없으면 보통", required = false) String pace,

            @ToolParam(description = "사용자가 말한 여행 시작 시점의 원문 표현 그대로. 예: '3일뒤', '다음주 월요일', '이번주말'. 날짜 계산하지 말 것, yyyy-MM-dd로 변환하지 말것", required = false) String startDateText) {

        log.info("🔧 Tool 호출: duration={}, style={}, location={}, pace={}, startDateText={}",
                duration, style, location, pace, startDateText);

        if (duration == null || duration <= 0) {
            return "여행 기간을 지정해주세요. 며칠 동안 여행하실 예정인가요?";
        }

        int safeDuration;

        if (duration == null || duration <= 0) {
            log.warn("duration 누락/비정상 → DurationParser 기본값 사용");
            safeDuration = DurationParser.parse(null); // 기본 1일
        } else {
            safeDuration = Math.min(duration, 7); // 정책: 최대 7일
        }

        // ⭐ 여기서 날짜 확정 (딱 1번만)
        LocalDate startDate = DateParser.parse(startDateText);
        LocalDate endDate = startDate.plusDays(safeDuration - 1);

        log.info("📅 여행 날짜 확정: {} ~ {} ({}일)", startDate, endDate, safeDuration);

        try {
            List<String> searchQueries = generateSearchQueries(style);
            List<TravelPlaces> places = multiQueryVectorSearch(searchQueries, location, safeDuration);

            if (places.isEmpty()) {
                return "검색 결과가 없습니다. 다른 조건으로 시도해주세요.";
            }

            Map<String, List<TravelPlaces>> clusters = groupByZone(places);
            return buildPlan(safeDuration, clusters, style, location, pace);

        } catch (Exception e) {
            log.error("❌ 오류", e);
            return "일정 생성 중 오류가 발생했습니다: " + e.getMessage();
        }
    }

    // =========================================================
    // ✅ 신규 Tool: 구조화 일정 (GeneratedTravelPlan)
    // =========================================================
    @Tool(name = "createSeoulTravelPlanStructured")
    public GeneratedTravelPlan createSeoulTravelPlanStructured(
            @ToolParam(description = "여행 기간(일). 필수!") Integer duration,

            @ToolParam(description = "여행 스타일. 예: 'kpop', '힐링'", required = false) String style,

            @ToolParam(description = "선호 지역. 예: '강남', '강남, 홍대'", required = false) String location,

            @ToolParam(description = "일정 강도. '빡빡', '널널'. 없으면 보통", required = false) String pace,

            @ToolParam(description = "사용자가 말한 여행 시작 시점의 원문 표현 그대로. 예: '3일뒤', '다음주 월요일', '이번주말'. 날짜 계산하지 말 것, yyyy-MM-dd로 변환하지 말것", required = false) String startDateText) {

        log.info("🔧 Tool 호출(STRUCTURED): duration={}, style={}, location={}, pace={}, startDateText={}",
                duration, style, location, pace, startDateText);

        if (duration == null || duration <= 0) {
            // 구조화도 동일하게 “duration 없으면” 에러 응답 대신 빈 구조를 주고 싶으면 여기 정책 바꾸면 됨
            // 지금은 기존 텍스트 Tool과 정책을 맞추기 위해 예외 대신 빈 구조 반환
            int d = DurationParser.parse(null);
            LocalDate s = DateParser.parse(startDateText);
            LocalDate e = s.plusDays(d - 1);
            return new GeneratedTravelPlan(d, Pace.fromString(pace).getLabel(), List.of(), s, e);
        }

        int safeDuration;

        if (duration == null || duration <= 0) {
            log.warn("duration 누락/비정상 → DurationParser 기본값 사용");
            safeDuration = DurationParser.parse(null); // 기본 1일
        } else {
            safeDuration = Math.min(duration, 7); // 정책: 최대 7일
        }

        // ⭐ 여기서 날짜 확정 (딱 1번만)
        LocalDate startDate = DateParser.parse(startDateText);
        LocalDate endDate = startDate.plusDays(safeDuration - 1);

        log.info("📅 여행 날짜 확정(STRUCTURED): {} ~ {} ({}일)", startDate, endDate, safeDuration);

        try {
            List<String> searchQueries = generateSearchQueries(style);
            List<TravelPlaces> places = multiQueryVectorSearch(searchQueries, location, safeDuration);

            if (places.isEmpty()) {
                return new GeneratedTravelPlan(
                        safeDuration,
                        Pace.fromString(pace).getLabel(),
                        List.of(),
                        startDate,
                        endDate);
            }

            Map<String, List<TravelPlaces>> clusters = groupByZone(places);
            return buildPlanStructured(safeDuration, clusters, style, location, pace, startDate, endDate);

        } catch (Exception e) {
            log.error("❌ 오류(STRUCTURED)", e);
            // 실패 시에도 구조는 반환 (원하면 throw로 바꿔도 됨)
            return new GeneratedTravelPlan(
                    safeDuration,
                    Pace.fromString(pace).getLabel(),
                    List.of(),
                    startDate,
                    endDate);
        }
    }

    /**
     * LLM이 검색 쿼리 생성
     */
    private List<String> generateSearchQueries(String style) {
        log.info("  🤖 LLM 쿼리 생성: style={}", style);

        if (style == null || style.trim().isEmpty()) {
            return List.of("인기 관광지", "추천 맛집", "유명 카페");
        }

        String prompt = String.format("""
                다음 여행 스타일에 맞는 장소를 찾기 위한 검색 쿼리를 생성하세요.

                여행 스타일: %s

                요구사항:
                1. 스타일 관련 구체적 쿼리 3-4개 생성
                2. 기본 필수 쿼리 3-4개 추가 (맛집, 카페, 관광지 등)
                3. 각 쿼리는 3-6단어로 간결하게
                4. 벡터 검색에 최적화된 구체적 표현 사용

                출력 형식: 쉼표로 구분된 리스트만 출력 (설명 없이)

                예시:
                입력: "kpop"
                출력: kpop 명소, 아이돌 굿즈샵, 케이팝 카페, 엔터 회사, 맛집, 감성 카페, 관광지

                이제 '%s' 스타일의 검색 쿼리를 생성하세요:
                """, style, style);

        try {
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            List<String> queries = Arrays.stream(response.split(","))
                    .map(String::trim)
                    .filter(q -> !q.isEmpty() && q.length() > 1)
                    .collect(Collectors.toList());

            log.info("  ✅ LLM 생성 쿼리 ({}개): {}", queries.size(), queries);
            return queries;

        } catch (Exception e) {
            log.error("  ❌ LLM 쿼리 생성 실패, fallback 사용", e);
            return Arrays.asList(
                    style + " 명소",
                    style + " 추천",
                    "맛집", "카페", "관광지");
        }
    }

    private List<TravelPlaces> multiQueryVectorSearch(
            List<String> searchQueries, String location, int duration) {

        log.info("  📍 벡터 검색: {}개 쿼리", searchQueries.size());

        List<TravelPlaces> allResults = new ArrayList<>();
        List<String> zoneIds = extractZoneIds(location, duration);

        if (!zoneIds.isEmpty()) {
            log.info("  🗺️ 지역 필터: {}", zoneIds);
        }

        for (String query : searchQueries) {
            log.info("    - 쿼리: {}", query);

            float[] embedding = embeddingModel.embed(query);

            List<TravelPlaces> results = planDao.vectorSearch(
                    embedding,
                    zoneIds.isEmpty() ? null : zoneIds,
                    50);

            Collections.shuffle(results);
            allResults.addAll(results.stream()
                    .limit(20)
                    .collect(Collectors.toList()));

            log.info("      → {}개 중 랜덤 20개", results.size());
        }

        List<TravelPlaces> uniquePlaces = deduplicatePlaces(allResults);
        log.info("  ✅ 총 {}개 장소", uniquePlaces.size());

        return uniquePlaces;
    }

    private List<String> extractZoneIds(String location, int duration) {
        if (location == null || location.trim().isEmpty()) {
            return selectRandomZones(duration);
        }

        String cleaned = location.replace("근처", "")
                .replace("쪽", "")
                .replace("방면", "")
                .trim();
        String[] parts = cleaned.split("[,、،/\\s+]+|랑|과|와|하고|나|혹은");

        List<String> zoneIds = new ArrayList<>();

        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty())
                continue;

            SeoulRegion region = SeoulRegion.fromUserInput(trimmed);
            if (region != null) {
                String zoneId = region.getZoneId();
                if (!zoneIds.contains(zoneId)) {
                    zoneIds.add(zoneId);
                }
            }
        }

        if (!zoneIds.isEmpty()) {
            log.info("  🔍 사용자 지정 지역: {} → {}", location, zoneIds);
        }

        return zoneIds;
    }

    private List<String> selectRandomZones(int duration) {
        int zoneCount = switch (duration) {
            case 1 -> 1;
            case 2 -> 2;
            case 3 -> 2 + new Random().nextInt(2);
            default -> 3;
        };

        List<SeoulRegion> allRegions = new ArrayList<>(List.of(SeoulRegion.values()));
        Collections.shuffle(allRegions);

        List<String> selected = allRegions.stream()
                .limit(zoneCount)
                .map(SeoulRegion::getZoneId)
                .collect(Collectors.toList());

        log.info("  🎲 랜덤 지역 ({}일 → {}개): {}", duration, zoneCount, selected);

        return selected;
    }

    private List<TravelPlaces> deduplicatePlaces(List<TravelPlaces> places) {
        return places.stream()
                .collect(Collectors.toMap(
                        TravelPlaces::getId,
                        p -> p,
                        (p1, p2) -> p1))
                .values()
                .stream()
                .collect(Collectors.toList());
    }

    private Map<String, List<TravelPlaces>> groupByZone(List<TravelPlaces> places) {
        return places.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getZoneId() != null ? p.getZoneId() : "기타"));
    }

    private String buildPlan(int duration, Map<String, List<TravelPlaces>> clusters,
            String style, String location, String paceStr) {

        Pace pace = Pace.fromString(paceStr);
        int placesPerDay = pace.getPlacesPerDay();

        StringBuilder plan = new StringBuilder();
        plan.append("=== Seoul Travel Itinerary ===\n\n");
        plan.append(String.format("📅 Duration: %d day%s\n", duration, duration > 1 ? "s" : ""));

        if (style != null) {
            plan.append(String.format("🎨 Theme: %s\n", style));
        }
        if (location != null) {
            plan.append(String.format("📍 Areas: %s\n", location));
        }
        plan.append(String.format("⏱️ Pace: %s\n\n", pace.getLabel()));

        List<String> zoneKeys = new ArrayList<>(clusters.keySet());
        Set<Long> usedPlaceIds = new HashSet<>();

        for (int day = 1; day <= duration; day++) {
            String zone = zoneKeys.get((day - 1) % zoneKeys.size());
            List<TravelPlaces> zonePlaces = clusters.get(zone);

            // ⭐ 당일치기는 중간날로 처리
            boolean isFirstDay = (duration > 1 && day == 1);
            boolean isLastDay = (duration > 1 && day == duration);

            buildDayPlan(plan, day, zone, zonePlaces, usedPlaceIds,
                    placesPerDay, isFirstDay, isLastDay);
        }

        plan.append("✈️ Safe travels! 🌟\n");

        log.info("✅ 일정 생성 완료 ({}개 장소)", usedPlaceIds.size());
        return plan.toString();
    }

    // =========================================================
    // ✅ 구조화 Plan 빌더 추가 (기존 buildPlan을 “복제+변형”)
    // =========================================================
    private GeneratedTravelPlan buildPlanStructured(
            int duration,
            Map<String, List<TravelPlaces>> clusters,
            String style,
            String location,
            String paceStr,
            LocalDate startDate,
            LocalDate endDate) {

        Pace pace = Pace.fromString(paceStr);
        int placesPerDay = pace.getPlacesPerDay();

        List<String> zoneKeys = new ArrayList<>(clusters.keySet());
        Set<Long> usedPlaceIds = new HashSet<>();

        List<GeneratedTravelPlan.GeneratedDay> days = new ArrayList<>();

        for (int day = 1; day <= duration; day++) {
            String zone = zoneKeys.get((day - 1) % zoneKeys.size());
            List<TravelPlaces> zonePlaces = clusters.get(zone);
            LocalDate dayDate = startDate.plusDays(day - 1);

            // ⭐ 당일치기는 중간날로 처리
            boolean isFirstDay = (duration > 1 && day == 1);
            boolean isLastDay = (duration > 1 && day == duration);

            List<GeneratedTravelPlan.GeneratedPlace> generatedPlaces = buildDayPlanStructured(
                    day, zone, zonePlaces, usedPlaceIds, placesPerDay, isFirstDay, isLastDay, startDate);

            days.add(new GeneratedTravelPlan.GeneratedDay(day, dayDate, generatedPlaces));
        }

        log.info("✅ 일정 생성 완료 ({}개 장소)", usedPlaceIds.size());

        return new GeneratedTravelPlan(
                duration,
                pace.getLabel(),
                days,
                startDate,
                endDate);
    }

    /**
     * ⭐ 하루 일정 생성 (첫날/중간/마지막날 모두 처리)
     */
    /**
     * ⭐ 하루 일정 생성 (연속 카테고리 방지)
     */
    /**
     * ⭐ 하루 일정 생성 (연속 카테고리 방지)
     */
    private void buildDayPlan(StringBuilder plan, int day, String zone,
            List<TravelPlaces> zonePlaces, Set<Long> usedPlaceIds,
            int placesPerDay, boolean isFirstDay, boolean isLastDay) {

        // 헤더
        plan.append(String.format("【Day %d】 %s", day, zone));
        if (isFirstDay) {
            plan.append(" (After check-in at 15:00)");
        } else if (isLastDay) {
            plan.append(" (After check-out at 11:00)");
        }
        plan.append("\n");

        if (isLastDay) {
            plan.append("  ⚠️ Airport departure at 15:00\n");
        }
        plan.append("\n");

        // 시작 시간 & 장소 개수
        int startHour;
        int maxPlaces;

        if (isFirstDay) {
            startHour = 15;
            maxPlaces = Math.max(2, placesPerDay - 2);
        } else if (isLastDay) {
            startHour = 11;
            maxPlaces = 2;
        } else {
            startHour = 9;
            maxPlaces = placesPerDay;
        }

        int currentMinutes = startHour * 60;

        if (isLastDay) {
            currentMinutes = 11 * 60 + 30;
        }

        // ⭐ 이전 카테고리 추적
        String previousCategory = null;

        // 장소별 시간 배정
        for (int i = 0; i < maxPlaces; i++) {
            int hour = currentMinutes / 60;

            if (isLastDay && currentMinutes >= 14 * 60 + 30) {
                break;
            }

            if (!isLastDay && hour >= 20) {
                break;
            }

            // 선호 카테고리 결정
            List<String> preferredCategories;
            if (isLastDay && i == 0) {
                preferredCategories = List.of(CategoryNames.FOOD);
            } else if (isLastDay && i == 1) {
                preferredCategories = List.of(CategoryNames.CAFE, CategoryNames.SPOT);
            } else {
                preferredCategories = getPreferredCategoriesByHour(hour);
            }

            // ⭐ 이전 카테고리와 같으면 제외
            if (previousCategory != null) {
                final String prevCat = previousCategory; // ⭐ final 복사본

                preferredCategories = preferredCategories.stream()
                        .filter(cat -> !cat.equals(prevCat))
                        .collect(Collectors.toList());

                // 필터링 후 비어있으면 다른 카테고리로 대체
                if (preferredCategories.isEmpty()) {
                    preferredCategories = CategoryNames.EXCLUDE_ETC.stream()
                            .filter(cat -> !cat.equals(prevCat))
                            .collect(Collectors.toList());
                }
            }

            TravelPlaces place = selectPlaceByCategory(
                    zonePlaces, usedPlaceIds, preferredCategories,
                    i, maxPlaces, isFirstDay, isLastDay);

            if (place != null) {
                String category = place.getNormalizedCategory() != null
                        ? place.getNormalizedCategory()
                        : CategoryNames.ETC;

                int duration = getCategoryDuration(category);

                if (isLastDay && i == 1) {
                    duration = 90;
                }

                int startH = currentMinutes / 60;
                int startM = currentMinutes % 60;
                int endMinutes = currentMinutes + duration;
                int endH = endMinutes / 60;
                int endM = endMinutes % 60;

                String timeSlot = String.format("%02d:%02d-%02d:%02d", startH, startM, endH, endM);
                String emoji = getCategoryEmoji(category);

                plan.append(String.format("  %s  %s %s (%s)",
                        timeSlot, emoji, place.getTitle(), category));

                if (isLastDay && i == 1) {
                    plan.append(" - Light visit before airport");
                }
                plan.append("\n");

                if (place.getAddress() != null) {
                    plan.append(String.format("         📍 %s\n", place.getAddress()));
                }

                usedPlaceIds.add(place.getId());
                currentMinutes = endMinutes;

                // ⭐ 이전 카테고리 업데이트
                previousCategory = category;
            }
        }

        if (isLastDay) {
            plan.append("\n  🚖 15:00 - Departure to Airport\n");
        }

        plan.append("\n");
    }

    // =========================================================
    // ✅ 구조화 하루 일정 생성 (기존 buildDayPlan을 “복제+변형”)
    // =========================================================
    private List<GeneratedTravelPlan.GeneratedPlace> buildDayPlanStructured(
            int day,
            String zone,
            List<TravelPlaces> zonePlaces,
            Set<Long> usedPlaceIds,
            int placesPerDay,
            boolean isFirstDay,
            boolean isLastDay,
            LocalDate startDate) {

        List<GeneratedTravelPlan.GeneratedPlace> generated = new ArrayList<>();

        // 시작 시간 & 장소 개수
        int startHour;
        int maxPlaces;

        if (isFirstDay) {
            startHour = 15;
            maxPlaces = Math.max(2, placesPerDay - 2);
        } else if (isLastDay) {
            startHour = 11;
            maxPlaces = 2;
        } else {
            startHour = 9;
            maxPlaces = placesPerDay;
        }

        int currentMinutes = startHour * 60;

        if (isLastDay) {
            currentMinutes = 11 * 60 + 30;
        }

        // ⭐ 이전 카테고리 추적
        String previousCategory = null;

        // 장소별 시간 배정
        for (int i = 0; i < maxPlaces; i++) {
            int hour = currentMinutes / 60;

            if (isLastDay && currentMinutes >= 14 * 60 + 30) {
                break;
            }

            if (!isLastDay && hour >= 20) {
                break;
            }

            // 선호 카테고리 결정
            List<String> preferredCategories;
            if (isLastDay && i == 0) {
                preferredCategories = List.of(CategoryNames.FOOD);
            } else if (isLastDay && i == 1) {
                preferredCategories = List.of(CategoryNames.CAFE, CategoryNames.SPOT);
            } else {
                preferredCategories = getPreferredCategoriesByHour(hour);
            }

            // ⭐ 이전 카테고리와 같으면 제외
            if (previousCategory != null) {
                final String prevCat = previousCategory; // ⭐ final 복사본

                preferredCategories = preferredCategories.stream()
                        .filter(cat -> !cat.equals(prevCat))
                        .collect(Collectors.toList());

                // 필터링 후 비어있으면 다른 카테고리로 대체
                if (preferredCategories.isEmpty()) {
                    preferredCategories = CategoryNames.EXCLUDE_ETC.stream()
                            .filter(cat -> !cat.equals(prevCat))
                            .collect(Collectors.toList());
                }
            }

            TravelPlaces place = selectPlaceByCategory(
                    zonePlaces, usedPlaceIds, preferredCategories,
                    i, maxPlaces, isFirstDay, isLastDay);

            if (place != null) {
                String category = place.getNormalizedCategory() != null
                        ? place.getNormalizedCategory()
                        : CategoryNames.ETC;

                int durationMin = getCategoryDuration(category);

                if (isLastDay && i == 1) {
                    durationMin = 90;
                }

                int startH = currentMinutes / 60;
                int startM = currentMinutes % 60;
                int endMinutes = currentMinutes + durationMin;
                int endH = endMinutes / 60;
                int endM = endMinutes % 60;

                LocalTime startTime = LocalTime.of(startH, startM);
                LocalTime endTime = LocalTime.of(endH, endM);

                // KST 기준 OffsetDateTime (필요하면 시스템 정책에 맞게 바꿔)
                ZoneOffset kst = ZoneOffset.ofHours(9);
                LocalDate dayDate = startDate.plusDays(day - 1);

                OffsetDateTime startAt = dayDate.atTime(startTime).atOffset(kst);
                OffsetDateTime endAt = dayDate.atTime(endTime).atOffset(kst);

                generated.add(new GeneratedTravelPlan.GeneratedPlace(
                        place.getTitle(), // title
                        place.getTitle(), // placeName (원하면 별도 필드 사용)
                        startAt,
                        endAt,
                        place.getLat(),
                        place.getLng(),
                        place.getAddress(),
                        category,
                        place.getFirstImage(),
                        place.getFirstImage2()));

                usedPlaceIds.add(place.getId());
                currentMinutes = endMinutes;

                // ⭐ 이전 카테고리 업데이트
                previousCategory = category;
            }
        }

        return generated;
    }

    private int getCategoryDuration(String category) {
        return switch (category) {
            case CategoryNames.FOOD -> 75;
            case CategoryNames.CAFE -> 60;
            case CategoryNames.SPOT -> 120;
            case CategoryNames.SHOPPING -> 90;
            case CategoryNames.EVENT -> 120;
            default -> 90;
        };
    }

    private List<String> getPreferredCategoriesByHour(int hour) {
        if (hour == 12 || hour == 11) {
            return List.of(CategoryNames.FOOD);
        }

        if (hour >= 18 && hour <= 19) {
            return List.of(CategoryNames.FOOD, CategoryNames.CAFE);
        }

        if (hour >= 9 && hour < 12) {
            return List.of(CategoryNames.SPOT, CategoryNames.EVENT, CategoryNames.SHOPPING);
        }

        if (hour >= 13 && hour < 18) {
            return List.of(CategoryNames.SPOT, CategoryNames.CAFE, CategoryNames.SHOPPING);
        }

        return List.of(CategoryNames.SPOT, CategoryNames.CAFE);
    }

    private boolean isCategoryAvailableAtTime(String category, int hour) {
        return switch (category) {
            case CategoryNames.SPOT -> hour >= 9 && hour <= 18;
            case CategoryNames.FOOD -> hour >= 7 && hour <= 21;
            case CategoryNames.CAFE -> hour >= 8 && hour <= 22;
            case CategoryNames.SHOPPING -> hour >= 10 && hour <= 20;
            case CategoryNames.EVENT -> hour >= 10 && hour <= 19;
            case CategoryNames.STAY -> false;
            default -> true;
        };
    }

    private int getSlotCenterHour(int slotIndex, int totalSlots,
            boolean isFirstDay, boolean isLastDay) {
        int startHour = isFirstDay ? 15 : (isLastDay ? 11 : 9);
        int endHour = isFirstDay ? 20 : (isLastDay ? 19 : 20);
        int totalMinutes = (endHour - startHour) * 60;

        int slotDuration = totalMinutes / totalSlots;
        int slotCenterMinutes = (slotIndex * slotDuration) + (slotDuration / 2);

        return startHour + slotCenterMinutes / 60;
    }

    private TravelPlaces selectPlaceByCategory(
            List<TravelPlaces> places,
            Set<Long> usedPlaceIds,
            List<String> preferredCategories,
            int slotIndex,
            int totalSlots,
            boolean isFirstDay,
            boolean isLastDay) {

        int hour = getSlotCenterHour(slotIndex, totalSlots, isFirstDay, isLastDay);

        // 1순위: 선호 카테고리 + 시간 제약
        for (String category : preferredCategories) {
            if (!isCategoryAvailableAtTime(category, hour)) {
                continue;
            }

            for (TravelPlaces place : places) {
                if (usedPlaceIds.contains(place.getId()))
                    continue;

                String placeCategory = place.getNormalizedCategory();
                if (placeCategory != null && placeCategory.equals(category)) {
                    return place;
                }
            }
        }

        // 2순위: 시간 제약만 만족
        for (TravelPlaces place : places) {
            if (usedPlaceIds.contains(place.getId()))
                continue;

            String placeCategory = place.getNormalizedCategory();

            if (placeCategory != null && placeCategory.equals(CategoryNames.STAY)) {
                continue;
            }

            if (placeCategory != null && !isCategoryAvailableAtTime(placeCategory, hour)) {
                continue;
            }

            return place;
        }

        // 3순위: STAY 제외하고 아무거나
        for (TravelPlaces place : places) {
            if (usedPlaceIds.contains(place.getId()))
                continue;

            String placeCategory = place.getNormalizedCategory();
            if (placeCategory == null || !placeCategory.equals(CategoryNames.STAY)) {
                return place;
            }
        }

        return null;
    }

    private String getCategoryEmoji(String category) {
        if (category == null)
            return "🧩";

        return switch (category) {
            case CategoryNames.SPOT -> "📍";
            case CategoryNames.FOOD -> "🍽️";
            case CategoryNames.CAFE -> "☕";
            case CategoryNames.EVENT -> "🎭";
            case CategoryNames.SHOPPING -> "🛍️";
            case CategoryNames.STAY -> "🏨";
            case CategoryNames.ETC -> "🧩";
            default -> "🧩";
        };
    }
}
