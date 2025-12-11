package com.example.demo.planner.plan.agent.test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

import com.example.demo.common.chat.intent.dto.IntentCommand;
import com.example.demo.planner.plan.agent.test.date.DateParser;
import com.example.demo.planner.plan.agent.test.date.DurationParser;
import com.example.demo.planner.plan.dao.PlanDao;
import com.example.demo.planner.plan.dto.TravelPlaceCandidate;
import com.example.demo.planner.plan.dto.entity.Plan;
import com.example.demo.planner.plan.dto.entity.PlanDay;
import com.example.demo.planner.plan.dto.entity.PlanPlace;
import com.example.demo.planner.plan.dto.entity.TravelPlaces;
import com.example.demo.planner.plan.dto.response.PlanResult;
import com.example.demo.planner.plan.utils.CategoryNames;
import com.example.demo.planner.plan.utils.GeoUtils;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TravelPlanAgent {

    private final PlanDao planDao;
    private final EmbeddingModel embeddingModel;
    private final ChatClient chatClient;

    public TravelPlanAgent(ChatClient.Builder chatClientBuilder, EmbeddingModel embeddingModel, PlanDao planDao) {
        this.planDao = planDao;
        this.embeddingModel = embeddingModel;
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * Day별 일정 생성
     */
    public PlanResult generatePlan(IntentCommand command, Long userId) {

        // 1️⃣ 초기화
        Map<String, Object> args = command.getArguments();
        String locationStr = (String) args.get("location");
        String durationStr = (String) args.get("duration");
        String startDateStr = (String) args.get("startDate");
        String theme = (String) args.get("theme");
        String pace = (String) args.get("pace");
        String companion = (String) args.get("companion");

        int duration = DurationParser.parse(durationStr);
        LocalDate startDate = DateParser.parse(startDateStr);
        LocalDate endDate = startDate.plusDays(duration - 1);

        log.info("여행 일정 생성 시작 - {}일, {}", duration, locationStr);

        // 2️⃣ Vector Search
        List<String> locations = parseLocations(locationStr);
        List<TravelPlaces> allCandidates;

        if (locations.size() > 1) {
            allCandidates = searchMultipleLocations(locations, theme, companion);
        } else {
            String searchQuery = buildSearchQuery(locationStr, theme, companion);
            float[] embedding = embeddingModel.embed(searchQuery);
            List<TravelPlaceCandidate> candidateResults = planDao.searchByVector(embedding, 300);
            allCandidates = candidateResults.stream()
                    .map(TravelPlaceCandidate::getTravelPlaces)
                    .collect(Collectors.toList());
        }

        // 3️⃣ 방문 이력 제외
        List<String> visited = planDao.getUserLastVisitedPlaces(userId);
        if (visited != null && !visited.isEmpty()) {
            int beforeSize = allCandidates.size();
            allCandidates = allCandidates.stream()
                    .filter(p -> !visited.contains(p.getTitle()))
                    .collect(Collectors.toList());
            log.info("방문 이력 제외 - {}개 → {}개", beforeSize, allCandidates.size());
        }

        // 4️⃣ 균형 샘플링 (FOOD 우선 확보)
        List<TravelPlaces> availablePlaces = balancedSampling(allCandidates, duration);

        // placeMap 생성 (검증용)
        Map<Long, TravelPlaces> placeMap = allCandidates.stream()
                .collect(Collectors.toMap(TravelPlaces::getId, p -> p));

        log.info("초기 후보: {}개", availablePlaces.size());

        // 5️⃣ Day별 생성
        List<LlmDaySchedule> allDays = new ArrayList<>();

        for (int dayIndex = 1; dayIndex <= duration; dayIndex++) {

            log.info("Day {} 생성 시작 (남은 후보: {}개)", dayIndex, availablePlaces.size());

            // Day별 LLM 요청
            LlmDaySchedule daySchedule = generateSingleDay(
                    dayIndex,
                    duration,
                    availablePlaces,
                    theme,
                    pace,
                    companion,
                    locationStr);

            // FOOD 검증 및 수정
            daySchedule = ensureFoodCount(daySchedule, availablePlaces, placeMap);

            allDays.add(daySchedule);

            // 사용한 장소 제거
            Set<Long> usedIds = daySchedule.getPlaces().stream()
                    .map(LlmPlaceSchedule::getPlaceId)
                    .collect(Collectors.toSet());

            availablePlaces.removeIf(p -> usedIds.contains(p.getId()));

            log.info("Day {} 완료 ({}개 선택, {}개 남음)",
                    dayIndex, usedIds.size(), availablePlaces.size());
        }

        // 6️⃣ 결과 조합
        LlmPlanResponse llmResponse = new LlmPlanResponse();
        llmResponse.setTitle("여행 일정");
        llmResponse.setDays(allDays);

        // 7️⃣ Entity 생성
        return buildPlanResult(userId, llmResponse, allCandidates, startDate, endDate, duration);
    }

    /**
     * 하루 일정 생성
     */
    private LlmDaySchedule generateSingleDay(
            int dayIndex,
            int totalDays,
            List<TravelPlaces> availablePlaces,
            String theme,
            String pace,
            String companion,
            String location) {

        String systemPrompt = """
                하루 여행 일정 큐레이터입니다.

                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                ⚠️ 절대 규칙
                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

                1. FOOD를 정확히 2개 선택
                2. FOOD는 연속 배치 금지 (사이에 다른 카테고리)
                3. 총 5-6곳 선택

                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                ✅ 올바른 예시
                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

                5곳:
                1. SPOT
                2. SPOT
                3. FOOD ← 점심
                4. CAFE
                5. FOOD ← 저녁

                6곳:
                1. SPOT
                2. SHOPPING
                3. FOOD ← 점심
                4. CAFE
                5. SPOT
                6. FOOD ← 저녁

                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                ❌ 나쁜 예시
                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

                1. SPOT
                2. FOOD
                3. FOOD ← X 연속!

                1. SPOT
                2. SPOT
                3. CAFE ← X FOOD 없음!

                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

                출력: JSON만
                {
                  "dayIndex": 1,
                  "title": "하루 제목",
                  "places": [
                    { "placeId": 123, "order": 1 }
                  ]
                }
                """;

        // Day별 장소 수 결정
        int targetPlaces;
        if (totalDays >= 2 && dayIndex == 1) {
            targetPlaces = 5; // 체크인 날
        } else if (totalDays >= 2 && dayIndex == totalDays) {
            targetPlaces = 5; // 체크아웃 날
        } else {
            targetPlaces = 6; // 중간 날
        }

        String userPrompt = String.format("""
                # Day %d / %d
                - 지역: %s
                - 테마: %s
                - 페이스: %s
                - 동행인: %s

                # 장소 목록 (이 중에서만 선택)
                %s

                위 장소에서 %d곳을 선택하여 하루 일정을 구성하세요.
                FOOD는 정확히 2개, 연속 배치 금지.
                """,
                dayIndex, totalDays,
                location != null ? location : "서울",
                theme != null ? theme : "자유",
                pace != null ? pace : "적당",
                companion != null ? companion : "혼자",
                convertPlacesToJson(availablePlaces.stream().limit(50).collect(Collectors.toList())),
                targetPlaces);

        log.info("Day {} LLM 요청 (목표: {}곳)", dayIndex, targetPlaces);

        String responseJson = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .options(ChatOptions.builder()
                        .temperature(0.7)
                        .model("gpt-4o")
                        .build())
                .call()
                .content();

        // JSON 정제
        responseJson = responseJson.trim();
        if (responseJson.startsWith("```")) {
            responseJson = responseJson
                    .replaceFirst("^```(json)?\\s*", "")
                    .replaceFirst("```\\s*$", "")
                    .trim();
        }

        ObjectMapper mapper = new ObjectMapper();

        try {
            LlmDaySchedule daySchedule = mapper.readValue(responseJson, LlmDaySchedule.class);
            daySchedule.setDayIndex(dayIndex);

            if (daySchedule.getPlaces() == null || daySchedule.getPlaces().isEmpty()) {
                throw new RuntimeException("Day " + dayIndex + " 장소가 비어있음");
            }

            log.info("Day {} LLM 생성 완료: {}곳", dayIndex, daySchedule.getPlaces().size());

            return daySchedule;

        } catch (Exception e) {
            log.error("Day {} 응답 파싱 실패: {}", dayIndex, responseJson, e);
            throw new RuntimeException("Day " + dayIndex + " 일정 생성 실패: " + e.getMessage());
        }
    }

    /**
     * FOOD 개수 검증 및 수정
     */
    private LlmDaySchedule ensureFoodCount(
            LlmDaySchedule daySchedule,
            List<TravelPlaces> availablePlaces,
            Map<Long, TravelPlaces> placeMap) {

        List<LlmPlaceSchedule> places = daySchedule.getPlaces();

        // FOOD 개수 확인
        long foodCount = places.stream()
                .filter(p -> {
                    TravelPlaces place = placeMap.get(p.getPlaceId());
                    return place != null && "FOOD".equals(place.getNormalizedCategory());
                })
                .count();

        // FOOD가 2개면 OK
        if (foodCount == 2) {
            return daySchedule;
        }

        log.warn("Day {} FOOD 개수 이상 ({}개), 2개로 수정", daySchedule.getDayIndex(), foodCount);

        // FOOD 분류
        List<LlmPlaceSchedule> foodPlaces = new ArrayList<>();
        List<LlmPlaceSchedule> nonFoodPlaces = new ArrayList<>();

        for (LlmPlaceSchedule place : places) {
            TravelPlaces tp = placeMap.get(place.getPlaceId());
            if (tp != null && "FOOD".equals(tp.getNormalizedCategory())) {
                foodPlaces.add(place);
            } else {
                nonFoodPlaces.add(place);
            }
        }

        // 사용 가능한 FOOD 찾기
        Set<Long> usedIds = places.stream()
                .map(LlmPlaceSchedule::getPlaceId)
                .collect(Collectors.toSet());

        List<TravelPlaces> availableFoods = availablePlaces.stream()
                .filter(p -> "FOOD".equals(p.getNormalizedCategory()))
                .filter(p -> !usedIds.contains(p.getId()))
                .limit(2)
                .collect(Collectors.toList());

        if (availableFoods.size() >= 2) {
            // 재구성
            List<LlmPlaceSchedule> newPlaces = new ArrayList<>();

            // 앞부분
            int mid = Math.max(1, nonFoodPlaces.size() / 2);
            for (int i = 0; i < mid; i++) {
                newPlaces.add(nonFoodPlaces.get(i));
            }

            // 첫 FOOD
            LlmPlaceSchedule food1 = new LlmPlaceSchedule();
            food1.setPlaceId(availableFoods.get(0).getId());
            newPlaces.add(food1);

            // 뒷부분
            for (int i = mid; i < nonFoodPlaces.size(); i++) {
                newPlaces.add(nonFoodPlaces.get(i));
            }

            // 둘째 FOOD
            LlmPlaceSchedule food2 = new LlmPlaceSchedule();
            food2.setPlaceId(availableFoods.get(1).getId());
            newPlaces.add(food2);

            // Order 재설정
            for (int i = 0; i < newPlaces.size(); i++) {
                newPlaces.get(i).setOrder(i + 1);
            }

            daySchedule.setPlaces(newPlaces);
            log.info("Day {} 재구성 완료: {}곳 (FOOD 2개 보장)",
                    daySchedule.getDayIndex(), newPlaces.size());
        } else {
            log.error("Day {} FOOD 후보 부족 ({}개)", daySchedule.getDayIndex(), availableFoods.size());
        }

        return daySchedule;
    }

    /**
     * 지역 문자열 파싱
     */
    private List<String> parseLocations(String locationStr) {
        if (locationStr == null || locationStr.isEmpty()) {
            return List.of("서울");
        }

        if (locationStr.contains(",")) {
            return Arrays.stream(locationStr.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }

        return List.of(locationStr);
    }

    /**
     * 다중 지역 검색
     */
    private List<TravelPlaces> searchMultipleLocations(
            List<String> locations,
            String theme,
            String companion) {

        List<TravelPlaces> allCandidates = new ArrayList<>();
        int perLocation = 300 / locations.size();

        for (String location : locations) {
            String query = buildSearchQuery(location, theme, companion);
            float[] embedding = embeddingModel.embed(query);

            List<TravelPlaceCandidate> results = planDao.searchByVector(embedding, perLocation);

            List<TravelPlaces> places = results.stream()
                    .map(TravelPlaceCandidate::getTravelPlaces)
                    .collect(Collectors.toList());

            allCandidates.addAll(places);
            log.info("  {} 검색: {}개", location, places.size());
        }

        // 중복 제거
        return new ArrayList<>(
                allCandidates.stream()
                        .collect(Collectors.toMap(
                                TravelPlaces::getId,
                                place -> place,
                                (p1, p2) -> p1))
                        .values());
    }

    /**
     * Vector Search 쿼리 생성
     */
    private String buildSearchQuery(String location, String theme, String companion) {
        StringBuilder query = new StringBuilder();

        if (location != null && !location.isEmpty()) {
            query.append(location).append(" ");
        }

        if (theme != null && !theme.isEmpty()) {
            query.append(theme).append(" ");
        }

        if (companion != null) {
            String companionKeywords = getCompanionKeywords(companion);
            if (!companionKeywords.isEmpty()) {
                query.append(companionKeywords).append(" ");
            }
        }

        String result = query.toString().trim();
        return result.isEmpty() ? "서울" : result;
    }

    /**
     * Companion별 검색 키워드 매핑
     */
    private String getCompanionKeywords(String companion) {
        if (companion == null) {
            return "";
        }

        return switch (companion.toLowerCase()) {
            case "연인", "여자친구", "남자친구" -> "데이트 분위기 로맨틱";
            case "가족", "부모님" -> "편안한 가족 단체";
            case "친구", "친구들" -> "핫플 인기 트렌디";
            case "혼자" -> "조용한 혼밥 힐링";
            default -> "";
        };
    }

    /**
     * LLM 응답을 Entity로 변환
     */
    private PlanResult buildPlanResult(
            Long userId,
            LlmPlanResponse llmResponse,
            List<TravelPlaces> candidates,
            LocalDate startDate,
            LocalDate endDate,
            int duration) {

        Map<Long, TravelPlaces> placeMap = candidates.stream()
                .collect(Collectors.toMap(TravelPlaces::getId, p -> p));

        Plan plan = Plan.builder()
                .userId(userId)
                .title(null)
                .startDate(startDate)
                .endDate(endDate)
                .budget(null)
                .isEnded(false)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        List<PlanDay> planDays = new ArrayList<>();
        List<PlanPlace> planPlaces = new ArrayList<>();

        int totalDays = llmResponse.getDays().size();

        for (LlmDaySchedule daySchedule : llmResponse.getDays()) {
            int dayIndex = daySchedule.getDayIndex();
            LocalDate dayDate = startDate.plusDays(dayIndex - 1);

            PlanDay planDay = PlanDay.builder()
                    .dayIndex(dayIndex)
                    .title(null)
                    .planDate(dayDate)
                    .build();

            planDays.add(planDay);

            // order 기준 정렬
            List<LlmPlaceSchedule> orderedPlaces = new ArrayList<>(daySchedule.getPlaces());
            orderedPlaces.sort(Comparator.comparing(LlmPlaceSchedule::getOrder));

            // Day별 시작 시간 설정
            LocalTime dayStartTime;
            if (duration >= 2 && dayIndex == 1) {
                dayStartTime = LocalTime.of(15, 30);
            } else if (duration >= 2 && dayIndex == totalDays) {
                dayStartTime = LocalTime.of(9, 0);
            } else {
                dayStartTime = LocalTime.of(9, 0);
            }

            // 시간 배치
            List<LlmPlaceSchedule> placesWithTime = assignTimesForDay(orderedPlaces, placeMap, dayStartTime);

            // PlanPlace 생성
            for (LlmPlaceSchedule placeSchedule : placesWithTime) {
                TravelPlaces travelPlace = placeMap.get(placeSchedule.getPlaceId());

                if (travelPlace == null) {
                    log.warn("placeId {}를 찾을 수 없음, 스킵", placeSchedule.getPlaceId());
                    continue;
                }

                OffsetDateTime startAt = OffsetDateTime.of(
                        dayDate,
                        placeSchedule.getStartTime(),
                        OffsetDateTime.now().getOffset());

                OffsetDateTime endAt = OffsetDateTime.of(
                        dayDate,
                        placeSchedule.getEndTime(),
                        OffsetDateTime.now().getOffset());

                PlanPlace planPlace = PlanPlace.builder()
                        .title(travelPlace.getTitle())
                        .placeName(travelPlace.getTitle())
                        .address(travelPlace.getAddress())
                        .lat(travelPlace.getLat())
                        .lng(travelPlace.getLng())
                        .startAt(startAt)
                        .endAt(endAt)
                        .expectedCost(null)
                        .normalizedCategory(travelPlace.getNormalizedCategory())
                        .build();

                planPlaces.add(planPlace);
            }
        }

        log.info("여행 일정 생성 완료 - {}일, 총 {}개 장소",
                planDays.size(), planPlaces.size());

        return PlanResult.builder()
                .plan(plan)
                .planDays(planDays)
                .planPlaces(planPlaces)
                .build();
    }

    /**
     * 카테고리별 균형 잡힌 샘플링
     */
    private List<TravelPlaces> balancedSampling(List<TravelPlaces> allCandidates, int duration) {

        int foodNeeded = duration * 2;

        Map<String, List<TravelPlaces>> byCategory = allCandidates.stream()
                .collect(Collectors.groupingBy(TravelPlaces::getNormalizedCategory));

        List<TravelPlaces> result = new ArrayList<>();

        // FOOD 우선 확보 (필요량의 3배)
        List<TravelPlaces> foods = byCategory.getOrDefault("FOOD", new ArrayList<>());
        Collections.shuffle(foods);
        int foodTarget = Math.min(foodNeeded * 3, foods.size());
        result.addAll(foods.subList(0, foodTarget));

        log.info("FOOD 확보: {}개 (필요: {}개)", foodTarget, foodNeeded);

        // 나머지 카테고리
        int remainingSlots = 100 - result.size();
        List<String> otherCategories = byCategory.keySet().stream()
                .filter(cat -> !"FOOD".equals(cat))
                .collect(Collectors.toList());

        if (!otherCategories.isEmpty()) {
            int perCategory = remainingSlots / otherCategories.size();

            for (String category : otherCategories) {
                List<TravelPlaces> categoryPlaces = byCategory.get(category);
                Collections.shuffle(categoryPlaces);
                int take = Math.min(perCategory, categoryPlaces.size());
                result.addAll(categoryPlaces.subList(0, take));
            }
        }

        Collections.shuffle(result);
        log.info("최종 샘플링: 총 {}개 (FOOD: {}개)",
                result.size(),
                result.stream().filter(p -> "FOOD".equals(p.getNormalizedCategory())).count());

        return result;
    }

    /**
     * 하루 일정의 시간 배치
     */
    private List<LlmPlaceSchedule> assignTimesForDay(
            List<LlmPlaceSchedule> places,
            Map<Long, TravelPlaces> placeMap,
            LocalTime dayStartTime) {

        if (places == null || places.isEmpty()) {
            return places;
        }

        LocalTime currentTime = dayStartTime;
        LocalTime dayEndTime = LocalTime.of(22, 0);

        List<LlmPlaceSchedule> result = new ArrayList<>();

        for (int i = 0; i < places.size(); i++) {

            if (currentTime.isAfter(dayEndTime)) {
                break;
            }

            LlmPlaceSchedule current = places.get(i);
            TravelPlaces currentPlace = placeMap.get(current.getPlaceId());
            if (currentPlace == null) {
                continue;
            }

            int stayMinutes = getStayMinutes(currentPlace.getNormalizedCategory());
            LocalTime endTime = currentTime.plusMinutes(stayMinutes);

            if (endTime.isBefore(currentTime)) {
                break;
            }

            if (endTime.isAfter(dayEndTime)) {
                break;
            }

            current.setStartTime(currentTime);
            current.setEndTime(endTime);
            result.add(current);

            // 다음 장소 이동 시간
            if (i < places.size() - 1) {
                LlmPlaceSchedule next = places.get(i + 1);
                TravelPlaces nextPlace = placeMap.get(next.getPlaceId());

                int travelMinutes = 10;
                if (nextPlace != null) {
                    double distance = GeoUtils.haversine(
                            currentPlace.getLat(), currentPlace.getLng(),
                            nextPlace.getLat(), nextPlace.getLng());
                    travelMinutes = Math.max(10, Math.min(60, (int) Math.ceil(distance * 10)));
                }

                currentTime = endTime.plusMinutes(travelMinutes);
            }
        }

        return result;
    }

    /**
     * 장소 목록을 JSON으로 변환
     */
    private String convertPlacesToJson(List<TravelPlaces> places) {
        StringBuilder sb = new StringBuilder("[\n");

        for (TravelPlaces place : places) {
            sb.append(String.format("""
                      {
                        "id": %d,
                        "title": "%s",
                        "category": "%s",
                        "zone": "%s",
                        "lat": %.6f,
                        "lng": %.6f,
                        "tags": %s
                      },
                    """,
                    place.getId(),
                    place.getTitle().replaceAll("\"", "'"),
                    place.getNormalizedCategory() != null ? place.getNormalizedCategory() : "ETC",
                    place.getZoneId() != null ? place.getZoneId() : "",
                    place.getLat(),
                    place.getLng(),
                    place.getTags() != null ? "[\"" + String.join("\", \"", place.getTags()) + "\"]" : "[]"));
        }

        if (sb.length() > 2) {
            sb.setLength(sb.length() - 2);
        }

        sb.append("\n]");
        return sb.toString();
    }

    /**
     * 카테고리별 체류 시간
     */
    private int getStayMinutes(String category) {
        if (category == null)
            return 60;

        return switch (category) {
            case CategoryNames.FOOD -> 60;
            case CategoryNames.CAFE -> 45;
            case CategoryNames.SPOT -> 90;
            case CategoryNames.SHOPPING -> 120;
            case CategoryNames.EVENT -> 90;
            case CategoryNames.ETC -> 60;
            default -> 60;
        };
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class LlmPlanResponse {
        private String title;
        private List<LlmDaySchedule> days;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class LlmDaySchedule {
        private Integer dayIndex;
        private String title;
        private List<LlmPlaceSchedule> places;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class LlmPlaceSchedule {
        private Long placeId;
        private Integer order;

        private LocalTime startTime;
        private LocalTime endTime;
    }
}