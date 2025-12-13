package com.example.demo.planner.plan.service.action;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Component;

import com.example.demo.common.naver.dto.LocalItem;
import com.example.demo.common.tools.NaverInternetSearchTool;
import com.example.demo.planner.plan.dto.entity.PlanDay;
import com.example.demo.planner.plan.dto.entity.PlanPlace;
import com.example.demo.planner.plan.service.PlanDayService;
import com.example.demo.planner.plan.service.PlanPlaceService;
import com.example.demo.planner.plan.service.PlanQueryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AI 전용 - 장소/날짜 추가 액션
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PlanAddAction {

    private final PlanQueryService queryService;
    private final PlanPlaceService placeService;
    private final PlanDayService dayService;
    private final NaverInternetSearchTool naverSearchTool;

    /**
     * 특정 날짜에 장소 추가
     */
    public String addPlace(Long planId, int dayIndex, String placeName, String startTime) {
        List<LocalItem> searchResults = searchNaverLocal(placeName);
        if (searchResults.isEmpty()) {
            throw new IllegalArgumentException("검색 결과가 없습니다: " + placeName);
        }

        LocalItem place = searchResults.get(0);
        var days = queryService.queryAllDaysOptimized(planId);
        var targetDay = days.stream()
            .filter(d -> d.getDay().getDayIndex() == dayIndex)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(dayIndex + "일차를 찾을 수 없습니다."));

        OffsetDateTime startAtDt;
        if (startTime != null && !startTime.isEmpty()) {
            // ISO 8601 형식(2025-12-19T10:00:00Z) 또는 시간만(10:00) 지원
            LocalTime time;
            if (startTime.contains("T")) {
                // ISO 8601 형식: 날짜+시간
                OffsetDateTime parsedDt = OffsetDateTime.parse(startTime);
                time = parsedDt.toLocalTime();
            } else {
                // 시간만: HH:mm 또는 HH:mm:ss
                time = LocalTime.parse(startTime);
            }
            startAtDt = targetDay.getDay().getPlanDate()
                .atTime(time)
                .atZone(ZoneId.of("Asia/Seoul"))
                .toOffsetDateTime();
        } else {
            var places = targetDay.getPlaces();
            if (places.isEmpty()) {
                startAtDt = targetDay.getDay().getPlanDate()
                    .atTime(9, 0)
                    .atZone(ZoneId.of("Asia/Seoul"))
                    .toOffsetDateTime();
            } else {
                startAtDt = places.get(places.size() - 1).getEndAt().plusMinutes(30);
            }
        }

        PlanPlace newPlace = PlanPlace.builder()
            .dayId(targetDay.getDay().getId())
            .title(cleanHtmlTags(place.getTitle()))
            .startAt(startAtDt)
            .endAt(startAtDt.plusHours(2))
            .placeName(cleanHtmlTags(place.getTitle()))
            .address(place.getRoadAddress())
            .lat(convertNaverY(place.getMapy()))
            .lng(convertNaverX(place.getMapx()))
            .expectedCost(BigDecimal.ZERO)
            .build();

        placeService.createPlace(newPlace);

        return String.format("%s (%s~)", cleanHtmlTags(place.getTitle()), startAtDt.toLocalTime());
    }

    /**
     * 특정 위치에 장소 삽입 (이후 일정 자동 조정)
     */
    public String addPlaceAtPosition(Long planId, int dayIndex, int position, String placeName, Integer duration) {
        List<LocalItem> searchResults = searchNaverLocal(placeName);
        if (searchResults.isEmpty()) {
            throw new IllegalArgumentException("검색 결과가 없습니다: " + placeName);
        }

        LocalItem place = searchResults.get(0);
        var days = queryService.queryAllDaysOptimized(planId);
        var targetDay = days.stream()
            .filter(d -> d.getDay().getDayIndex() == dayIndex)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(dayIndex + "일차를 찾을 수 없습니다."));

        List<PlanPlace> existingPlaces = targetDay.getPlaces();
        int durationMin = duration != null ? duration : 120;

        if (position < 1 || position > existingPlaces.size() + 1) {
            throw new IllegalArgumentException(
                String.format("잘못된 위치입니다. 1~%d 사이의 값을 입력해주세요.", existingPlaces.size() + 1)
            );
        }

        OffsetDateTime insertStartTime;
        if (position == 1) {
            if (existingPlaces.isEmpty()) {
                insertStartTime = targetDay.getDay().getPlanDate()
                    .atTime(9, 0)
                    .atZone(ZoneId.of("Asia/Seoul"))
                    .toOffsetDateTime();
            } else {
                insertStartTime = existingPlaces.get(0).getStartAt();
            }
        } else if (position > existingPlaces.size()) {
            insertStartTime = existingPlaces.get(existingPlaces.size() - 1).getEndAt().plusMinutes(30);
        } else {
            insertStartTime = existingPlaces.get(position - 2).getEndAt().plusMinutes(30);
        }

        OffsetDateTime insertEndTime = insertStartTime.plusMinutes(durationMin);

        PlanPlace newPlace = PlanPlace.builder()
            .dayId(targetDay.getDay().getId())
            .title(cleanHtmlTags(place.getTitle()))
            .startAt(insertStartTime)
            .endAt(insertEndTime)
            .placeName(cleanHtmlTags(place.getTitle()))
            .address(place.getRoadAddress())
            .lat(convertNaverY(place.getMapy()))
            .lng(convertNaverX(place.getMapx()))
            .expectedCost(BigDecimal.ZERO)
            .build();

        placeService.createPlace(newPlace);

        // 이후 일정 조정
        int adjustedCount = 0;
        for (int i = position - 1; i < existingPlaces.size(); i++) {
            PlanPlace placeToAdjust = existingPlaces.get(i);
            OffsetDateTime newStart = placeToAdjust.getStartAt().plusMinutes(durationMin);
            OffsetDateTime newEnd = placeToAdjust.getEndAt().plusMinutes(durationMin);
            placeService.updatePlaceTimeRange(placeToAdjust.getId(), newStart, newEnd);
            adjustedCount++;
        }

        return String.format("%s (소요시간: %d분, %d개 일정 조정됨)",
            cleanHtmlTags(place.getTitle()), durationMin, adjustedCount);
    }

    /**
     * 여행 기간 연장 (날짜 추가)
     */
    public void extendPlan(Long planId, int extraDays) {
        var days = queryService.queryAllDaysOptimized(planId);
        if (days.isEmpty()) {
            throw new IllegalArgumentException("여행 계획을 찾을 수 없습니다.");
        }

        int currentMaxDay = days.stream()
            .mapToInt(d -> d.getDay().getDayIndex())
            .max()
            .orElse(0);

        LocalDate lastDate = days.get(days.size() - 1).getDay().getPlanDate();

        for (int i = 1; i <= extraDays; i++) {
            LocalDate newDate = lastDate.plusDays(i);
            PlanDay newDay = PlanDay.builder()
                .planId(planId)
                .dayIndex(currentMaxDay + i)
                .planDate(newDate)
                .build();
            dayService.createDay(newDay, true);
        }
    }

    // ========== Helper Methods ==========

    public List<LocalItem> searchNaverLocal(String query) {
        String enhancedQuery = isLikelyTouristSpot(query) ? query + " 관광지" : query;

        List<LocalItem> results = naverSearchTool.getLocalInfo(enhancedQuery);
        if ((results == null || results.isEmpty()) && !enhancedQuery.equals(query)) {
            results = naverSearchTool.getLocalInfo(query);
        }

        return results != null ? results : List.of();
    }

    private Double convertNaverX(String mapx) {
        if (mapx == null || mapx.isEmpty()) return null;
        return Double.parseDouble(mapx) / 10000000.0;
    }

    private Double convertNaverY(String mapy) {
        if (mapy == null || mapy.isEmpty()) return null;
        return Double.parseDouble(mapy) / 10000000.0;
    }

    private String cleanHtmlTags(String text) {
        if (text == null) return null;
        return text.replaceAll("<[^>]*>", "");
    }

    private boolean isLikelyTouristSpot(String name) {
        return name.matches(".*(궁|성|산|타워|공원|박물관|미술관|전망대|기념관|사찰|절|대|문|루)$") ||
               name.contains("N서울") || name.contains("롯데월드") || name.contains("에버랜드");
    }
}
