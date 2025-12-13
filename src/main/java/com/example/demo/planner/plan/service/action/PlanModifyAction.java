package com.example.demo.planner.plan.service.action;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import org.springframework.stereotype.Component;

import com.example.demo.common.naver.dto.LocalItem;
import com.example.demo.planner.plan.dto.entity.PlanPlace;
import com.example.demo.planner.plan.dto.response.PlacePosition;
import com.example.demo.planner.plan.service.PlanPlaceService;
import com.example.demo.planner.plan.service.PlanQueryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AI 전용 - 장소 수정 서비스
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PlanModifyAction {

    private final PlanQueryService queryService;
    private final PlanPlaceService placeService;
    private final PlanAddAction addAction;  // 네이버 검색 재사용

    /**
     * 장소 교체 (첫 번째 검색 결과로 자동 교체)
     */
    public String replacePlaceWithSearch(Long planId, String oldPlaceName, String newPlaceName) {
        PlacePosition position = queryService.findPlacePositionByPlanId(oldPlaceName, planId);
        if (position == null) {
            throw new IllegalArgumentException("장소를 찾을 수 없습니다: " + oldPlaceName);
        }

        List<PlanPlace> places = queryService.queryPlacesByDayId(position.getDayId());
        PlanPlace targetPlace = places.get(position.getOrder() - 1);

        List<LocalItem> searchResults = addAction.searchNaverLocal(newPlaceName);
        if (searchResults.isEmpty()) {
            throw new IllegalArgumentException("검색 결과가 없습니다: " + newPlaceName);
        }

        LocalItem newPlace = searchResults.get(0);
        placeService.replacePlaceWithNew(
            targetPlace.getId(),
            cleanHtmlTags(newPlace.getTitle()),
            newPlace.getRoadAddress(),
            convertNaverY(newPlace.getMapy()),
            convertNaverX(newPlace.getMapx()),
            newPlace.getCategory(),
            BigDecimal.ZERO
        );

        return cleanHtmlTags(newPlace.getTitle());
    }

    /**
     * 장소 교체 (사용자가 선택한 검색 결과로 교체)
     */
    public String replacePlaceWithSelection(Long planId, String oldPlaceName, String newPlaceName, int selectedIndex) {
        PlacePosition position = queryService.findPlacePositionByPlanId(oldPlaceName, planId);
        if (position == null) {
            throw new IllegalArgumentException("장소를 찾을 수 없습니다: " + oldPlaceName);
        }

        List<PlanPlace> places = queryService.queryPlacesByDayId(position.getDayId());
        PlanPlace targetPlace = places.get(position.getOrder() - 1);

        List<LocalItem> searchResults = addAction.searchNaverLocal(newPlaceName);
        if (searchResults.isEmpty()) {
            throw new IllegalArgumentException("검색 결과가 없습니다: " + newPlaceName);
        }

        if (selectedIndex < 1 || selectedIndex > searchResults.size()) {
            throw new IllegalArgumentException(
                String.format("잘못된 선택입니다. 1~%d 사이의 번호를 입력해주세요.", searchResults.size())
            );
        }

        LocalItem newPlace = searchResults.get(selectedIndex - 1);
        placeService.replacePlaceWithNew(
            targetPlace.getId(),
            cleanHtmlTags(newPlace.getTitle()),
            newPlace.getRoadAddress(),
            convertNaverY(newPlace.getMapy()),
            convertNaverX(newPlace.getMapx()),
            newPlace.getCategory(),
            BigDecimal.ZERO
        );

        return cleanHtmlTags(newPlace.getTitle());
    }

    /**
     * 장소 시간 변경
     */
    public void updatePlaceTime(Long planId, String placeName, String newTime) {
        PlacePosition position = queryService.findPlacePositionByPlanId(placeName, planId);
        if (position == null) {
            throw new IllegalArgumentException("장소를 찾을 수 없습니다: " + placeName);
        }

        List<PlanPlace> places = queryService.queryPlacesByDayId(position.getDayId());
        PlanPlace targetPlace = places.get(position.getOrder() - 1);

        LocalTime time = LocalTime.parse(newTime);
        placeService.updatePlaceTime(targetPlace.getId(), time, null);
    }

    // ========== Helper Methods ==========

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
}
