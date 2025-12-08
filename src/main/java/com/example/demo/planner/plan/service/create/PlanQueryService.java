package com.example.demo.planner.plan.service.create;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.common.user.dao.UserDao;
import com.example.demo.common.user.dto.User;
import com.example.demo.planner.plan.dao.PlanDao;
import com.example.demo.planner.plan.dao.PlanDayDao;
import com.example.demo.planner.plan.dao.PlanPlaceDao;
import com.example.demo.planner.plan.dto.entity.Plan;
import com.example.demo.planner.plan.dto.entity.PlanDay;
import com.example.demo.planner.plan.dto.entity.PlanPlace;
import com.example.demo.planner.plan.dto.response.PlacePosition;
import com.example.demo.planner.plan.dto.response.PlanDayWithPlaces;
import com.example.demo.planner.plan.dto.response.PlanDetail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// Plan 조회 전용 서비스 (Agent용 복합 조회 메서드 포함)
@Service
@Slf4j
@RequiredArgsConstructor
public class PlanQueryService {

    private final PlanDao planDao;
    private final PlanDayDao planDayDao;
    private final PlanPlaceDao planPlaceDao;
    private final UserDao userDao;
    private final PlaceMatchingService placeMatchingService;

    // ========== Plan 상세 조회 ==========

    // Plan 상세 조회 (Days + Places 포함)
    public PlanDetail getPlanDetail(Long planId) {
        log.info("Plan 상세 조회 시작: planId={}", planId);

        Plan plan = planDao.selectPlanById(planId);
        if (plan == null) {
            log.warn("Plan을 찾을 수 없음: planId={}", planId);
            throw new IllegalArgumentException("존재하지 않는 Plan입니다: planId=" + planId);
        }

        List<PlanDay> days = planDayDao.selectPlanDaysByPlanId(planId);

        List<PlanDayWithPlaces> daysWithPlaces = days.stream()
                .map(day -> {
                    List<PlanPlace> places = planPlaceDao.selectPlanPlacesByPlanDayId(day.getId());
                    return new PlanDayWithPlaces(day, places);
                })
                .toList();

        log.info("Plan 상세 조회 완료: planId={}, {}일, {}개 장소", planId, days.size(),
                daysWithPlaces.stream().mapToInt(d -> d.getPlaces().size()).sum());

        return new PlanDetail(plan, daysWithPlaces);
    }

    // 사용자의 활성화된 Plan 상세 조회 (Days + Places 포함)
    public PlanDetail getLatestPlanDetail(Long userId) {
        log.info("사용자의 활성화된 Plan 상세 조회 시작: userId={}", userId);

        User user = userDao.selectUserById(userId);
        if (user == null) {
            log.warn("User not found: userId={}", userId);
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다: userId=" + userId);
        }

        Plan plan = planDao.selectActiveTravelPlanByUserId(userId);
        if (plan == null) {
            log.warn("활성 Plan not found: userId={}", userId);
            throw new IllegalArgumentException("활성 여행 계획이 없습니다: userId=" + userId);
        }

        return getPlanDetail(plan.getId());
    }

    // 사용자별 Plan 상세 목록 조회 (모든 Plan + Days + Places)
    public List<PlanDetail> getPlanDetailsByUserId(Long userId) {
        log.info("사용자별 Plan 상세 목록 조회 시작: userId={}", userId);

        List<Plan> plans = planDao.selectPlansByUserId(userId);

        List<PlanDetail> planDetails = plans.stream()
                .map(plan -> getPlanDetail(plan.getId()))
                .toList();

        log.info("사용자별 Plan 상세 목록 조회 완료: userId={}, 총 {}개 Plan", userId, planDetails.size());
        return planDetails;
    }

    // ========== Day 조회 ==========

    // 전체 일정 조회 (모든 Day + Place)
    public List<PlanDayWithPlaces> queryAllDays(Long planId) {
        log.info("전체 일정 조회: planId={}", planId);
        List<PlanDay> days = planDayDao.selectPlanDaysByPlanId(planId);
        return days.stream()
                .map(day -> {
                    List<PlanPlace> places = planPlaceDao.selectPlanPlacesByPlanDayId(day.getId());
                    return new PlanDayWithPlaces(day, places);
                })
                .toList();
    }

    // dayIndex로 일정 조회
    public PlanDayWithPlaces queryDay(Long planId, int dayIndex) {
        log.info("일정 조회: planId={}, dayIndex={}", planId, dayIndex);
        PlanDay day = planDayDao.selectPlanDayByPlanIdAndDayIndex(planId, dayIndex);
        if (day == null) {
            return null;
        }
        List<PlanPlace> places = planPlaceDao.selectPlanPlacesByPlanDayId(day.getId());
        return new PlanDayWithPlaces(day, places);
    }

    // 날짜로 일정 조회
    public PlanDayWithPlaces queryDayByDate(Long planId, String dateStr) {
        log.info("날짜로 일정 조회: planId={}, date={}", planId, dateStr);
        LocalDate date = LocalDate.parse(dateStr);
        Plan plan = planDao.selectPlanById(planId);
        if (plan == null) {
            throw new IllegalArgumentException("Plan not found: " + planId);
        }

        int dayIndex = (int) java.time.temporal.ChronoUnit.DAYS
                .between(plan.getStartDate(), date) + 1;

        PlanDay day = planDayDao.selectPlanDayByPlanIdAndDayIndex(planId, dayIndex);
        if (day == null) {
            return null;
        }

        List<PlanPlace> places = planPlaceDao.selectPlanPlacesByPlanDayId(day.getId());
        return new PlanDayWithPlaces(day, places);
    }

    // ========== Place 조회 ==========

    // 장소명으로 검색 (부분 일치)
    public List<PlanPlace> queryPlacesByName(Long planId, String placeName) {
        log.info("장소명 검색: planId={}, placeName={}", planId, placeName);
        List<PlanDay> days = planDayDao.selectPlanDaysByPlanId(planId);
        return days.stream()
                .flatMap(day -> planPlaceDao.selectPlanPlacesByPlanDayId(day.getId()).stream())
                .filter(place -> {
                    String pn = place.getPlaceName() != null ? place.getPlaceName().toLowerCase() : "";
                    String t = place.getTitle() != null ? place.getTitle().toLowerCase() : "";
                    String q = placeName.toLowerCase();
                    return pn.contains(q) || t.contains(q);
                })
                .toList();
    }

    // 현재 시간 기준 진행중인 일정 조회
    public PlanPlace queryCurrentActivity(Long planId) {
        log.info("현재 일정 조회: planId={}", planId);
        OffsetDateTime now = OffsetDateTime.now();
        List<PlanDay> days = planDayDao.selectPlanDaysByPlanId(planId);

        return days.stream()
                .flatMap(day -> planPlaceDao.selectPlanPlacesByPlanDayId(day.getId()).stream())
                .filter(place -> place.getStartAt() != null && place.getEndAt() != null)
                .filter(place -> !now.isBefore(place.getStartAt()) && !now.isAfter(place.getEndAt()))
                .findFirst()
                .orElse(null);
    }

    // 다음 일정 조회 (현재 시간 이후)
    public PlanPlace queryNextActivity(Long planId) {
        log.info("다음 일정 조회: planId={}", planId);
        OffsetDateTime now = OffsetDateTime.now();
        List<PlanDay> days = planDayDao.selectPlanDaysByPlanId(planId);

        return days.stream()
                .flatMap(day -> planPlaceDao.selectPlanPlacesByPlanDayId(day.getId()).stream())
                .filter(place -> place.getStartAt() != null)
                .filter(place -> place.getStartAt().isAfter(now))
                .sorted((p1, p2) -> p1.getStartAt().compareTo(p2.getStartAt()))
                .findFirst()
                .orElse(null);
    }

    // 시간대별 일정 조회 (아침/점심/저녁)
    public List<PlanPlace> getPlansByTimeRange(Long userId, String timeRange) {
        log.info("시간대별 일정 조회: userId={}, timeRange={}", userId, timeRange);

        Plan activePlan = planDao.selectActiveTravelPlanByUserId(userId);
        if (activePlan == null) {
            log.info("활성 여행 계획이 없습니다: userId={}", userId);
            return Collections.emptyList();
        }

        LocalTime startTime;
        LocalTime endTime;
        switch (timeRange.toLowerCase()) {
            case "morning" -> {
                startTime = LocalTime.of(5, 0);
                endTime = LocalTime.of(11, 0);
            }
            case "lunch" -> {
                startTime = LocalTime.of(11, 0);
                endTime = LocalTime.of(15, 0);
            }
            case "evening" -> {
                startTime = LocalTime.of(17, 0);
                endTime = LocalTime.of(23, 59);
            }
            default -> {
                log.warn("알 수 없는 시간대: {}", timeRange);
                return Collections.emptyList();
            }
        }

        List<PlanDay> allDays = planDayDao.selectPlanDaysByPlanId(activePlan.getId());
        List<PlanPlace> filteredPlaces = new ArrayList<>();

        for (PlanDay day : allDays) {
            List<PlanPlace> dayPlaces = planPlaceDao.selectPlanPlacesByPlanDayId(day.getId());
            for (PlanPlace place : dayPlaces) {
                if (place.getStartAt() != null) {
                    LocalTime placeTime = place.getStartAt().toLocalTime();
                    if (!placeTime.isBefore(startTime) && placeTime.isBefore(endTime)) {
                        filteredPlaces.add(place);
                    }
                }
            }
        }

        log.info("시간대 '{}' 조회 결과: {}개 장소", timeRange, filteredPlaces.size());
        return filteredPlaces;
    }

    // ========== Fuzzy Matching 기반 조회 ==========

    // 특정 장소가 몇일차에 있는지 조회 (Fuzzy matching 지원)
    public PlanDayWithPlaces findPlaceDay(Long planId, String placeName) {
        log.info("장소→날짜 조회 (fuzzy): planId={}, placeName={}", planId, placeName);
        List<PlanDay> days = planDayDao.selectPlanDaysByPlanId(planId);

        List<String> allPlaceNames = new ArrayList<>();
        HashMap<String, PlanDay> placeToDay = new HashMap<>();

        for (PlanDay day : days) {
            List<PlanPlace> places = planPlaceDao.selectPlanPlacesByPlanDayId(day.getId());
            for (PlanPlace place : places) {
                if (place.getPlaceName() != null) {
                    allPlaceNames.add(place.getPlaceName());
                    placeToDay.put(place.getPlaceName(), day);
                }
                if (place.getTitle() != null) {
                    allPlaceNames.add(place.getTitle());
                    placeToDay.put(place.getTitle(), day);
                }
            }
        }

        String bestMatch = placeMatchingService.findClosestPlaceName(placeName, allPlaceNames);
        if (bestMatch == null) {
            return null;
        }

        PlanDay matchedDay = placeToDay.get(bestMatch);
        if (matchedDay != null) {
            List<PlanPlace> places = planPlaceDao.selectPlanPlacesByPlanDayId(matchedDay.getId());
            return new PlanDayWithPlaces(matchedDay, places);
        }

        return null;
    }

    // 장소의 위치 정보 조회 (planId 기반, Fuzzy matching)
    public PlacePosition findPlacePositionInPlan(Long planId, String placeName) {
        log.info("장소 위치 조회 (planId): planId={}, placeName={}", planId, placeName);

        List<PlanDay> allDays = planDayDao.selectPlanDaysByPlanId(planId);
        if (allDays.isEmpty()) {
            return null;
        }

        HashMap<String, PlacePosition> placePositions = new HashMap<>();

        for (PlanDay day : allDays) {
            List<PlanPlace> places = planPlaceDao.selectPlanPlacesByPlanDayId(day.getId());
            for (int i = 0; i < places.size(); i++) {
                PlanPlace place = places.get(i);
                placePositions.put(place.getPlaceName(), PlacePosition.builder()
                        .dayIndex(day.getDayIndex())
                        .order(i + 1)
                        .date(day.getPlanDate())
                        .placeName(place.getPlaceName())
                        .dayId(day.getId())
                        .build());
            }
        }

        List<String> allPlaceNames = new ArrayList<>(placePositions.keySet());
        String bestMatch = placeMatchingService.findClosestPlaceName(placeName, allPlaceNames);

        if (bestMatch == null) {
            log.info("장소를 찾을 수 없습니다: '{}'", placeName);
            return null;
        }

        log.info("Fuzzy match result: '{}' → '{}'", placeName, bestMatch);
        return placePositions.get(bestMatch);
    }

    // 장소의 위치 정보 조회 (userId 기반 활성 Plan, Fuzzy matching)
    public PlacePosition findPlacePositionInActivePlan(Long userId, String placeName) {
        log.info("장소 위치 조회 (활성 Plan): userId={}, placeName={}", userId, placeName);

        Plan activePlan = planDao.selectActiveTravelPlanByUserId(userId);
        if (activePlan == null) {
            log.info("활성 여행 계획이 없습니다: userId={}", userId);
            return null;
        }

        return findPlacePositionInPlan(activePlan.getId(), placeName);
    }

}
