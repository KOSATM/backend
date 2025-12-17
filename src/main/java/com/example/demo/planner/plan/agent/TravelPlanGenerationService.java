package com.example.demo.planner.plan.agent;

import com.example.demo.planner.plan.dao.PlanDao;
import com.example.demo.planner.plan.dto.entity.TravelPlaces;
import com.example.demo.planner.plan.dto.entity.GeneratedTravelPlan;
import com.example.demo.planner.plan.utils.CategoryNames;
import com.example.demo.common.chat.intent.dto.SeoulRegion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * TravelPlanAgent의 순수 비즈니스 로직 추출
 * 벡터 검색 → 클러스터링 → 일정 배치
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TravelPlanGenerationService {

    private final EmbeddingModel embeddingModel;
    private final PlanDao planDao;

    // =========================================================
    // ✅ 메인 일정 생성 로직
    // =========================================================
    public GeneratedTravelPlan generatePlan(
            int duration,
            String style,
            String location,
            String paceStr,
            LocalDate startDate) {

        log.info("🔥 TravelPlanGenerationService 시작: duration={}, style={}, location={}, pace={}",
                duration, style, location, paceStr);

        // 1) 검색 쿼리 생성
        List<String> queries = generateSearchQueries(style);

        // 2) 벡터 검색
        List<TravelPlaces> places = multiQueryVectorSearch(queries, location, duration);
        log.info("✅ 벡터 검색 결과: {} 개소", places.size());

        // 3) 클러스터링
        Map<String, List<TravelPlaces>> clusters = groupByZone(places);
        log.info("✅ 클러스터링: {} 개 zone", clusters.size());

        // 4) 일정 구조화
        LocalDate endDate = startDate.plusDays(duration - 1);
        GeneratedTravelPlan plan = buildPlanStructured(
                duration, clusters, paceStr, startDate, endDate);

        log.info("✅ 일정 생성 완료: {} 일 / {} 장소",
                plan.days().size(),
                plan.days().stream().mapToInt(d -> d.places().size()).sum());

        return plan;
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

        TravelPlanAgent.Pace pace = TravelPlanAgent.Pace.fromString(paceStr);
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

            if (hour < 10) {
                allowed = allowed.stream()
                        .filter(c -> !CategoryNames.CAFE.equals(c))
                        .toList();
            }

            if (hour >= 22) {
                allowed = List.of(CategoryNames.CAFE);
            }

            if (lastFoodEndMinutes != null) {
                int gap = currentMinutes - lastFoodEndMinutes;
                if (gap < 180) {
                    allowed = allowed.stream()
                            .filter(c -> !CategoryNames.FOOD.equals(c))
                            .toList();
                }
            }

            if (isMiddleDay && slot == maxPlaces - 1 && foodCount < minFoodPerDay) {
                allowed = List.of(CategoryNames.FOOD);
            }

            if (CategoryNames.FOOD.equals(previousCategory)) {
                boolean forceFood = isMiddleDay && slot == maxPlaces - 1 && foodCount < minFoodPerDay;

                if (!forceFood) {
                    allowed = allowed.stream()
                            .filter(c -> !CategoryNames.FOOD.equals(c))
                            .toList();
                }
            }

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

            if (CategoryNames.CAFE.equals(category) && cafeCount >= maxCafePerDay) {
                List<String> retryAllowed = allowed.stream()
                        .filter(c -> !CategoryNames.CAFE.equals(c))
                        .toList();

                selected = selectPlaceStrict(zonePlaces, usedContentIds, retryAllowed);
                if (selected == null)
                    break;

                category = normalizeCategory(selected.getNormalizedCategory());
            }

            if (CategoryNames.FOOD.equals(category) && foodCount >= maxFoodPerDay) {
                List<String> retryAllowed = allowed.stream()
                        .filter(c -> !CategoryNames.FOOD.equals(c))
                        .toList();

                selected = selectPlaceStrict(zonePlaces, usedContentIds, retryAllowed);
                if (selected == null)
                    break;

                category = normalizeCategory(selected.getNormalizedCategory());
            }

            int travelBuffer = getTravelBufferMinutes(previousPlace, selected, isLastDay);
            currentMinutes += travelBuffer;

            hour = currentMinutes / 60;

            int duration = getCategoryDuration(category);

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

        if (isLastDay) {
            return 10;
        }

        if (prev.getZoneId() != null && prev.getZoneId().equals(next.getZoneId())) {
            return 15;
        }

        return 30;
    }

    private int getMaxFoodPerDay(TravelPlanAgent.Pace pace) {
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

            if (previousCategory != null && previousCategory.equals(cat))
                continue;

            return p;
        }
        return null;
    }

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
