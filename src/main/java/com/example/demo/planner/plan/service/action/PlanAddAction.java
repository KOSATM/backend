package com.example.demo.planner.plan.service.action;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.common.naver.dto.LocalItem;
import com.example.demo.common.tools.NaverInternetSearchTool;
import com.example.demo.planner.plan.dao.PlanDao;
import com.example.demo.planner.plan.dao.PlanPlaceDao;
import com.example.demo.planner.plan.dto.entity.PlanDay;
import com.example.demo.planner.plan.dto.entity.PlanPlace;
import com.example.demo.planner.plan.dto.entity.TravelPlaces;
import com.example.demo.planner.plan.dto.response.PlanDayWithPlaces;
import com.example.demo.planner.plan.service.PlanDayService;
import com.example.demo.planner.plan.service.PlanPlaceService;
import com.example.demo.planner.plan.service.PlanQueryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AI 전용 - 장소/날짜 추가 액션
 * - 일정에 실제로 장소를 삽입하는 순수 비즈니스 로직
 * - 추천 판단 / 사용자 의도 해석은 여기서 하지 않는다
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PlanAddAction {

    private final PlanDao planDao;
    private final PlanPlaceDao planPlaceDao;
    private final PlanQueryService queryService;
    private final PlanPlaceService placeService;
    private final PlanDayService dayService;
    private final NaverInternetSearchTool naverSearchTool;

    /**
     * 특정 날짜에 장소 추가 (이름 기반)
     */
    public String addPlace(Long planId, Integer dayIndex, String placeName, Integer position) {

        // 1. Day 조회 (타입 안전)
        List<PlanDayWithPlaces> days = queryService.queryAllDaysOptimized(planId);

        PlanDayWithPlaces targetDay = days.stream()
                .filter(d -> d.getDay().getDayIndex() == dayIndex)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(dayIndex + "일차를 찾을 수 없습니다."));

        List<PlanPlace> existingPlaces = targetDay.getPlaces();
        Long dayId = targetDay.getDay().getId();

        // 2. position 보정 (맨 뒤 허용)
        // int insertIndex = Math.max(0, Math.min(position - 1, existingPlaces.size()));

        // 3. 장소 조회
        TravelPlaces place = planDao.findExactByTitle(placeName);

        // 3-1. exact 실패 → normalized like
        if (place == null) {
            List<TravelPlaces> candidates = planDao.findByTitleLikeNormalized(placeName);

            if (candidates.size() == 1) {
                place = candidates.get(0); // 1개면 자동 확정
            } else if (candidates.size() > 1) {
                return renderPlaceCandidates(candidates);
            }
        }

        // 3-2. 그래도 없으면 일반 like
        if (place == null) {
            List<TravelPlaces> candidates = planDao.findByTitleLike(placeName);

            if (candidates.size() == 1) {
                place = candidates.get(0);
            } else if (candidates.size() > 1) {
                return renderPlaceCandidates(candidates);
            }
        }

        // 3-3. 최종 실패
        if (place == null) {
            return "❌ '" + placeName + "'에 해당하는 장소를 찾지 못했습니다.";
        }


        PlanPlace newPlace = PlanPlace.builder()
                .dayId(targetDay.getDay().getId())
                .title(place.getTitle())
                .placeName(place.getTitle())
                .address(place.getAddress())
                .lat(place.getLat())
                .lng(place.getLng())
                .startAt(OffsetDateTime.now(ZoneId.of("Asia/Seoul")))
                .endAt(OffsetDateTime.now(ZoneId.of("Asia/Seoul")).plusMinutes(1))
                .expectedCost(BigDecimal.ZERO)
                .normalizedCategory(place.getNormalizedCategory())
                .firstImage(place.getFirstImage())
                .firstImage2(place.getFirstImage2())
                .build();
        existingPlaces.add(position - 1, newPlace);

        // 해당 일자 여행지 삭제
        planPlaceDao.deletePlanPlaceByDayId(dayId);
        planPlaceDao.insertPlanPlaceBatch(existingPlaces);

        return place.getTitle();
    }

    /**
     * 추천 결과(Map)를 해당 day에 삽입
     */
    @Transactional
    public String addPlaceFromRecommendation(Long planId, int dayIndex, int position, Long placeId) {

        List<?> days = queryService.queryAllDaysOptimized(planId);

        Object targetDayObj = days.stream()
                .filter(d -> ((PlanDayWithPlaces) d)
                        .getDay().getDayIndex() == dayIndex)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(dayIndex + "일차를 찾을 수 없습니다."));

        PlanDayWithPlaces targetDay = (PlanDayWithPlaces) targetDayObj;

        // 해당 일자의 여행지
        List<PlanPlace> existingPlaces = targetDay.getPlaces();

        Long dayId = targetDay.getDay().getId();

        // OffsetDateTime insertStartTime = calculateInsertTime(planDate,
        // existingPlaces, position);
        // OffsetDateTime insertEndTime = insertStartTime.plusMinutes(durationMin);

        TravelPlaces place = planDao.findByPlaceId(placeId);

        if (place == null) {
            throw new IllegalArgumentException("추천 장소를 찾을 수 없습니다.");
        }

        PlanPlace newPlace = PlanPlace.builder()
                .dayId(targetDay.getDay().getId())
                .title(place.getTitle())
                .placeName(place.getTitle())
                .address(place.getAddress())
                .lat(place.getLat())
                .lng(place.getLng())
                .startAt(OffsetDateTime.now(ZoneId.of("Asia/Seoul")))
                .endAt(OffsetDateTime.now(ZoneId.of("Asia/Seoul")).plusMinutes(1))
                .expectedCost(BigDecimal.ZERO)
                .normalizedCategory(place.getNormalizedCategory())
                .firstImage(place.getFirstImage())
                .firstImage2(place.getFirstImage2())
                .build();

        existingPlaces.add(position - 1, newPlace);

        // 해당 일자 여행지 삭제
        planPlaceDao.deletePlanPlaceByDayId(dayId);
        planPlaceDao.insertPlanPlaceBatch(existingPlaces);

        return place.getTitle();
    }

    /**
     * 웹 검색 결과를 특정 위치에 장소 삽입 (이후 일정 자동 조정)
     */
    public String addPlaceAtPosition(Long planId, int dayIndex, int position, String placeName, Integer duration) {

        List<LocalItem> searchResults = searchNaverLocal(placeName);
        if (searchResults.isEmpty()) {
            throw new IllegalArgumentException("검색 결과가 없습니다: " + placeName);
        }

        LocalItem place = searchResults.get(0);

        List<?> days = queryService.queryAllDaysOptimized(planId);

        Object targetDayObj = days.stream()
                .filter(d -> ((PlanDayWithPlaces) d)
                        .getDay().getDayIndex() == dayIndex)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(dayIndex + "일차를 찾을 수 없습니다."));

        PlanDayWithPlaces targetDay = (PlanDayWithPlaces) targetDayObj;

        LocalDate planDate = targetDay.getDay().getPlanDate();
        List<PlanPlace> existingPlaces = targetDay.getPlaces();
        int durationMin = duration != null ? duration : 120;

        validatePosition(position, existingPlaces.size());

        OffsetDateTime insertStartTime = calculateInsertTime(planDate, existingPlaces, position);
        OffsetDateTime insertEndTime = insertStartTime.plusMinutes(durationMin);

        PlanPlace newPlace = PlanPlace.builder()
                .dayId(targetDay.getDay().getId())
                .title(cleanHtmlTags(place.getTitle()))
                .placeName(cleanHtmlTags(place.getTitle()))
                .address(place.getRoadAddress())
                .lat(convertNaverY(place.getMapy()))
                .lng(convertNaverX(place.getMapx()))
                .startAt(insertStartTime)
                .endAt(insertEndTime)
                .expectedCost(BigDecimal.ZERO)
                .build();

        placeService.createPlace(newPlace);

        shiftLaterPlaces(existingPlaces, position, durationMin);

        return String.format("%s (소요시간: %d분)", cleanHtmlTags(place.getTitle()), durationMin);
    }

    /**
     * 여행 기간 연장 (날짜 추가)
     */
    public void extendPlan(Long planId, int extraDays) {
        List<?> days = queryService.queryAllDaysOptimized(planId);
        if (days.isEmpty()) {
            throw new IllegalArgumentException("여행 계획을 찾을 수 없습니다.");
        }

        int currentMaxDay = days.stream()
                .mapToInt(d -> ((PlanDayWithPlaces) d)
                        .getDay().getDayIndex())
                .max()
                .orElse(0);

        PlanDayWithPlaces last = (PlanDayWithPlaces) days.get(days.size() - 1);

        LocalDate lastDate = last.getDay().getPlanDate();

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

    /*
     * =========================
     * Helper: 시간/위치 계산
     * =========================
     */

    // resolveStartTime 에러 해결: targetDay 타입을 받지 않고 "필요한 값"만 받도록 변경
    private OffsetDateTime resolveStartTime(LocalDate planDate, List<PlanPlace> existingPlaces, String startTime) {

        if (startTime == null || startTime.isEmpty()) {
            if (existingPlaces == null || existingPlaces.isEmpty()) {
                return planDate.atTime(9, 0)
                        .atZone(ZoneId.of("Asia/Seoul"))
                        .toOffsetDateTime();
            }
            return existingPlaces.get(existingPlaces.size() - 1).getEndAt().plusMinutes(30);
        }

        LocalTime time;
        if (startTime.contains("T")) {
            // ISO 8601 (예: 2025-12-19T10:00:00Z)
            OffsetDateTime parsed = OffsetDateTime.parse(startTime);
            time = parsed.toLocalTime();
        } else {
            // HH:mm or HH:mm:ss
            time = LocalTime.parse(startTime);
        }

        return planDate.atTime(time)
                .atZone(ZoneId.of("Asia/Seoul"))
                .toOffsetDateTime();
    }

    private OffsetDateTime calculateInsertTime(LocalDate planDate, List<PlanPlace> existingPlaces, int position) {

        if (position == 1) {
            if (existingPlaces == null || existingPlaces.isEmpty()) {
                return planDate.atTime(9, 0)
                        .atZone(ZoneId.of("Asia/Seoul"))
                        .toOffsetDateTime();
            }
            return existingPlaces.get(0).getStartAt();
        }

        if (existingPlaces == null || existingPlaces.isEmpty()) {
            // position이 2 이상인데 기존 일정이 비었으면 그냥 9시로 처리
            return planDate.atTime(9, 0)
                    .atZone(ZoneId.of("Asia/Seoul"))
                    .toOffsetDateTime();
        }

        if (position > existingPlaces.size()) {
            return existingPlaces.get(existingPlaces.size() - 1).getEndAt().plusMinutes(30);
        }

        return existingPlaces.get(position - 2).getEndAt().plusMinutes(30);
    }

    private void shiftLaterPlaces(List<PlanPlace> existingPlaces, int position, int durationMin) {
        if (existingPlaces == null || existingPlaces.isEmpty())
            return;

        for (int i = position - 1; i < existingPlaces.size(); i++) {
            PlanPlace p = existingPlaces.get(i);

            OffsetDateTime newStart = p.getStartAt().plusMinutes(durationMin);
            OffsetDateTime newEnd = p.getEndAt().plusMinutes(durationMin);

            placeService.updatePlaceTimeRange(p.getId(), newStart, newEnd);
        }
    }

    private void validatePosition(int position, int existingSize) {
        if (position < 1 || position > existingSize + 1) {
            throw new IllegalArgumentException(
                    String.format("잘못된 위치입니다. 1~%d 사이의 값을 입력해주세요.", existingSize + 1));
        }
    }

    /*
     * =========================
     * Search & Utils
     * =========================
     */

    public List<LocalItem> searchNaverLocal(String query) {
        String enhancedQuery = isLikelyTouristSpot(query) ? query + " 관광지" : query;

        List<LocalItem> results = naverSearchTool.getLocalInfo(enhancedQuery);
        if ((results == null || results.isEmpty()) && !enhancedQuery.equals(query)) {
            results = naverSearchTool.getLocalInfo(query);
        }

        return results != null ? results : List.of();
    }

    private Double convertNaverX(String mapx) {
        if (mapx == null || mapx.isEmpty())
            return null;
        return Double.parseDouble(mapx) / 10000000.0;
    }

    private Double convertNaverY(String mapy) {
        if (mapy == null || mapy.isEmpty())
            return null;
        return Double.parseDouble(mapy) / 10000000.0;
    }

    private String cleanHtmlTags(String text) {
        if (text == null)
            return null;
        return text.replaceAll("<[^>]*>", "");
    }

    private boolean isLikelyTouristSpot(String name) {
        return name.matches(".*(궁|성|산|타워|공원|박물관|미술관|전망대|기념관|사찰|절|대|문|루)$") ||
                name.contains("N서울") || name.contains("롯데월드") || name.contains("에버랜드");
    }

    private String renderPlaceCandidates(List<TravelPlaces> candidates) {
        StringBuilder sb = new StringBuilder();
        sb.append("여러 장소가 검색되었습니다. 어떤 장소를 추가할까요?\n\n");

        for (int i = 0; i < Math.min(5, candidates.size()); i++) {
            TravelPlaces p = candidates.get(i);
            sb.append(String.format(
                    "%d. %s (%s)\n",
                    i + 1,
                    p.getTitle(),
                    p.getAddress()));
        }

        sb.append("\n번호로 선택해주세요. 예: \"2번 추가해줘\"");
        return sb.toString();
    }

}
