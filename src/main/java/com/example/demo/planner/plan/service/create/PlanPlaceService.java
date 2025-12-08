package com.example.demo.planner.plan.service.create;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.planner.plan.dao.PlanDayDao;
import com.example.demo.planner.plan.dao.PlanPlaceDao;
import com.example.demo.planner.plan.dto.entity.PlanDay;
import com.example.demo.planner.plan.dto.entity.PlanPlace;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// PlanPlace 생성/수정/삭제 전용 서비스
@Service
@Slf4j
@RequiredArgsConstructor
public class PlanPlaceService {

    private final PlanPlaceDao planPlaceDao;
    private final PlanDayDao planDayDao;

    // ========== 조회 (READ) ==========

    // PlanPlace 단건 조회 by ID
    public PlanPlace findPlaceById(Long placeId) {
        log.info("PlanPlace 조회: placeId={}", placeId);
        PlanPlace place = planPlaceDao.selectPlanPlaceById(placeId);
        if (place == null) {
            throw new IllegalArgumentException("존재하지 않는 여행 장소입니다: placeId=" + placeId);
        }
        return place;
    }

    // Day의 모든 Place 조회 (간편 메서드)
    public List<PlanPlace> getPlacesByDayId(Long dayId) {
        log.info("Day의 모든 Place 조회: dayId={}", dayId);
        return planPlaceDao.selectPlanPlacesByPlanDayId(dayId);
    }

    // ========== 생성 (CREATE) ==========

    // PlanPlace 생성 (기본)
    public PlanPlace createPlace(PlanPlace place) {
        log.info("PlanPlace 생성: dayId={}", place.getDayId());
        planPlaceDao.insertPlanPlace(place);
        log.info("PlanPlace 생성 완료: placeId={}", place.getId());
        return place;
    }

    // Place 추가 (간편 메서드 - dayId + place 정보)
    @Transactional
    public PlanPlace addPlace(Long dayId, PlanPlace place) {
        log.info("장소 추가: dayId={}, placeName={}", dayId, place.getPlaceName());

        PlanDay day = planDayDao.selectPlanDayById(dayId);
        if (day == null) {
            throw new IllegalArgumentException("일자를 찾을 수 없습니다: " + dayId);
        }

        PlanPlace newPlace = PlanPlace.builder()
                .dayId(dayId)
                .title(place.getTitle())
                .placeName(place.getPlaceName())
                .address(place.getAddress())
                .lat(place.getLat())
                .lng(place.getLng())
                .startAt(place.getStartAt())
                .endAt(place.getEndAt())
                .expectedCost(place.getExpectedCost())
                .build();

        planPlaceDao.insertPlanPlace(newPlace);

        log.info("장소 추가 완료: placeId={}, placeName={}", newPlace.getId(), newPlace.getPlaceName());
        return newPlace;
    }

    // 시간대별 자동 배치로 장소 추가 (첫 장소: 오전 9시, 이후: 이전 종료 시간부터, 기본 2시간)
    @Transactional
    public PlanPlace addPlaceWithAutoTime(Long dayId, PlanPlace place) {
        log.info("자동 시간 배치로 장소 추가: dayId={}, placeName={}", dayId, place.getPlaceName());

        PlanDay day = planDayDao.selectPlanDayById(dayId);
        if (day == null) {
            throw new IllegalArgumentException("일자를 찾을 수 없습니다: " + dayId);
        }

        List<PlanPlace> existingPlaces = planPlaceDao.selectPlanPlacesByPlanDayId(dayId);

        OffsetDateTime startAt;
        if (existingPlaces.isEmpty()) {
            startAt = OffsetDateTime.of(day.getPlanDate(), LocalTime.of(9, 0), ZoneOffset.ofHours(9));
        } else {
            PlanPlace lastPlace = existingPlaces.get(existingPlaces.size() - 1);
            startAt = lastPlace.getEndAt();
        }

        OffsetDateTime endAt = startAt.plusHours(2);

        PlanPlace newPlace = PlanPlace.builder()
                .dayId(dayId)
                .title(place.getTitle())
                .placeName(place.getPlaceName())
                .address(place.getAddress())
                .lat(place.getLat())
                .lng(place.getLng())
                .startAt(startAt)
                .endAt(endAt)
                .expectedCost(place.getExpectedCost())
                .build();

        planPlaceDao.insertPlanPlace(newPlace);

        log.info("자동 시간 배치 완료: {} ~ {}", startAt, endAt);
        return newPlace;
    }

    // ========== 수정 (UPDATE) ==========

    // PlanPlace 부분 수정 (전체 필드)
    public void updatePlace(Long placeId, PlanPlace place) {
        PlanPlace existing = planPlaceDao.selectPlanPlaceById(placeId);
        if (existing == null) {
            throw new IllegalArgumentException("존재하지 않는 여행 장소입니다: placeId=" + placeId);
        }

        PlanPlace updatedPlace = PlanPlace.builder()
                .id(placeId)
                .dayId(place.getDayId() != null ? place.getDayId() : existing.getDayId())
                .title(place.getTitle() != null ? place.getTitle() : existing.getTitle())
                .startAt(place.getStartAt() != null ? place.getStartAt() : existing.getStartAt())
                .endAt(place.getEndAt() != null ? place.getEndAt() : existing.getEndAt())
                .placeName(place.getPlaceName() != null ? place.getPlaceName() : existing.getPlaceName())
                .address(place.getAddress() != null ? place.getAddress() : existing.getAddress())
                .lat(place.getLat() != 0.0 ? place.getLat() : existing.getLat())
                .lng(place.getLng() != 0.0 ? place.getLng() : existing.getLng())
                .expectedCost(place.getExpectedCost() != null ? place.getExpectedCost() : existing.getExpectedCost())
                .build();

        planPlaceDao.updatePlanPlace(updatedPlace);
        log.info("PlanPlace 수정 완료: placeId={}", placeId);
    }

    // 특정 장소의 시간/시간대 변경
    @Transactional
    public void updatePlaceTime(Long placeId, LocalTime newTime, Integer newDuration) {
        log.info("장소 시간 변경: placeId={}, newTime={}, newDuration={}", placeId, newTime, newDuration);

        PlanPlace place = planPlaceDao.selectPlanPlaceById(placeId);
        if (place == null) {
            throw new IllegalArgumentException("Place not found: " + placeId);
        }

        if (newTime != null) {
            planPlaceDao.updatePlaceTime(placeId, newTime);
        }
        if (newDuration != null) {
            planPlaceDao.updatePlaceDuration(placeId, newDuration);
        }

        log.info("Updated time: {} at {}, duration={} min", place.getPlaceName(), newTime, newDuration);
    }

    // 장소를 다른 Day로 이동
    @Transactional
    public void movePlaceToAnotherDay(Long placeId, Long targetDayId) {
        log.info("장소를 다른 Day로 이동: placeId={}, targetDayId={}", placeId, targetDayId);

        PlanPlace place = planPlaceDao.selectPlanPlaceById(placeId);
        if (place == null) {
            throw new IllegalArgumentException("장소를 찾을 수 없습니다: " + placeId);
        }

        PlanDay targetDay = planDayDao.selectPlanDayById(targetDayId);
        if (targetDay == null) {
            throw new IllegalArgumentException("목표 일자를 찾을 수 없습니다: " + targetDayId);
        }

        planPlaceDao.updatePlanDayId(placeId, targetDayId);
        log.info("장소 이동 완료: {} → {}일차", place.getPlaceName(), targetDay.getDayIndex());
    }

    // 두 장소 순서 교체 (같은 Day 내에서, ID 기반)
    @Transactional
    public void swapPlaces(Long dayId, Long placeAId, Long placeBId) {
        log.info("장소 순서 교체: dayId={}, placeA={}, placeB={}", dayId, placeAId, placeBId);

        PlanPlace placeA = planPlaceDao.selectPlanPlaceById(placeAId);
        PlanPlace placeB = planPlaceDao.selectPlanPlaceById(placeBId);

        if (placeA == null || placeB == null) {
            throw new IllegalArgumentException("하나 이상의 장소를 찾을 수 없습니다");
        }

        if (!placeA.getDayId().equals(dayId) || !placeB.getDayId().equals(dayId)) {
            throw new IllegalArgumentException("다른 일자의 장소입니다");
        }

        OffsetDateTime tempStartAt = placeA.getStartAt();
        OffsetDateTime tempEndAt = placeA.getEndAt();

        PlanPlace updatedA = PlanPlace.builder()
                .id(placeA.getId())
                .dayId(placeA.getDayId())
                .title(placeA.getTitle())
                .placeName(placeA.getPlaceName())
                .address(placeA.getAddress())
                .lat(placeA.getLat())
                .lng(placeA.getLng())
                .startAt(placeB.getStartAt())
                .endAt(placeB.getEndAt())
                .expectedCost(placeA.getExpectedCost())
                .build();

        PlanPlace updatedB = PlanPlace.builder()
                .id(placeB.getId())
                .dayId(placeB.getDayId())
                .title(placeB.getTitle())
                .placeName(placeB.getPlaceName())
                .address(placeB.getAddress())
                .lat(placeB.getLat())
                .lng(placeB.getLng())
                .startAt(tempStartAt)
                .endAt(tempEndAt)
                .expectedCost(placeB.getExpectedCost())
                .build();

        planPlaceDao.updatePlanPlace(updatedA);
        planPlaceDao.updatePlanPlace(updatedB);

        log.info("장소 순서 교체 완료: {} ↔ {}", placeA.getPlaceName(), placeB.getPlaceName());
    }

    // 같은 날 내부의 두 장소 순서 교환 (placeIndex 기반, startAt/endAt 교환)
    @Transactional
    public void swapPlaceOrdersInner(Long planId, int dayIndex, int placeIndexA, int placeIndexB) {
        log.info("같은 날 장소 순서 교환: day={}, placeA={}, placeB={}", dayIndex, placeIndexA, placeIndexB);

        if (placeIndexA == placeIndexB) {
            return;
        }

        PlanDay day = planDayDao.selectPlanDayByPlanIdAndDayIndex(planId, dayIndex);
        if (day == null) {
            throw new IllegalArgumentException("Day not found: " + dayIndex);
        }

        List<PlanPlace> places = planPlaceDao.selectPlanPlacesByPlanDayId(day.getId());

        if (placeIndexA < 1 || placeIndexA > places.size() || placeIndexB < 1 || placeIndexB > places.size()) {
            throw new IllegalArgumentException("Invalid place indices");
        }

        PlanPlace placeA = places.get(placeIndexA - 1);
        PlanPlace placeB = places.get(placeIndexB - 1);

        OffsetDateTime tempStartAt = placeA.getStartAt();
        OffsetDateTime tempEndAt = placeA.getEndAt();

        PlanPlace updatedA = PlanPlace.builder()
                .id(placeA.getId())
                .dayId(placeA.getDayId())
                .title(placeA.getTitle())
                .placeName(placeA.getPlaceName())
                .address(placeA.getAddress())
                .lat(placeA.getLat())
                .lng(placeA.getLng())
                .startAt(placeB.getStartAt())
                .endAt(placeB.getEndAt())
                .expectedCost(placeA.getExpectedCost())
                .build();

        PlanPlace updatedB = PlanPlace.builder()
                .id(placeB.getId())
                .dayId(placeB.getDayId())
                .title(placeB.getTitle())
                .placeName(placeB.getPlaceName())
                .address(placeB.getAddress())
                .lat(placeB.getLat())
                .lng(placeB.getLng())
                .startAt(tempStartAt)
                .endAt(tempEndAt)
                .expectedCost(placeB.getExpectedCost())
                .build();

        planPlaceDao.updatePlanPlace(updatedA);
        planPlaceDao.updatePlanPlace(updatedB);

        log.info("같은 날 장소 {} 와 {} 순서 교환 완료 (시간 교환)", placeIndexA, placeIndexB);
    }

    // 서로 다른 날짜 간 장소 교환 (placeIndex 기반, plan_day_id 교환)
    @Transactional
    public void swapPlacesBetweenDays(Long planId, int dayIndexA, int placeIndexA, int dayIndexB, int placeIndexB) {
        log.info("날짜 간 장소 교환: day{}[{}] ↔ day{}[{}]", dayIndexA, placeIndexA, dayIndexB, placeIndexB);

        PlanDay dayA = planDayDao.selectPlanDayByPlanIdAndDayIndex(planId, dayIndexA);
        PlanDay dayB = planDayDao.selectPlanDayByPlanIdAndDayIndex(planId, dayIndexB);

        if (dayA == null || dayB == null) {
            throw new IllegalArgumentException("One or both days not found");
        }

        List<PlanPlace> placesA = planPlaceDao.selectPlanPlacesByPlanDayId(dayA.getId());
        List<PlanPlace> placesB = planPlaceDao.selectPlanPlacesByPlanDayId(dayB.getId());

        if (placeIndexA < 1 || placeIndexA > placesA.size() || placeIndexB < 1 || placeIndexB > placesB.size()) {
            throw new IllegalArgumentException("Invalid place indices");
        }

        PlanPlace placeA = placesA.get(placeIndexA - 1);
        PlanPlace placeB = placesB.get(placeIndexB - 1);

        planPlaceDao.updatePlanDayId(placeA.getId(), dayB.getId());
        planPlaceDao.updatePlanDayId(placeB.getId(), dayA.getId());

        log.info("날짜 간 장소 교환 완료: day{}[{}] ↔ day{}[{}]", dayIndexA, placeIndexA, dayIndexB, placeIndexB);
    }

    // 특정 장소를 다른 장소로 교체 (장소 정보 업데이트, 시간/duration은 유지)
    @Transactional
    public void replacePlaceWithNew(Long placeId, String newPlaceName, String newAddress,
            Double newLat, Double newLng, String newCategory, BigDecimal newCost) {
        log.info("장소 교체: placeId={}, newPlace={}", placeId, newPlaceName);

        PlanPlace existingPlace = planPlaceDao.selectPlanPlaceById(placeId);
        if (existingPlace == null) {
            throw new IllegalArgumentException("Place not found: " + placeId);
        }

        planPlaceDao.updatePlaceInfo(placeId, newPlaceName, newAddress, newLat, newLng, newCategory, newCost);
        log.info("Replaced: {} → {}", existingPlace.getPlaceName(), newPlaceName);
    }

    // ========== 삭제 (DELETE) ==========

    // PlanPlace 삭제 (ID 기반)
    public void deletePlaceById(Long placeId) {
        PlanPlace existing = planPlaceDao.selectPlanPlaceById(placeId);
        if (existing == null) {
            throw new IllegalArgumentException("존재하지 않는 여행 장소입니다: placeId=" + placeId);
        }
        planPlaceDao.deletePlanPlace(placeId);
        log.info("PlanPlace 삭제 완료: placeId={}", placeId);
    }

    // 특정 장소 삭제 (planId + dayIndex + placeIndex 기반)
    @Transactional
    public void deletePlaceByIndex(Long planId, int dayIndex, int placeIndex) {
        log.info("장소 삭제: planId={}, day={}, place={}", planId, dayIndex, placeIndex);

        PlanDay day = planDayDao.selectPlanDayByPlanIdAndDayIndex(planId, dayIndex);
        if (day == null) {
            throw new IllegalArgumentException("Day not found: " + dayIndex);
        }

        List<PlanPlace> places = planPlaceDao.selectPlanPlacesByPlanDayId(day.getId());
        if (placeIndex < 1 || placeIndex > places.size()) {
            throw new IllegalArgumentException("Invalid place index: " + placeIndex);
        }

        PlanPlace targetPlace = places.get(placeIndex - 1);
        planPlaceDao.deletePlanPlaceById(targetPlace.getId());
        log.info("장소 삭제 완료: {}", targetPlace.getPlaceName());
    }

    // Day의 모든 장소 삭제
    @Transactional
    public void deletePlacesByDay(Long dayId) {
        log.info("Day의 모든 장소 삭제: dayId={}", dayId);

        PlanDay day = planDayDao.selectPlanDayById(dayId);
        if (day == null) {
            throw new IllegalArgumentException("일자를 찾을 수 없습니다: " + dayId);
        }

        int deletedCount = planPlaceDao.deletePlacesByDayId(dayId);
        log.info("{}개 장소 삭제 완료", deletedCount);
    }

}
