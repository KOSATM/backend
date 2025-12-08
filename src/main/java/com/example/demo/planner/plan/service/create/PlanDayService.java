package com.example.demo.planner.plan.service.create;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.planner.plan.dao.PlanDao;
import com.example.demo.planner.plan.dao.PlanDayDao;
import com.example.demo.planner.plan.dao.PlanPlaceDao;
import com.example.demo.planner.plan.dto.entity.Plan;
import com.example.demo.planner.plan.dto.entity.PlanDay;
import com.example.demo.planner.plan.dto.response.MovePreview;
import com.example.demo.planner.plan.dto.response.PlanDetail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// PlanDay 생성/수정/삭제 전용 서비스
@Service
@Slf4j
@RequiredArgsConstructor
public class PlanDayService {

    private final PlanDayDao planDayDao;
    private final PlanDao planDao;
    private final PlanPlaceDao planPlaceDao;
    private final PlanQueryService planQueryService;

    // ========== 조회 (READ) ==========

    // PlanDay 단건 조회 by ID
    public PlanDay findDayById(Long dayId) {
        log.info("PlanDay 조회: dayId={}", dayId);
        PlanDay day = planDayDao.selectPlanDayById(dayId);
        if (day == null) {
            throw new IllegalArgumentException("존재하지 않는 여행 일자입니다: dayId=" + dayId);
        }
        return day;
    }

    // Plan의 모든 Day 조회 (간편 메서드)
    public List<PlanDay> getPlanDaysByPlanId(Long planId) {
        log.info("Plan의 모든 Day 조회: planId={}", planId);
        return planDayDao.selectPlanDaysByPlanId(planId);
    }

    // ========== 생성 (CREATE) ==========

    // PlanDay 생성 (dayIndex 자동 계산 + 확장 승인 처리)
    @Transactional
    public PlanDay createDay(PlanDay day, Boolean confirm) {
        log.info("PlanDay 생성: planId={}", day.getPlanId());

        Plan plan = planDao.selectPlanById(day.getPlanId());
        if (plan == null) {
            throw new IllegalArgumentException("존재하지 않는 여행 계획입니다: planId=" + day.getPlanId());
        }

        Integer dayIndex = day.getDayIndex();

        if (dayIndex == null) {
            Integer maxIndex = planDayDao.selectMaxDayIndexByPlanId(day.getPlanId());
            dayIndex = (maxIndex == null) ? 1 : maxIndex + 1;
            log.info("dayIndex 자동 계산: {}", dayIndex);
        } else {
            PlanDay existing = planDayDao.selectPlanDayByPlanIdAndDayIndex(day.getPlanId(), dayIndex);
            if (existing != null) {
                throw new IllegalArgumentException("해당 여행 계획의 " + dayIndex + "일차가 이미 존재합니다.");
            }
        }

        LocalDate planDate = plan.getStartDate() != null
                ? plan.getStartDate().plusDays(dayIndex - 1)
                : null;

        if (plan.getStartDate() != null && plan.getEndDate() != null) {
            long planDuration = java.time.temporal.ChronoUnit.DAYS
                    .between(plan.getStartDate(), plan.getEndDate()) + 1;
            if (dayIndex > planDuration) {
                if (confirm == null || !confirm) {
                    log.warn("PlanDay 생성 시 확장 승인 필요: planId={}, currentDuration={}, requestedDayIndex={}",
                            plan.getId(), planDuration, dayIndex);
                    throw new IllegalArgumentException(
                            "여행 기간 확장이 필요합니다. preview API로 확인 후 confirm=true로 호출하세요. currentDuration="
                                    + planDuration + ", requestedDayIndex=" + dayIndex);
                }

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

    // PlanDay 생성 미리보기 (확장 필요 여부 및 예상 endDate 계산)
    public MovePreview createDayPreview(Long planId, Integer dayIndex) {
        if (dayIndex == null || dayIndex < 1) {
            throw new IllegalArgumentException("dayIndex는 1 이상의 정수여야 합니다.");
        }

        Plan plan = planDao.selectPlanById(planId);
        if (plan == null) {
            throw new IllegalArgumentException("존재하지 않는 여행 계획입니다: planId=" + planId);
        }

        if (plan.getStartDate() == null || plan.getEndDate() == null) {
            LocalDate newEndDate = plan.getStartDate() != null ? plan.getStartDate().plusDays(dayIndex - 1) : null;
            return new MovePreview(true, newEndDate, 0, dayIndex);
        }

        long planDuration = java.time.temporal.ChronoUnit.DAYS
                .between(plan.getStartDate(), plan.getEndDate()) + 1;
        Integer maxIndexResult = planDayDao.selectMaxDayIndexByPlanId(planId);
        int currentMaxIndex = (maxIndexResult != null) ? maxIndexResult : 0;
        boolean requiresExtension = dayIndex > planDuration || dayIndex > currentMaxIndex;
        LocalDate newEndDate = null;
        if (requiresExtension) {
            newEndDate = plan.getStartDate().plusDays(dayIndex - 1);
        }

        return new MovePreview(requiresExtension, newEndDate, currentMaxIndex, dayIndex);
    }

    // ========== 수정 (UPDATE) ==========

    // PlanDay 부분 수정 (전체 필드)
    @Transactional
    public void updateDay(Long dayId, PlanDay day) {
        PlanDay existing = planDayDao.selectPlanDayById(dayId);
        if (existing == null) {
            throw new IllegalArgumentException("존재하지 않는 여행 일자입니다: dayId=" + dayId);
        }

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

    // Day 제목만 수정
    @Transactional
    public void updateDayTitle(Long dayId, String title) {
        log.info("Day 제목 수정: dayId={}, title={}", dayId, title);

        PlanDay existing = planDayDao.selectPlanDayById(dayId);
        if (existing == null) {
            throw new IllegalArgumentException("존재하지 않는 여행 일자입니다: dayId=" + dayId);
        }

        PlanDay updated = PlanDay.builder()
                .id(dayId)
                .planId(existing.getPlanId())
                .dayIndex(existing.getDayIndex())
                .title(title)
                .planDate(existing.getPlanDate())
                .build();

        planDayDao.updatePlanDay(updated);
        log.info("Day 제목 수정 완료: {}", title);
    }

    // Day 인덱스 이동 + 날짜 재계산
    @Transactional
    public PlanDetail moveDay(Long dayId, Integer toIndex, Boolean confirm) {
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

        int fromIndex = moving.getDayIndex();
        if (toIndex == fromIndex) {
            return planQueryService.getPlanDetail(plan.getId());
        }

        List<PlanDay> days = planDayDao.selectPlanDaysByPlanId(plan.getId());

        int currentMaxIndex = days.stream()
                .map(PlanDay::getDayIndex)
                .filter(idx -> idx != null)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);
        boolean requiresExtension = toIndex > currentMaxIndex;

        if (requiresExtension && (confirm == null || !confirm)) {
            log.warn("확장 승인 필요: planId={}, currentMaxIndex={}, requestedToIndex={}",
                    plan.getId(), currentMaxIndex, toIndex);
            throw new IllegalArgumentException(
                    "여행 기간 확장이 필요합니다. preview API 호출 후 confirm=true로 호출하세요. currentMaxIndex="
                            + currentMaxIndex + ", requested=" + toIndex);
        }

        PlanDay temp = PlanDay.builder()
                .id(moving.getId())
                .planId(moving.getPlanId())
                .dayIndex(-1)
                .title(moving.getTitle())
                .planDate(moving.getPlanDate())
                .build();
        planDayDao.updatePlanDay(temp);

        if (fromIndex < toIndex) {
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

        PlanDay moved = PlanDay.builder()
                .id(moving.getId())
                .planId(moving.getPlanId())
                .dayIndex(toIndex)
                .title(moving.getTitle())
                .planDate(moving.getPlanDate())
                .build();
        planDayDao.updatePlanDay(moved);

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

    // Day 이동 미리보기 (확장 필요 여부 및 예상 endDate 계산)
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

    // ========== 삭제 (DELETE) ==========

    // PlanDay 삭제 (dayId 기반, 연관 Place 포함)
    @Transactional
    public void deleteDayById(Long dayId) {
        PlanDay existing = planDayDao.selectPlanDayById(dayId);
        if (existing == null) {
            throw new IllegalArgumentException("존재하지 않는 여행 일자입니다: dayId=" + dayId);
        }

        planPlaceDao.deletePlacesByDayId(dayId);
        planDayDao.deletePlanDay(dayId);
        log.info("PlanDay 삭제 완료: dayId={}", dayId);
    }

    // PlanDay 삭제 (dayIndex 기반 + 뒤 일자 인덱스/날짜 당김)
    @Transactional
    public void deleteDayByIndex(Long planId, int dayIndex) {
        log.info("날짜 삭제: planId={}, dayIndex={}", planId, dayIndex);

        PlanDay targetDay = planDayDao.selectPlanDayByPlanIdAndDayIndex(planId, dayIndex);
        if (targetDay == null) {
            throw new IllegalArgumentException("Day not found: " + dayIndex);
        }

        int deletedCount = planPlaceDao.deletePlacesByDayId(targetDay.getId());
        log.info("{}일차의 장소 {}개 삭제 완료", dayIndex, deletedCount);

        planDayDao.deletePlanDayById(targetDay.getId());
        log.info("Deleted day {}", dayIndex);

        List<PlanDay> remainingDays = planDayDao.selectPlanDaysByPlanId(planId);
        Plan plan = planDao.selectPlanById(planId);

        for (PlanDay day : remainingDays) {
            if (day.getDayIndex() > dayIndex) {
                int newIndex = day.getDayIndex() - 1;
                planDayDao.updateDayIndex(day.getId(), newIndex);

                LocalDate newDate = plan.getStartDate().plusDays(newIndex - 1);
                planDayDao.updatePlanDate(day.getId(), newDate);

                log.info("Day {} renumbered to {}, date updated to {}", day.getDayIndex(), newIndex, newDate);
            }
        }

        LocalDate newEndDate = plan.getEndDate().minusDays(1);
        planDao.updatePlanDates(planId, plan.getStartDate(), newEndDate);

        log.info("Plan endDate updated to {}", newEndDate);
    }

}
