package com.example.demo.planner.plan.service;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.planner.plan.dao.PlanDao;
import com.example.demo.planner.plan.dao.PlanDayDao;
import com.example.demo.planner.plan.dao.PlanPlaceDao;
import com.example.demo.planner.plan.dto.entity.Plan;
import com.example.demo.planner.plan.dto.entity.PlanDay;
import com.example.demo.planner.plan.dto.entity.PlanPlace;
import com.example.demo.planner.plan.dto.response.PlacePosition;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * PlanPlace(장소) 관련 CRUD 및 조작 서비스
 * - Place CRUD (생성, 조회, 수정, 삭제)
 * - 같은 날 장소 순서 교환 (swapPlaceOrdersInner)
 * - 다른 날 장소 교환 (swapPlacesBetweenDays)
 * - 장소 교체 (replacePlaceWithNew)
 * - 장소 시간 업데이트 (updatePlaceTime, updatePlaceTimeRange)
 * - 장소명으로 삭제 (fuzzy matching)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanPlaceService {

    private final PlanPlaceDao planPlaceDao;
    private final PlanDayDao planDayDao;
    private final PlanDao planDao;
    private final PlanSnapshotService planSnapshotService;
    private final PlanQueryService planQueryService;

    // ========== Place CRUD ==========

    /**
     * Place 조회
     */
    public PlanPlace findPlaceById(Long placeId) {
        log.info("PlanPlace 조회: placeId={}", placeId);
        PlanPlace place = planPlaceDao.selectPlanPlaceById(placeId);
        if (place == null) {
            throw new IllegalArgumentException("존재하지 않는 여행 장소입니다: placeId=" + placeId);
        }
        return place;
    }

    /**
     * Place 생성
     */
    @Transactional
    public PlanPlace createPlace(PlanPlace place) {
        log.info("PlanPlace 생성: dayId={}", place.getDayId());
        planPlaceDao.insertPlanPlace(place);
        log.info("PlanPlace 생성 완료: placeId={}", place.getId());
        return place;
    }

    /**
     * Place 수정 (부분 수정 지원)
     * @throws Exception 
     */
    @Transactional
    public void updatePlace(Long placeId, PlanPlace place) throws Exception {
        PlanPlace existing = planPlaceDao.selectPlanPlaceById(placeId);
        if (existing == null) {
            throw new IllegalArgumentException("존재하지 않는 여행 장소입니다: placeId=" + placeId);
        }

        // null이 아닌 필드만 업데이트 (부분 수정)
        // lat, lng는 primitive double이라 0.0이 아니면 업데이트
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

        // 스냅샷 저장
        PlanDay planDay = planDayDao.selectPlanDayById(existing.getDayId());
        Plan plan = planDao.selectPlanById(planDay.getPlanId());
        Long planId = plan.getId();
        List<PlanDay> planDays = planDayDao.selectPlanDaysByPlanId(planId);
        List<PlanPlace> planPlaces = planPlaceDao.selectPlanPlacesByPlanId(planId);
        planSnapshotService.savePlanSnapshot(plan, planDays, planPlaces);
    }

    /**
     * Place 삭제
     * @throws Exception 
     */
    @Transactional
    public void deletePlace(Long placeId) throws Exception {
        PlanPlace existing = planPlaceDao.selectPlanPlaceById(placeId);
        if (existing == null) {
            throw new IllegalArgumentException("존재하지 않는 여행 장소입니다: placeId=" + placeId);
        }
        planPlaceDao.deletePlanPlaceById(placeId);

        log.info("PlanPlace 삭제 완료: placeId={}", placeId);

        // 스냅샷 저장
        PlanDay planDay = planDayDao.selectPlanDayById(existing.getDayId());
        Plan plan = planDao.selectPlanById(planDay.getPlanId());
        Long planId = plan.getId();
        List<PlanDay> planDays = planDayDao.selectPlanDaysByPlanId(planId);
        List<PlanPlace> planPlaces = planPlaceDao.selectPlanPlacesByPlanId(planId);
        planSnapshotService.savePlanSnapshot(plan, planDays, planPlaces);
    }

    // ========== 장소 조작 메서드 ==========

    /**
     * 같은 날 장소 순서 교환 (Place Swap Within Day)
     * - 같은 날의 placeA ↔ placeB 교환
     * - dayId는 변경하지 않고 내용만 교환
     */
    @Transactional
    public void swapPlaceOrdersInner(Long planId, int dayIndex, int idxA, int idxB) {
        log.info("같은 날 장소 순서 교환: day={}, placeA={}, placeB={}", dayIndex, idxA, idxB);

        if (idxA == idxB) {
            return;
        }

        PlanDay day = planDayDao.selectPlanDayByPlanIdAndDayIndex(planId, dayIndex);
        if (day == null) {
            throw new IllegalArgumentException("Day not found: " + dayIndex);
        }

        List<PlanPlace> places = planPlaceDao.selectPlanPlacesByPlanDayId(day.getId());

        if (idxA < 1 || idxA > places.size() || idxB < 1 || idxB > places.size()) {
            throw new IllegalArgumentException("Invalid place indices");
        }

        PlanPlace placeA = places.get(idxA - 1);
        PlanPlace placeB = places.get(idxB - 1);

        // A의 정보를 임시 저장
        PlanPlace tempA = PlanPlace.builder()
                .id(placeA.getId())
                .dayId(placeA.getDayId())
                .title(placeA.getTitle())
                .startAt(placeA.getStartAt())
                .endAt(placeA.getEndAt())
                .placeName(placeA.getPlaceName())
                .address(placeA.getAddress())
                .lat(placeA.getLat())
                .lng(placeA.getLng())
                .expectedCost(placeA.getExpectedCost())
                .build();

        // A 위치에 B 정보 복사 (ID는 A 것 유지)
        PlanPlace newA = PlanPlace.builder()
                .id(placeA.getId())
                .dayId(day.getId())
                .title(placeB.getTitle())
                .startAt(placeB.getStartAt())
                .endAt(placeB.getEndAt())
                .placeName(placeB.getPlaceName())
                .address(placeB.getAddress())
                .lat(placeB.getLat())
                .lng(placeB.getLng())
                .expectedCost(placeB.getExpectedCost())
                .build();
        planPlaceDao.updatePlaceAllFields(newA);

        // B 위치에 원래 A 정보 복사 (ID는 B 것 유지)
        PlanPlace newB = PlanPlace.builder()
                .id(placeB.getId())
                .dayId(day.getId())
                .title(tempA.getTitle())
                .startAt(tempA.getStartAt())
                .endAt(tempA.getEndAt())
                .placeName(tempA.getPlaceName())
                .address(tempA.getAddress())
                .lat(tempA.getLat())
                .lng(tempA.getLng())
                .expectedCost(tempA.getExpectedCost())
                .build();
        planPlaceDao.updatePlaceAllFields(newB);

        log.info("Swapped: {} ↔ {}", placeA.getPlaceName(), placeB.getPlaceName());
    }

    /**
     * 서로 다른 날짜 간 장소 교환 (Place Swap Between Days)
     * - dayA의 placeA ↔ dayB의 placeB
     * - dayId까지 포함하여 완전히 교환
     */
    @Transactional
    public void swapPlacesBetweenDays(
            Long planId, int dayIndexA, int placeIndexA, int dayIndexB, int placeIndexB) {
        log.info("날짜 간 장소 교환: day{}[{}] ↔ day{}[{}]",
                dayIndexA, placeIndexA, dayIndexB, placeIndexB);

        PlanDay dayA = planDayDao.selectPlanDayByPlanIdAndDayIndex(planId, dayIndexA);
        PlanDay dayB = planDayDao.selectPlanDayByPlanIdAndDayIndex(planId, dayIndexB);

        if (dayA == null || dayB == null) {
            throw new IllegalArgumentException("One or both days not found");
        }

        List<PlanPlace> placesA = planPlaceDao.selectPlanPlacesByPlanDayId(dayA.getId());
        List<PlanPlace> placesB = planPlaceDao.selectPlanPlacesByPlanDayId(dayB.getId());

        if (placeIndexA < 1 || placeIndexA > placesA.size() ||
                placeIndexB < 1 || placeIndexB > placesB.size()) {
            throw new IllegalArgumentException("Invalid place indices");
        }

        PlanPlace placeA = placesA.get(placeIndexA - 1);
        PlanPlace placeB = placesB.get(placeIndexB - 1);

        // A의 정보를 임시 저장
        PlanPlace tempA = PlanPlace.builder()
                .id(placeA.getId())
                .dayId(placeA.getDayId())
                .title(placeA.getTitle())
                .startAt(placeA.getStartAt())
                .endAt(placeA.getEndAt())
                .placeName(placeA.getPlaceName())
                .address(placeA.getAddress())
                .lat(placeA.getLat())
                .lng(placeA.getLng())
                .expectedCost(placeA.getExpectedCost())
                .build();

        // A 위치에 B 정보 + dayA로 복사
        PlanPlace newA = PlanPlace.builder()
                .id(placeA.getId())
                .dayId(dayA.getId())  // A의 dayId 유지
                .title(placeB.getTitle())
                .startAt(placeB.getStartAt())
                .endAt(placeB.getEndAt())
                .placeName(placeB.getPlaceName())
                .address(placeB.getAddress())
                .lat(placeB.getLat())
                .lng(placeB.getLng())
                .expectedCost(placeB.getExpectedCost())
                .build();
        planPlaceDao.updatePlaceAllFields(newA);

        // B 위치에 원래 A 정보 + dayB로 복사
        PlanPlace newB = PlanPlace.builder()
                .id(placeB.getId())
                .dayId(dayB.getId())  // B의 dayId 유지
                .title(tempA.getTitle())
                .startAt(tempA.getStartAt())
                .endAt(tempA.getEndAt())
                .placeName(tempA.getPlaceName())
                .address(tempA.getAddress())
                .lat(tempA.getLat())
                .lng(tempA.getLng())
                .expectedCost(tempA.getExpectedCost())
                .build();
        planPlaceDao.updatePlaceAllFields(newB);

        log.info("Swapped: day{} {} ↔ day{} {}",
                dayIndexA, placeA.getPlaceName(), dayIndexB, placeB.getPlaceName());
    }

    /**
     * 특정 장소를 다른 장소로 교체 (Place Replace)
     * - 기존 place의 정보를 새로운 place 데이터로 업데이트
     * - 시간/duration은 유지하거나 선택적 변경
     */
    @Transactional
    public void replacePlaceWithNew(Long placeId, String newPlaceName, String newAddress,
                                    Double newLat, Double newLng, String newCategory, BigDecimal newCost) {
        log.info("🔄 장소 교체 시작: placeId={}, newPlace={}", placeId, newPlaceName);
        log.info("   ├─ newAddress: {}", newAddress);
        log.info("   ├─ newLat: {}, newLng: {}", newLat, newLng);
        log.info("   ├─ newCategory: {}, newCost: {}", newCategory, newCost);

        PlanPlace existingPlace = planPlaceDao.selectPlanPlaceById(placeId);
        if (existingPlace == null) {
            throw new IllegalArgumentException("Place not found: " + placeId);
        }

        log.info("   ├─ 기존 장소: {}", existingPlace.getPlaceName());
        log.info("   ├─ 기존 주소: {}", existingPlace.getAddress());
        log.info("   ├─ 기존 좌표: lat={}, lng={}", existingPlace.getLat(), existingPlace.getLng());

        // 기존 시간/duration은 유지하고 장소 정보만 업데이트 (title = placeName)
        planPlaceDao.updatePlaceInfo(placeId, newPlaceName, newAddress, newLat, newLng, newPlaceName, newCost);

        log.info("   └─ ✅ DB UPDATE 완료: {} → {}", existingPlace.getPlaceName(), newPlaceName);

        // 검증: 업데이트 후 다시 조회
        PlanPlace updatedPlace = planPlaceDao.selectPlanPlaceById(placeId);
        log.info("   [검증] 업데이트 후 조회:");
        log.info("      ├─ placeName: {}", updatedPlace.getPlaceName());
        log.info("      ├─ address: {}", updatedPlace.getAddress());
        log.info("      └─ lat={}, lng={}", updatedPlace.getLat(), updatedPlace.getLng());
    }

    /**
     * 특정 장소의 시간/시간대 변경
     */
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

    /**
     * 장소의 시작/종료 시간을 OffsetDateTime으로 직접 변경
     * (일정 삽입 시 뒤 일정들을 밀어낼 때 사용)
     */
    @Transactional
    public void updatePlaceTimeRange(Long placeId, OffsetDateTime newStartAt, OffsetDateTime newEndAt) {
        log.info("장소 시간 범위 변경: placeId={}, newStartAt={}, newEndAt={}", placeId, newStartAt, newEndAt);

        PlanPlace place = planPlaceDao.selectPlanPlaceById(placeId);
        if (place == null) {
            throw new IllegalArgumentException("Place not found: " + placeId);
        }

        planPlaceDao.updatePlaceTimeRange(placeId, newStartAt, newEndAt);
        log.info("   ✅ 시간 조정 완료: {} ({}~{})", place.getPlaceName(),
                newStartAt.toLocalTime(), newEndAt.toLocalTime());
    }

    // ========== 장소 삭제 메서드 ==========

    /**
     * 특정 장소 삭제 (Place Delete by position)
     * - 해당 place 삭제
     */
    @Transactional
    public void deletePlace(Long planId, int dayIndex, int placeIndex) {
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

        // 삭제
        planPlaceDao.deletePlanPlaceById(targetPlace.getId());
        log.info("Deleted: {}", targetPlace.getPlaceName());
    }

    /**
     * 장소명으로 삭제 (Fuzzy matching 사용)
     */
    @Transactional
    public void deletePlaceByName(Long planId, String placeName) {
        log.info("장소명으로 삭제: planId={}, placeName={}", planId, placeName);

        PlacePosition position = planQueryService.findPlacePositionByPlanId(placeName, planId);
        if (position == null) {
            throw new IllegalArgumentException("Place not found: " + placeName);
        }

        deletePlace(planId, position.getDayIndex(), position.getOrder());
    }
}
