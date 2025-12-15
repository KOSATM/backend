package com.example.demo.planner.plan.agent;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

    private final EmbeddingModel embeddingModel;
    private final PlanDao planDao;

    public TravelPlanAgent(
            EmbeddingModel embeddingModel,
            PlanDao planDao) {

        this.embeddingModel = embeddingModel;
        this.planDao = planDao;

        log.info("✅ TravelPlanAgent 초기화 완료 (GeneratedTravelPlan 전용)");
    }

    /**
     * 일정 강도 Enum
     */
    private enum Pace {
        RELAXED(3),
        NORMAL(5),
        TIGHT(7);

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
            if (str == null)
                return NORMAL;
            if (str.contains("빡빡"))
                return TIGHT;
            if (str.contains("널널"))
                return RELAXED;
            return NORMAL;
        }
    }

    // =========================================================
    // ✅ 유일한 일정 생성 Tool (STRUCTURED)
    // =========================================================
    @Tool(name = "createSeoulTravelPlanStructured")
    public GeneratedTravelPlan createSeoulTravelPlanStructured(
            @ToolParam(description = "여행 기간(일). 필수!") Integer duration,
            @ToolParam(description = "여행 스타일", required = false) String style,
            @ToolParam(description = "선호 지역", required = false) String location,
            @ToolParam(description = "일정 강도", required = false) String pace,
            @ToolParam(description = "시작일 자연어", required = false) String startDateText) {

        log.info("🔧 일정 생성 요청: duration={}, style={}, location={}, pace={}",
                duration, style, location, pace);

        int safeDuration = Math.min(
                duration != null && duration > 0 ? duration : DurationParser.parse(null),
                7);

        LocalDate startDate = DateParser.parse(startDateText);
        LocalDate endDate = startDate.plusDays(safeDuration - 1);

        try {
            List<String> queries = generateSearchQueries(style);
            List<TravelPlaces> places = multiQueryVectorSearch(queries, location, safeDuration);

            if (places.isEmpty()) {
                return new GeneratedTravelPlan(
                        safeDuration,
                        Pace.fromString(pace).getLabel(),
                        List.of(),
                        startDate,
                        endDate);
            }

            Map<String, List<TravelPlaces>> clusters = groupByZone(places);
            return buildPlanStructured(
                    safeDuration,
                    clusters,
                    pace,
                    startDate,
                    endDate);

        } catch (Exception e) {
            log.error("❌ 일정 생성 실패", e);
            return new GeneratedTravelPlan(
                    safeDuration,
                    Pace.fromString(pace).getLabel(),
                    List.of(),
                    startDate,
                    endDate);
        }
    }

    // =========================================================
    // ✅ 검색 쿼리 생성 (rule 기반)
    // =========================================================
    private List<String> generateSearchQueries(String style) {
        if (style == null || style.isBlank()) {
            return List.of(
                    "서울 인기 관광지",
                    "서울 맛집",
                    "서울 카페",
                    "서울 명소");
        }

        return List.of(
                style + " 명소",
                style + " 추천",
                "서울 맛집",
                "서울 카페",
                "서울 관광지");
    }

    // =========================================================
    // ✅ 벡터 검색
    // =========================================================
    private List<TravelPlaces> multiQueryVectorSearch(
            List<String> queries, String location, int duration) {

        List<TravelPlaces> all = new ArrayList<>();
        List<String> zoneIds = extractZoneIds(location, duration);

        for (String q : queries) {
            float[] embedding = embeddingModel.embed(q);
            List<TravelPlaces> result = planDao.vectorSearch(
                    embedding,
                    zoneIds.isEmpty() ? null : zoneIds,
                    50);

            Collections.shuffle(result);
            all.addAll(result.stream().limit(20).toList());
        }

        return deduplicatePlaces(all);
    }

    // =========================================================
    // ✅ contentId 기준 중복 제거
    // =========================================================
    private List<TravelPlaces> deduplicatePlaces(List<TravelPlaces> places) {
        Map<Long, TravelPlaces> map = new HashMap<>();

        for (TravelPlaces p : places) {
            Long key = p.getContentId() != null ? p.getContentId() : -p.getId();
            map.putIfAbsent(key, p);
        }
        return new ArrayList<>(map.values());
    }

    private Map<String, List<TravelPlaces>> groupByZone(List<TravelPlaces> places) {
        return places.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getZoneId() != null ? p.getZoneId() : "기타"));
    }

    // =========================================================
    // ✅ 구조화 일정 빌더 (여행 전체 기준 중복 방지)
    // =========================================================
    private GeneratedTravelPlan buildPlanStructured(
            int duration,
            Map<String, List<TravelPlaces>> clusters,
            String paceStr,
            LocalDate startDate,
            LocalDate endDate) {

        Pace pace = Pace.fromString(paceStr);
        List<String> zoneKeys = new ArrayList<>(clusters.keySet());

        Set<Long> usedContentIds = new HashSet<>();
        List<GeneratedTravelPlan.GeneratedDay> days = new ArrayList<>();

        for (int day = 1; day <= duration; day++) {
            String zone = zoneKeys.get((day - 1) % zoneKeys.size());
            List<TravelPlaces> zonePlaces = clusters.get(zone);

            boolean isFirstDay = duration > 1 && day == 1;
            boolean isLastDay = duration > 1 && day == duration;

            int maxFoodPerDay = getMaxFoodPerDay(pace);

            List<GeneratedTravelPlan.GeneratedPlace> places = buildDayPlanStructured(
                    day,
                    zonePlaces,
                    usedContentIds,
                    pace.getPlacesPerDay(),
                    maxFoodPerDay,
                    isFirstDay,
                    isLastDay,
                    startDate);

            days.add(new GeneratedTravelPlan.GeneratedDay(
                    day,
                    startDate.plusDays(day - 1),
                    places));
        }

        return new GeneratedTravelPlan(
                duration,
                pace.getLabel(),
                days,
                startDate,
                endDate);
    }

    // =========================================================
    // 하루 일정 생성
    // =========================================================
    private List<GeneratedTravelPlan.GeneratedPlace> buildDayPlanStructured(
            int day,
            List<TravelPlaces> zonePlaces,
            Set<Long> usedContentIds,
            int placesPerDay,
            int maxFoodPerDay,
            boolean isFirstDay,
            boolean isLastDay,
            LocalDate startDate) {

        TravelPlaces previousPlace = null;
        String previousCategory = null;

        // 마지막 식사 종료 시점 (분 단위)
        Integer lastFoodEndMinutes = null;

        int cafeCount = 0;
        int maxCafePerDay = 2;

        int foodCount = 0;

        boolean isMiddleDay = !isFirstDay && !isLastDay;
        int minFoodPerDay = isMiddleDay ? 2 : 1;

        List<GeneratedTravelPlan.GeneratedPlace> result = new ArrayList<>();

        int startHour = isFirstDay ? 15 : (isLastDay ? 11 : 10);
        int maxPlaces = isLastDay ? 2 : placesPerDay;
        int currentMinutes = startHour * 60;

        for (int slot = 0; slot < maxPlaces; slot++) {

            int hour = currentMinutes / 60;

            List<String> allowed = getAllowedCategories(
                    slot, hour, isFirstDay, isLastDay, previousCategory);

            // 아침 카페 방지
            if (hour < 10) {
                allowed = allowed.stream()
                        .filter(c -> !CategoryNames.CAFE.equals(c))
                        .toList();
            }

            // 22시 이후 → 카페만
            if (hour >= 22) {
                allowed = List.of(CategoryNames.CAFE);
            }

            // 식사 간 최소 간격
            if (lastFoodEndMinutes != null) {
                int gap = currentMinutes - lastFoodEndMinutes;

                // 3시간(180분) 미만이면 FOOD 제외
                if (gap < 180) {
                    allowed = allowed.stream()
                            .filter(c -> !CategoryNames.FOOD.equals(c))
                            .toList();
                }
            }

            // 중간날 마지막 슬롯 → FOOD 강제
            if (isMiddleDay && slot == maxPlaces - 1 && foodCount < minFoodPerDay) {
                allowed = List.of(CategoryNames.FOOD);
            }

            // 연속 FOOD 방지
            if (CategoryNames.FOOD.equals(previousCategory)) {
                boolean forceFood = isMiddleDay && slot == maxPlaces - 1 && foodCount < minFoodPerDay;

                if (!forceFood) {
                    allowed = allowed.stream()
                            .filter(c -> !CategoryNames.FOOD.equals(c))
                            .toList();
                }
            }

            // 연속 CAFE 방지
            if (CategoryNames.CAFE.equals(previousCategory) && hour < 22) {
                allowed = allowed.stream()
                        .filter(c -> !CategoryNames.CAFE.equals(c))
                        .toList();
            }

            TravelPlaces selected = selectPlaceStrict(
                    zonePlaces,
                    usedContentIds,
                    allowed);

            if (selected == null) {
                selected = selectAnyPlace(zonePlaces, usedContentIds, previousCategory);
                if (selected == null)
                    break;
            }

            String category = normalizeCategory(selected.getNormalizedCategory());

            // 카페 상한 → 카페 제외 재선택
            if (CategoryNames.CAFE.equals(category) && cafeCount >= maxCafePerDay) {
                List<String> retryAllowed = allowed.stream()
                        .filter(c -> !CategoryNames.CAFE.equals(c))
                        .toList();

                selected = selectPlaceStrict(zonePlaces, usedContentIds, retryAllowed);
                if (selected == null)
                    break;

                category = normalizeCategory(selected.getNormalizedCategory());
            }

            // FOOD 상한 → FOOD 제외 재선택
            if (CategoryNames.FOOD.equals(category) && foodCount >= maxFoodPerDay) {
                List<String> retryAllowed = allowed.stream()
                        .filter(c -> !CategoryNames.FOOD.equals(c))
                        .toList();

                selected = selectPlaceStrict(zonePlaces, usedContentIds, retryAllowed);
                if (selected == null)
                    break;

                category = normalizeCategory(selected.getNormalizedCategory());
            }

            // 이동시간
            int travelBuffer = getTravelBufferMinutes(previousPlace, selected, isLastDay);
            currentMinutes += travelBuffer;

            hour = currentMinutes / 60;

            int duration = getCategoryDuration(category);

            // 야간 카페는 짧게
            if (CategoryNames.CAFE.equals(category) && hour >= 22) {
                duration = 45;
            }

            OffsetDateTime startAt = startDate
                    .plusDays(day - 1)
                    .atTime(currentMinutes / 60, currentMinutes % 60)
                    .atOffset(ZoneOffset.ofHours(9));

            OffsetDateTime endAt = startAt.plusMinutes(duration);

            result.add(new GeneratedTravelPlan.GeneratedPlace(
                    selected.getTitle(),
                    selected.getTitle(),
                    startAt,
                    endAt,
                    selected.getLat(),
                    selected.getLng(),
                    selected.getAddress(),
                    category,
                    selected.getFirstImage(),
                    selected.getFirstImage2()));

            // FOOD 종료 시점 기록 (여기 필수)
            if (CategoryNames.FOOD.equals(category)) {
                lastFoodEndMinutes = currentMinutes + duration;
                foodCount++;
            }

            if (CategoryNames.CAFE.equals(category)) {
                cafeCount++;
            }

            usedContentIds.add(
                    selected.getContentId() != null
                            ? selected.getContentId()
                            : selected.getId());

            previousPlace = selected;
            previousCategory = category;

            currentMinutes += duration;

            // ⏰ 하루 종료 제한
            int endLimitMinutes = isLastDay
                    ? 15 * 60
                    : (isMiddleDay ? 22 * 60 : 21 * 60);

            if (currentMinutes >= endLimitMinutes) {
                break;
            }
        }

        return result;
    }

    private int getTravelBufferMinutes(
            TravelPlaces prev,
            TravelPlaces next,
            boolean isLastDay) {

        if (prev == null || next == null) {
            return 0;
        }

        // 마지막 날은 이동 최소화
        if (isLastDay) {
            return 10;
        }

        // zoneId 기준
        if (prev.getZoneId() != null && prev.getZoneId().equals(next.getZoneId())) {
            return 15;
        }

        // zone 다르면
        return 30;
    }

    private int getMaxFoodPerDay(Pace pace) {
        return switch (pace) {
            case TIGHT -> 3;
            case RELAXED, NORMAL -> 2;
        };
    }

    private TravelPlaces selectAnyPlace(
            List<TravelPlaces> places,
            Set<Long> usedContentIds,
            String previousCategory) {

        for (TravelPlaces p : places) {
            Long key = p.getContentId() != null ? p.getContentId() : p.getId();
            String cat = normalizeCategory(p.getNormalizedCategory());

            if (usedContentIds.contains(key))
                continue;

            // 연속 방지
            if (previousCategory != null && previousCategory.equals(cat))
                continue;

            return p;
        }
        return null;
    }

    // =========================================================
    // ✅ 선택 / 규칙 로직
    // =========================================================
    private TravelPlaces selectPlaceStrict(
            List<TravelPlaces> places,
            Set<Long> usedContentIds,
            List<String> allowedCategories) {

        for (String cat : allowedCategories) {
            for (TravelPlaces p : places) {
                Long key = p.getContentId() != null ? p.getContentId() : p.getId();
                if (usedContentIds.contains(key))
                    continue;
                if (cat.equals(normalizeCategory(p.getNormalizedCategory())))
                    return p;
            }
        }

        for (TravelPlaces p : places) {
            Long key = p.getContentId() != null ? p.getContentId() : p.getId();
            if (!usedContentIds.contains(key))
                return p;
        }

        return null;
    }

    private List<String> getAllowedCategories(
            int slot,
            int hour,
            boolean isFirstDay,
            boolean isLastDay,
            String previousCategory) {

        List<String> base;

        if (hour >= 11 && hour <= 13) {
            base = List.of(CategoryNames.FOOD, CategoryNames.SPOT);
        } else if (hour >= 14 && hour <= 17) {
            base = List.of(CategoryNames.SPOT, CategoryNames.CAFE);
        } else if (hour >= 18) {
            base = List.of(CategoryNames.FOOD, CategoryNames.CAFE);
        } else {
            base = List.of(CategoryNames.SPOT);
        }

        return base;
    }

    private String normalizeCategory(String cat) {
        return cat == null ? CategoryNames.ETC : cat;
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

    // =========================================================
    // ✅ 지역 처리
    // =========================================================
    private List<String> extractZoneIds(String location, int duration) {
        if (location == null || location.isBlank()) {
            return selectRandomZones(duration);
        }

        return Arrays.stream(location.split("[,\\s]+"))
                .map(SeoulRegion::fromUserInput)
                .filter(r -> r != null)
                .map(SeoulRegion::getZoneId)
                .distinct()
                .toList();
    }

    private List<String> selectRandomZones(int duration) {
        int count = duration <= 2 ? 1 : 2;
        List<SeoulRegion> all = new ArrayList<>(List.of(SeoulRegion.values()));
        Collections.shuffle(all);

        return all.stream()
                .limit(count)
                .map(SeoulRegion::getZoneId)
                .toList();
    }
}
