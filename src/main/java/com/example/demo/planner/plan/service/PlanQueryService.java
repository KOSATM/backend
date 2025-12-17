package com.example.demo.planner.plan.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.demo.common.user.dao.UserDao;
import com.example.demo.common.user.dto.User;
import com.example.demo.planner.plan.dao.PlanDao;
import com.example.demo.planner.plan.dao.PlanDayDao;
import com.example.demo.planner.plan.dao.PlanPlaceDao;
import com.example.demo.planner.plan.dto.entity.Plan;
import com.example.demo.planner.plan.dto.entity.PlanDay;
import com.example.demo.planner.plan.dto.entity.PlanPlace;
import com.example.demo.planner.plan.dto.response.ActivePlanInfoResponse;
import com.example.demo.planner.plan.dto.response.PlacePosition;
import com.example.demo.planner.plan.dto.response.PlanDayWithPlaces;
import com.example.demo.planner.plan.dto.response.PlanDetail;
import com.example.demo.planner.plan.util.FuzzyUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Plan 조회 전문 서비스
 * - 전체 조회, 검색, Fuzzy matching
 * - 복잡한 조회 로직 담당
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PlanQueryService {

    private final PlanDao planDao;
    private final PlanDayDao planDayDao;
    private final PlanPlaceDao planPlaceDao;
    private final UserDao userDao;
    private final FuzzyUtils fuzzyUtils;

    /**
     * 특정 일차의 전체 일정 조회 (PlanDay + PlanPlace 리스트)
     */
    public PlanDayWithPlaces queryDay(Long planId, int dayIndex) {
        log.info("일차 조회: planId={}, dayIndex={}", planId, dayIndex);
        PlanDay day = planDayDao.selectPlanDayByPlanIdAndDayIndex(planId, dayIndex);
        if (day == null) throw new IllegalArgumentException(dayIndex + "일차가 존재하지 않습니다.");
        List<PlanPlace> places = planPlaceDao.selectPlanPlacesByPlanDayId(day.getId());
        return new PlanDayWithPlaces(day, places);
    }

    /**
     * 특정 일차의 특정 장소 조회 (placeIndex는 1부터 시작)
     */
    public PlanPlace queryPlace(Long planId, int dayIndex, int placeIndex) {
        log.info("장소 조회: planId={}, dayIndex={}, placeIndex={}", planId, dayIndex, placeIndex);
        PlanDay day = planDayDao.selectPlanDayByPlanIdAndDayIndex(planId, dayIndex);
        if (day == null) throw new IllegalArgumentException(dayIndex + "일차가 존재하지 않습니다.");
        List<PlanPlace> places = planPlaceDao.selectPlanPlacesByPlanDayId(day.getId());
        if (placeIndex < 1 || placeIndex > places.size()) {
            throw new IllegalArgumentException(dayIndex + "일차의 " + placeIndex + "번째 장소가 존재하지 않습니다.");
        }
        return places.get(placeIndex - 1);
    }

    /**
     * 전체 일정 조회 (구버전 - N+1 문제 있음)
     */
    public List<PlanDayWithPlaces> queryAllDays(Long planId) {
        log.info("전체 일정 조회 (구버전): planId={}", planId);
        List<PlanDay> days = planDayDao.selectPlanDaysByPlanId(planId);
        return days.stream()
            .map(day -> {
                List<PlanPlace> places = planPlaceDao.selectPlanPlacesByPlanDayId(day.getId());
                return new PlanDayWithPlaces(day, places);
            })
            .collect(Collectors.toList());
    }

    /**
     * 🔥 전체 일정 조회 (최적화 버전 - 2개의 쿼리로 전체 데이터 로드)
     */
    public List<PlanDayWithPlaces> queryAllDaysOptimized(Long planId) {
        log.info("🚀 전체 일정 조회 (최적화): planId={}", planId);

        // 1. Days 조회 (1회 쿼리)
        List<PlanDay> days = planDayDao.selectPlanDaysByPlanId(planId);

        // 2. 전체 Places를 JOIN으로 한 번에 조회 (1회 쿼리)
        List<PlanPlace> allPlaces = planPlaceDao.selectPlanPlacesByPlanId(planId);

        // 3. Places를 Day별로 그룹핑
        Map<Long, List<PlanPlace>> placesGroupedByDay = allPlaces.stream()
            .collect(Collectors.groupingBy(PlanPlace::getDayId));

        // 4. Days와 Places 결합
        return days.stream()
            .map(day -> {
                List<PlanPlace> places = placesGroupedByDay.getOrDefault(
                    day.getId(),
                    Collections.emptyList()
                );
                return new PlanDayWithPlaces(day, places);
            })
            .collect(Collectors.toList());
    }

    /**
     * 날짜로 일정 조회
     */
    public PlanDayWithPlaces queryDayByDate(Long planId, String dateStr) {
        log.info("날짜로 일정 조회: planId={}, date={}", planId, dateStr);
        LocalDate date = LocalDate.parse(dateStr);
        Plan plan = planDao.selectPlanById(planId);
        if (plan == null) {
            throw new IllegalArgumentException("Plan not found: " + planId);
        }

        // 시작일로부터 몇 일째인지 계산
        int dayIndex = (int) java.time.temporal.ChronoUnit.DAYS.between(plan.getStartDate(), date) + 1;
        return queryDay(planId, dayIndex);
    }

    /**
     * DayId로 장소 목록 조회
     */
    public List<PlanPlace> queryPlacesByDayId(Long dayId) {
        return planPlaceDao.selectPlanPlacesByPlanDayId(dayId);
    }

    /**
     * 장소명으로 검색 (부분 일치)
     */
    public List<PlanPlace> queryPlacesByName(Long planId, String placeName) {
        log.info("장소명 검색: planId={}, placeName={}", planId, placeName);
        List<PlanDay> days = planDayDao.selectPlanDaysByPlanId(planId);
        return days.stream()
            .flatMap(day -> planPlaceDao.selectPlanPlacesByPlanDayId(day.getId()).stream())
            .filter(place -> place.getPlaceName().toLowerCase().contains(placeName.toLowerCase()) ||
                            place.getTitle().toLowerCase().contains(placeName.toLowerCase()))
            .collect(Collectors.toList());
    }

    /**
     * 현재 시간 기준 일정 조회
     */
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

    /**
     * 다음 일정 조회
     */
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

    /**
     * 특정 장소가 몇일차에 있는지 조회 (Fuzzy matching 지원)
     */
    public PlanDayWithPlaces findPlaceDay(Long planId, String placeName) {
        log.info("장소→날짜 조회 (fuzzy): planId={}, placeName={}", planId, placeName);
        List<PlanDay> days = planDayDao.selectPlanDaysByPlanId(planId);

        // 1. 모든 장소명 수집
        List<String> allPlaceNames = new ArrayList<>();
        Map<String, PlanDay> placeToDay = new HashMap<>();

        for (PlanDay day : days) {
            List<PlanPlace> places = planPlaceDao.selectPlanPlacesByPlanDayId(day.getId());
            for (PlanPlace place : places) {
                allPlaceNames.add(place.getPlaceName());
                allPlaceNames.add(place.getTitle());
                placeToDay.put(place.getPlaceName(), day);
                placeToDay.put(place.getTitle(), day);
            }
        }

        // 2. Fuzzy matching으로 가장 가까운 장소명 찾기
        String bestMatch = fuzzyUtils.findClosestPlaceName(placeName, allPlaceNames);

        if (bestMatch == null) {
            return null;
        }

        log.info("Fuzzy match result: '{}' → '{}'", placeName, bestMatch);

        // 3. 매칭된 장소가 속한 Day 반환
        PlanDay matchedDay = placeToDay.get(bestMatch);
        if (matchedDay != null) {
            List<PlanPlace> places = planPlaceDao.selectPlanPlacesByPlanDayId(matchedDay.getId());
            return new PlanDayWithPlaces(matchedDay, places);
        }

        return null;
    }

    /**
     * 장소의 위치 정보 조회 (planId 직접 지정)
     * Fuzzy matching 기반
     */
    public PlacePosition findPlacePositionByPlanId(String placeName, Long planId) {
        log.info("장소 위치 조회 (planId): placeName={}, planId={}", placeName, planId);

        // 1. 모든 PlanDay 조회
        List<PlanDay> allDays = planDayDao.selectPlanDaysByPlanId(planId);
        if (allDays.isEmpty()) {
            return null;
        }

        // 2. 모든 PlanPlace 조회하여 fuzzy matching
        Map<String, PlacePosition> placePositions = new HashMap<>();

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

        // 3. Fuzzy matching으로 가장 가까운 장소명 찾기
        List<String> allPlaceNames = new ArrayList<>(placePositions.keySet());
        String bestMatch = fuzzyUtils.findClosestPlaceName(placeName, allPlaceNames);

        if (bestMatch == null) {
            log.info("장소를 찾을 수 없습니다: '{}'", placeName);
            return null;
        }

        log.info("Fuzzy match result: '{}' → '{}'", placeName, bestMatch);
        return placePositions.get(bestMatch);
    }

    /**
     * 특정 Day의 모든 장소 조회 (order 순서대로)
     */
    public List<PlanPlace> getDayPlaces(Long dayId) {
        log.info("Day 장소 목록 조회: dayId={}", dayId);
        return planPlaceDao.selectPlanPlacesByPlanDayId(dayId);
    }

    /**
     * 시간대별 일정 조회 (아침/점심/저녁)
     */
    public List<PlanPlace> getPlansByTimeRange(Long planId, String timeRange) {
        log.info("시간대별 일정 조회: planId={}, timeRange={}", planId, timeRange);

        // 시간 범위 정의
        int startHour, endHour;
        switch (timeRange.toLowerCase()) {
            case "morning":
                startHour = 6;
                endHour = 12;
                break;
            case "lunch":
                startHour = 12;
                endHour = 14;
                break;
            case "afternoon":
                startHour = 14;
                endHour = 18;
                break;
            case "evening":
                startHour = 18;
                endHour = 22;
                break;
            default:
                log.warn("알 수 없는 시간대: {}", timeRange);
                return Collections.emptyList();
        }

        List<PlanDay> days = planDayDao.selectPlanDaysByPlanId(planId);

        return days.stream()
            .flatMap(day -> planPlaceDao.selectPlanPlacesByPlanDayId(day.getId()).stream())
            .filter(place -> place.getStartAt() != null)
            .filter(place -> {
                int hour = place.getStartAt().getHour();
                return hour >= startHour && hour < endHour;
            })
            .collect(Collectors.toList());
    }

    /**
     * 활성 Plan ID와 현재 dayIndex 조회
     */
    public ActivePlanInfoResponse getActivePlanIdAndDayIndex(Long userId) {
        log.info("활성 Plan 정보 조회: userId={}", userId);
        return planDao.selectPlanIdAndCurrentDayIndex(userId);
    }

    /**
     * Plan 상세 조회 (Days + Places 포함)
     */
    public PlanDetail getPlanDetail(Long planId) {
        log.info("Plan 상세 조회 시작: planId={}", planId);

        // 1. Plan 조회
        Plan plan = planDao.selectPlanById(planId);
        if (plan == null) {
            log.warn("Plan을 찾을 수 없음: planId={}", planId);
            throw new IllegalArgumentException("존재하지 않는 Plan입니다: planId=" + planId);
        }

        // 2. 최적화된 방식으로 모든 Days + Places 조회 (2 queries)
        List<PlanDayWithPlaces> daysWithPlaces = queryAllDaysOptimized(planId);

        log.info("Plan 상세 조회 완료: planId={}, days={}, 총 places={}",
            planId, daysWithPlaces.size(),
            daysWithPlaces.stream().mapToInt(d -> d.getPlaces().size()).sum());

        return new PlanDetail(plan, daysWithPlaces);
    }

    /**
     * 사용자의 활성화된 Plan 상세 조회 (Days + Places 포함)
     */
    public PlanDetail getLatestPlanDetail(Long userId) {
        log.info("사용자의 활성화된 Plan 상세 조회 시작: userId={}", userId);

        // 1. 사용자 조회
        User user = userDao.selectUserById(userId);
        if (user == null) {
            log.warn("존재하지 않는 사용자: userId={}", userId);
            throw new IllegalArgumentException("존재하지 않는 사용자입니다: userId=" + userId);
        }

        // 2. Plan 조회
        Plan plan = planDao.selectActiveTravelPlanByUserId(userId);
        if (plan == null || Boolean.TRUE.equals(plan.getIsEnded())) {
            log.warn("사용자의 활성화된 Plan을 찾을 수 없음: userId={}", userId);
            // throw new IllegalArgumentException("활성화된 Plan이 없습니다: userId=" + userId);
            return null;
        }
        // if(plan.getIsEnded() == true){
        //     log.warn("사용자의 일정이 없습니다. userId={}", userId);
        //     return null;
        // }
        long planId = plan.getId();

        // 3. Plan의 모든 Day 조회
        List<PlanDay> days = planDayDao.selectPlanDaysByPlanId(planId);

        // 4. 각 Day의 Places를 조회하여 PlanDayWithPlaces 생성
        List<PlanDayWithPlaces> daysWithPlaces = days.stream()
            .map(day -> {
                List<PlanPlace> places = planPlaceDao.selectPlanPlacesByPlanDayId(day.getId());
                return new PlanDayWithPlaces(day, places);
            })
            .collect(Collectors.toList());

        log.info("Plan 상세 조회 완료: planId={}, days={}, 총 places={}",
            planId, daysWithPlaces.size(),
            daysWithPlaces.stream().mapToInt(d -> d.getPlaces().size()).sum());

        return new PlanDetail(plan, daysWithPlaces);
    }

    /**
     * 사용자별 Plan 상세 목록 조회 (모든 Plan + Days + Places)
     */
    public List<PlanDetail> getPlanDetailsByUserId(Long userId) {
        log.info("사용자별 Plan 상세 목록 조회 시작: userId={}", userId);

        // 1. 사용자의 모든 Plan 조회
        List<Plan> plans = planDao.selectPlansByUserId(userId);

        // 2. 각 Plan의 상세 정보 조회
        List<PlanDetail> planDetails = plans.stream()
            .map(plan -> getPlanDetail(plan.getId()))
            .collect(Collectors.toList());

        log.info("사용자별 Plan 상세 목록 조회 완료: userId={}, 총 {}개 Plan", userId, planDetails.size());
        return planDetails;
    }
}
