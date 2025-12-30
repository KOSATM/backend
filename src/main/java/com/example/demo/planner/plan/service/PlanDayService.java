package com.example.demo.planner.plan.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.planner.plan.dao.PlanDao;
import com.example.demo.planner.plan.dao.PlanDayDao;
import com.example.demo.planner.plan.dao.PlanPlaceDao;
import com.example.demo.planner.plan.dto.entity.Plan;
import com.example.demo.planner.plan.dto.entity.PlanDay;
import com.example.demo.planner.plan.dto.entity.PlanPlace;
import com.example.demo.planner.plan.dto.response.MovePreview;
import com.example.demo.planner.plan.dto.response.PlanDetail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * PlanDay 전문 서비스
 * - Day CRUD
 * - Day 스왑, 이동
 * - Day 삭제 (연쇄 삭제)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PlanDayService {

    private final PlanDao planDao;
    private final PlanDayDao planDayDao;
    private final PlanPlaceDao planPlaceDao;

    /**
     * Day 단건 조회
     */
    public PlanDay findDayById(Long dayId) {
        log.info("PlanDay 조회: dayId={}", dayId);
        PlanDay day = planDayDao.selectPlanDayById(dayId);
        if (day == null) {
            throw new IllegalArgumentException("존재하지 않는 여행 일자입니다: dayId=" + dayId);
        }
        return day;
    }

    /**
     * Day 생성
     */
    @Transactional
    public PlanDay createDay(PlanDay day, Boolean confirm) {
        log.info("PlanDay 생성: planId={}", day.getPlanId());

        Plan plan = planDao.selectPlanById(day.getPlanId());
        if (plan == null) {
            throw new IllegalArgumentException("존재하지 않는 여행 계획입니다: planId=" + day.getPlanId());
        }

        Integer dayIndex = day.getDayIndex();

        // dayIndex가 null이면 자동 계산 (max + 1)
        if (dayIndex == null) {
            Integer maxIndex = planDayDao.selectMaxDayIndexByPlanId(day.getPlanId());
            dayIndex = (maxIndex == null) ? 1 : maxIndex + 1;
            log.info("dayIndex 자동 계산: {}", dayIndex);
        } else {
            // dayIndex가 지정된 경우 중복 체크
            PlanDay existing = planDayDao.selectPlanDayByPlanIdAndDayIndex(day.getPlanId(), dayIndex);
            if (existing != null) {
                throw new IllegalArgumentException("해당 여행 계획의 " + dayIndex + "일차가 이미 존재합니다.");
            }
        }

        // planDate 자동 계산: Plan의 startDate + (dayIndex - 1)일
        LocalDate planDate = plan.getStartDate() != null
            ? plan.getStartDate().plusDays(dayIndex - 1)
            : null;

        // Plan 기간 초과인 경우: 사용자 승인(confirm)이 있어야만 확장
        if (plan.getStartDate() != null && plan.getEndDate() != null) {
            long planDuration = java.time.temporal.ChronoUnit.DAYS.between(plan.getStartDate(), plan.getEndDate()) + 1;
            if (dayIndex > planDuration) {
                // 확장이 필요하지만 승인 없으면 예외
                if (confirm == null || !confirm) {
                    log.warn("PlanDay 생성 시 확장 승인 필요: planId={}, currentDuration={}, requestedDayIndex={}",
                        plan.getId(), planDuration, dayIndex);
                    throw new IllegalArgumentException("여행 기간 확장이 필요합니다. preview API로 확인 후 confirm=true로 호출하세요. currentDuration="
                        + planDuration + ", requestedDayIndex=" + dayIndex);
                }

                // 승인된 경우에만 endDate 확장 수행
                LocalDate newEndDate = plan.getStartDate().plusDays(dayIndex - 1);
                log.info("🔄 Plan 기간 자동 확장(승인됨): planId={}, {}일 → {}일 (endDate: {} → {})",
                    plan.getId(), planDuration, dayIndex, plan.getEndDate(), newEndDate);
                Plan updatedPlan = Plan.builder()
                    .id(plan.getId())
                    .userId(plan.getUserId())
                    .budget(plan.getBudget())
                    .startDate(plan.getStartDate())
                    .endDate(newEndDate)
                    .isEnded(plan.getIsEnded())
                    .title(plan.getTitle())
                    .build();
                planDao.updatePlan(updatedPlan);
                log.info("✅ Plan endDate 자동 업데이트 완료: {} → {}", plan.getEndDate(), newEndDate);
            }
        }

        // dayIndex와 planDate 설정하여 생성
        PlanDay newDay = PlanDay.builder()
            .planId(day.getPlanId())
            .dayIndex(dayIndex)
            .title(day.getTitle())
            .planDate(planDate)
            .build();

        planDayDao.insertPlanDay(newDay);
        log.info("PlanDay 생성 완료: dayId={}, dayIndex={}, planDate={}", newDay.getId(), dayIndex, planDate);
        return newDay;
    }

    /**
     * Day 수정 (부분 수정 지원)
     */
    @Transactional
    public void updateDay(Long dayId, PlanDay day) {
        PlanDay existing = planDayDao.selectPlanDayById(dayId);
        if (existing == null) {
            throw new IllegalArgumentException("존재하지 않는 여행 일자입니다: dayId=" + dayId);
        }

        // null이 아닌 필드만 업데이트 (부분 수정)
        PlanDay updatedDay = PlanDay.builder()
            .id(dayId)
            .planId(day.getPlanId() != null ? day.getPlanId() : existing.getPlanId())
            .dayIndex(day.getDayIndex() != null ? day.getDayIndex() : existing.getDayIndex())
            .title(day.getTitle() != null ? day.getTitle() : existing.getTitle())
            .planDate(day.getPlanDate() != null ? day.getPlanDate() : existing.getPlanDate())
            .build();

        planDayDao.updatePlanDay(updatedDay);
        log.info("PlanDay 수정 완료: dayId={}", dayId);
    }

    /**
     * Day 삭제 (연관된 Place도 함께 삭제)
     */
    @Transactional
    public void deleteDay(Long dayId) {
        PlanDay existing = planDayDao.selectPlanDayById(dayId);
        if (existing == null) {
            throw new IllegalArgumentException("존재하지 않는 여행 일자입니다: dayId=" + dayId);
        }

        // 연관된 Place 먼저 삭제
        planPlaceDao.deletePlanPlaceByDayId(dayId);

        // Day 삭제
        planDayDao.deletePlanDay(dayId);
        log.info("PlanDay 삭제 완료: dayId={}", dayId);
    }

    /**
     * 특정 날짜 전체 삭제 (dayIndex로)
     * - 해당 day와 모든 place 삭제
     * - 뒤의 day들을 앞으로 당김 (dayIndex 재조정)
     * - planDate도 업데이트
     */
    @Transactional
    public void deleteDay(Long planId, int dayIndex) {
        log.info("날짜 삭제: planId={}, dayIndex={}", planId, dayIndex);

        PlanDay targetDay = planDayDao.selectPlanDayByPlanIdAndDayIndex(planId, dayIndex);
        if (targetDay == null) {
            throw new IllegalArgumentException("Day not found: " + dayIndex);
        }

        // 해당 day의 모든 place 삭제
        List<PlanPlace> places = planPlaceDao.selectPlanPlacesByPlanDayId(targetDay.getId());
        for (PlanPlace place : places) {
            planPlaceDao.deletePlanPlaceById(place.getId());
        }
        log.info("Deleted {} places from day {}", places.size(), dayIndex);

        // day 삭제
        planDayDao.deletePlanDayById(targetDay.getId());
        log.info("Deleted day {}", dayIndex);

        // 뒤의 day들을 앞으로 당김
        List<PlanDay> remainingDays = planDayDao.selectPlanDaysByPlanId(planId);
        for (PlanDay day : remainingDays) {
            if (day.getDayIndex() > dayIndex) {
                int newIndex = day.getDayIndex() - 1;
                planDayDao.updateDayIndex(day.getId(), newIndex);
                
                // planDate도 업데이트 (startDate + newIndex - 1)
                Plan plan = planDao.selectPlanById(planId);
                LocalDate newDate = plan.getStartDate().plusDays(newIndex - 1);
                planDayDao.updatePlanDate(day.getId(), newDate);

                log.info("Day {} renumbered to {}, date updated to {}", day.getDayIndex(), newIndex, newDate);
            }
        }

        // Plan의 endDate도 하루 당김
        Plan plan = planDao.selectPlanById(planId);
        LocalDate newEndDate = plan.getEndDate().minusDays(1);
        planDao.updatePlanDates(planId, plan.getStartDate(), newEndDate);

        log.info("Plan endDate updated to {}", newEndDate);
    }

    /**
     * 전체 일정 삭제 (모든 Day와 Place 삭제)
     * - Plan 자체는 유지
     * - 모든 PlanDay와 PlanPlace 삭제
     */
    @Transactional
    public void deleteAllDaysAndPlaces(Long planId) {
        log.info("🗑️ 전체 일정 삭제: planId={}", planId);

        // 1. 해당 Plan의 모든 Day 조회
        List<PlanDay> allDays = planDayDao.selectPlanDaysByPlanId(planId);

        int totalPlaces = 0;
        // 2. 각 Day의 모든 Place 삭제
        for (PlanDay day : allDays) {
            List<PlanPlace> places = planPlaceDao.selectPlanPlacesByPlanDayId(day.getId());
            for (PlanPlace place : places) {
                planPlaceDao.deletePlanPlaceById(place.getId());
                totalPlaces++;
            }
            // 3. Day 삭제
            planDayDao.deletePlanDayById(day.getId());
        }

        // 4. Plan의 endDate를 startDate와 동일하게 설정 (0일 여행)
        Plan plan = planDao.selectPlanById(planId);
        planDao.updatePlanDates(planId, plan.getStartDate(), plan.getStartDate());

        log.info("✅ 전체 일정 삭제 완료: {}개 Day, {}개 Place 삭제됨", allDays.size(), totalPlaces);
    }

    /**
     * 두 일차(PlanDay)의 dayIndex를 서로 교체 (장소는 그대로)
     */
    @Transactional
    public void swapDay(Long planId, int dayA, int dayB) {
        if (dayA == dayB) return;
        PlanDay d1 = planDayDao.selectPlanDayByPlanIdAndDayIndex(planId, dayA);
        PlanDay d2 = planDayDao.selectPlanDayByPlanIdAndDayIndex(planId, dayB);
        if (d1 == null || d2 == null) throw new IllegalArgumentException("해당 일차가 존재하지 않습니다.");
        // 임시 인덱스(-1)로 충돌 방지 후 교체
        planDayDao.updateDayIndex(d1.getId(), -1);
        planDayDao.updateDayIndex(d2.getId(), dayA);
        planDayDao.updateDayIndex(d1.getId(), dayB);
    }

    /**
     * 두 날짜의 일정 전체 교환
     */
    @Transactional
    public void swapDaySchedules(Long planId, int dayIndexA, int dayIndexB) {
        log.info("날짜 스왑: planId={}, dayA={}, dayB={}", planId, dayIndexA, dayIndexB);

        if (dayIndexA == dayIndexB) {
            log.info("동일한 날짜이므로 변경 없음");
            return;
        }

        PlanDay dayA = planDayDao.selectPlanDayByPlanIdAndDayIndex(planId, dayIndexA);
        PlanDay dayB = planDayDao.selectPlanDayByPlanIdAndDayIndex(planId, dayIndexB);

        if (dayA == null || dayB == null) {
            throw new IllegalArgumentException("해당 일차를 찾을 수 없습니다.");
        }

        List<PlanPlace> placesA = planPlaceDao.selectPlanPlacesByPlanDayId(dayA.getId());
        List<PlanPlace> placesB = planPlaceDao.selectPlanPlacesByPlanDayId(dayB.getId());

        log.info("Day {} has {} places, Day {} has {} places", dayIndexA, placesA.size(), dayIndexB, placesB.size());

        // 1. A의 장소들을 B의 day_id로 변경
        for (PlanPlace place : placesA) {
            planPlaceDao.updatePlanDayId(place.getId(), dayB.getId());
        }

        // 2. B의 장소들을 A의 day_id로 변경
        for (PlanPlace place : placesB) {
            planPlaceDao.updatePlanDayId(place.getId(), dayA.getId());
        }

        log.info("날짜 스왑 완료: {} ↔ {}", dayIndexA, dayIndexB);
    }

    /**
     * PlanDay in-place 이동 (트랜잭션)
     * Day의 dayIndex를 toIndex로 변경하고, 영향받는 다른 Day들의 인덱스 재조정
     */
    @Transactional
    public PlanDetail moveDay(Long dayId, Integer toIndex, Boolean confirm, PlanQueryService planQueryService) {
        if (toIndex == null || toIndex < 1) {
            throw new IllegalArgumentException("toIndex는 1 이상의 정수여야 합니다.");
        }

        // 이동할 Day 조회
        PlanDay moving = planDayDao.selectPlanDayById(dayId);
        if (moving == null) {
            throw new IllegalArgumentException("존재하지 않는 여행 일자입니다: dayId=" + dayId);
        }

        Plan plan = planDao.selectPlanById(moving.getPlanId());
        if (plan == null) {
            throw new IllegalArgumentException("해당 Day의 Plan을 찾을 수 없습니다: planId=" + moving.getPlanId());
        }

        int fromIndex = moving.getDayIndex();
        if (toIndex == fromIndex) {
            // 변경 없음
            return planQueryService.getPlanDetail(plan.getId());
        }

        // 전체 Day 목록 조회 (정렬되어 반환된다고 가정)
        List<PlanDay> days = planDayDao.selectPlanDaysByPlanId(plan.getId());

        // 현재 최대 인덱스 계산 (null 안전 처리)
        int currentMaxIndex = days.stream()
                .map(PlanDay::getDayIndex)
                .filter(idx -> idx != null)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);
        boolean requiresExtension = toIndex > currentMaxIndex;

        // 확장이 필요하지만 사용자 승인이 없으면 예외로 알림
        if (requiresExtension && (confirm == null || !confirm)) {
            log.warn("확장 승인 필요: planId={}, currentMaxIndex={}, requestedToIndex={}", plan.getId(), currentMaxIndex, toIndex);
            throw new IllegalArgumentException("여행 기간 확장이 필요합니다. 먼저 preview API를 호출하여 사용자 승인을 받은 뒤 confirm=true로 호출하세요. currentMaxIndex="
                    + currentMaxIndex + ", requested=" + toIndex);
        }

        // 1) 임시로 이동 Day의 인덱스를 -1로 설정하여 유니크 제약 회피
        PlanDay temp = PlanDay.builder()
                .id(moving.getId())
                .planId(moving.getPlanId())
                .dayIndex(-1)
                .title(moving.getTitle())
                .planDate(moving.getPlanDate())
                .build();
        planDayDao.updatePlanDay(temp);

        // 2) 범위에 따라 다른 Day들을 shift
        if (fromIndex < toIndex) {
            // from+1 .. toIndex -> 각자 -1
            for (PlanDay d : days) {
                Integer idx = d.getDayIndex();
                if (idx != null && idx > fromIndex && idx <= toIndex) {
                    PlanDay updated = PlanDay.builder()
                            .id(d.getId())
                            .planId(d.getPlanId())
                            .dayIndex(idx - 1)
                            .title(d.getTitle())
                            .planDate(d.getPlanDate())
                            .build();
                    planDayDao.updatePlanDay(updated);
                }
            }
        } else {
            // toIndex .. from-1 -> 각자 +1
            for (PlanDay d : days) {
                Integer idx = d.getDayIndex();
                if (idx != null && idx >= toIndex && idx < fromIndex) {
                    PlanDay updated = PlanDay.builder()
                            .id(d.getId())
                            .planId(d.getPlanId())
                            .dayIndex(idx + 1)
                            .title(d.getTitle())
                            .planDate(d.getPlanDate())
                            .build();
                    planDayDao.updatePlanDay(updated);
                }
            }
        }

        // 3) 이동 Day을 목표 인덱스로 설정
        PlanDay moved = PlanDay.builder()
                .id(moving.getId())
                .planId(moving.getPlanId())
                .dayIndex(toIndex)
                .title(moving.getTitle())
                .planDate(moving.getPlanDate())
                .build();
        planDayDao.updatePlanDay(moved);

        // 4) 영향을 받은 Day들의 planDate 재계산 (Plan의 startDate 기준)
        List<PlanDay> updatedDays = planDayDao.selectPlanDaysByPlanId(plan.getId());
        for (PlanDay d : updatedDays) {
            if (plan.getStartDate() != null && d.getDayIndex() != null) {
                LocalDate newDate = plan.getStartDate().plusDays(d.getDayIndex() - 1);
                PlanDay pd = PlanDay.builder()
                        .id(d.getId())
                        .planId(d.getPlanId())
                        .dayIndex(d.getDayIndex())
                        .title(d.getTitle())
                        .planDate(newDate)
                        .build();
                planDayDao.updatePlanDay(pd);
            }
        }

        // 5) Plan 기간 확장 적용 (confirm이 true인 경우에만 실행됨)
        if (requiresExtension && plan.getStartDate() != null) {
            LocalDate newEndDate = plan.getStartDate().plusDays(toIndex - 1);
            Plan updatedPlan = Plan.builder()
                    .id(plan.getId())
                    .userId(plan.getUserId())
                    .budget(plan.getBudget())
                    .startDate(plan.getStartDate())
                    .endDate(newEndDate)
                    .isEnded(plan.getIsEnded())
                    .title(plan.getTitle())
                    .build();
            planDao.updatePlan(updatedPlan);
            log.info("🔄 Plan 기간 확장 완료(이동): planId={}, newEndDate={}", plan.getId(), newEndDate);
        }

        return planQueryService.getPlanDetail(plan.getId());
    }

    /**
     * 이동 미리보기: 확장 필요 여부 및 예상 endDate 계산
     */
    public MovePreview movePreview(Long dayId, Integer toIndex) {
        if (toIndex == null || toIndex < 1) {
            throw new IllegalArgumentException("toIndex는 1 이상의 정수여야 합니다.");
        }

        PlanDay moving = planDayDao.selectPlanDayById(dayId);
        if (moving == null) {
            throw new IllegalArgumentException("존재하지 않는 여행 일자입니다: dayId=" + dayId);
        }

        Plan plan = planDao.selectPlanById(moving.getPlanId());
        if (plan == null) {
            throw new IllegalArgumentException("해당 Day의 Plan을 찾을 수 없습니다: planId=" + moving.getPlanId());
        }

        List<PlanDay> days = planDayDao.selectPlanDaysByPlanId(plan.getId());
        int currentMaxIndex = days.stream()
                .map(PlanDay::getDayIndex)
                .filter(idx -> idx != null)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);
        boolean requiresExtension = toIndex > currentMaxIndex;
        LocalDate newEndDate = null;
        if (requiresExtension && plan.getStartDate() != null) {
            newEndDate = plan.getStartDate().plusDays(toIndex - 1);
        }

        return new MovePreview(requiresExtension, newEndDate, currentMaxIndex, toIndex);
    }

    /**
     * PlanDay 생성 미리보기: planId와 dayIndex로 확장 필요 여부 및 예상 endDate 계산
     */
    public MovePreview createDayPreview(Long planId, Integer dayIndex) {
        if (dayIndex == null || dayIndex < 1) {
            throw new IllegalArgumentException("dayIndex는 1 이상의 정수여야 합니다.");
        }

        Plan plan = planDao.selectPlanById(planId);
        if (plan == null) {
            throw new IllegalArgumentException("존재하지 않는 여행 계획입니다: planId=" + planId);
        }

        if (plan.getStartDate() == null || plan.getEndDate() == null) {
            // startDate가 없는 경우 확장 여부를 판단할 수 없으므로 requiresExtension=true
            LocalDate newEndDate = plan.getStartDate() != null ? plan.getStartDate().plusDays(dayIndex - 1) : null;
            return new MovePreview(true, newEndDate, 0, dayIndex);
        }

        long planDuration = java.time.temporal.ChronoUnit.DAYS.between(plan.getStartDate(), plan.getEndDate()) + 1;
        Integer maxIndexResult = planDayDao.selectMaxDayIndexByPlanId(planId);
        int currentMaxIndex = (maxIndexResult != null) ? maxIndexResult : 0;
        boolean requiresExtension = dayIndex > planDuration || dayIndex > currentMaxIndex;
        LocalDate newEndDate = null;
        if (requiresExtension) {
            newEndDate = plan.getStartDate().plusDays(dayIndex - 1);
        }

        return new MovePreview(requiresExtension, newEndDate, currentMaxIndex, dayIndex);
    }
}
