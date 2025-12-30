package com.example.demo.planner.plan.agent.tools;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.planner.plan.dao.PlanDao;
import com.example.demo.planner.plan.dao.PlanDayDao;
import com.example.demo.planner.plan.dao.PlanPlaceDao;
import com.example.demo.planner.plan.dao.PlanSnapshotDao;
import com.example.demo.planner.plan.dto.entity.Plan;
import com.example.demo.planner.plan.dto.entity.PlanDay;
import com.example.demo.planner.plan.dto.entity.PlanPlace;
import com.example.demo.planner.plan.dto.entity.PlanSnapshot;
import com.example.demo.planner.plan.dto.response.PlanSnapshotContent;
import com.example.demo.planner.plan.service.PlanSnapshotService;
import com.example.demo.planner.plan.service.PlanSnapshotUtility;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 버전 관리 도구
 * - 실제 서비스 함수 호출
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class VersionManagementTools {

    private final PlanSnapshotService planSnapshotService;
    private final PlanSnapshotUtility planSnapshotUtility;
    private final PlanSnapshotDao planSnapshotDao;
    private final PlanDao planDao;
    private final PlanDayDao planDayDao;
    private final PlanPlaceDao planPlaceDao;

    private final DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Transactional
    @Tool(description = "바로 이전 버전으로 롤백")
    public String rollBack(Long planId, ToolContext toolContext) {
        log.info("🔧 [VersionManagementTools] rollBack: planId={}", planId);

        try {
            Long userId = (Long) toolContext.getContext().get("userId");
            PlanSnapshot planSnapshot = planSnapshotService.getPlanSnapshotsByUserId(userId).get(1);
            PlanSnapshotContent snapshotContent = planSnapshotUtility.parseSnapshot(planSnapshot.getSnapshotJson());

            Plan plan = planDao.selectPlanById(planId);

            // 기존 데이터 삭제
            List<PlanPlace> existingPlaces = planPlaceDao.selectPlanPlacesByPlanId(planId);
            for (PlanPlace place : existingPlaces) {
                planPlaceDao.deletePlanPlaceById(place.getId());
            }
            log.info("plan_places 삭제 완료");

            List<PlanDay> existingDays = planDayDao.selectPlanDaysByPlanId(planId);
            for (PlanDay day : existingDays) {
                planDayDao.deletePlanDay(day.getId());
            }
            log.info("plan_days 삭제 완료");

            // Plan 업데이트
            Plan rollbackPlan = Plan.builder()
                    .id(planId)
                    .userId(userId)
                    .budget(snapshotContent.getBudget())
                    .startDate(LocalDate.parse(snapshotContent.getStartDate(), formatter1))
                    .endDate(LocalDate.parse(snapshotContent.getEndDate(), formatter1))
                    .createdAt(plan.getCreatedAt())
                    .updatedAt(OffsetDateTime.now())
                    .build();
            planDao.updatePlan(rollbackPlan);
            log.info("Plan 업데이트 완료");

            // PlanDay 재생성
            Map<String, Long> dateToDayId = new HashMap<>();
            for (int i = 0; i < snapshotContent.getDays().size(); i++) {
                PlanSnapshotContent.PlanDay pscDay = snapshotContent.getDays().get(i);

                PlanDay newDay = PlanDay.builder()
                        .planId(planId)
                        .dayIndex(i + 1)
                        .title(pscDay.getTitle())
                        .planDate(LocalDate.parse(pscDay.getDate(), formatter1))
                        .build();

                planDayDao.insertPlanDay(newDay);
                dateToDayId.put(pscDay.getDate(), newDay.getId());
            }
            log.info("PlanDays 재생성 완료");

            // PlanPlace 재생성
            for (PlanSnapshotContent.PlanDay pscDay : snapshotContent.getDays()) {
                Long dayId = dateToDayId.get(pscDay.getDate());

                for (PlanSnapshotContent.PlanDayItem pscItem : pscDay.getSchedules()) {
                    PlanPlace newPlace = PlanPlace.builder()
                            .dayId(dayId)
                            .title(pscItem.getTitle())
                            .startAt(LocalDateTime.parse(pscItem.getStartAt(), formatter2)
                                    .atOffset(ZoneOffset.of("+00:00")))
                            .endAt(LocalDateTime.parse(pscItem.getEndAt(), formatter2)
                                    .atOffset(ZoneOffset.of("+00:00")))
                            .placeName(pscItem.getPlaceName())
                            .address(pscItem.getAddress())
                            .lat(pscItem.getLat())
                            .lng(pscItem.getLng())
                            .expectedCost(pscItem.getExpectedCost())
                            .normalizedCategory(pscItem.getNormalizedCategory())
                            .firstImage(pscItem.getFirstImage())
                            .firstImage2(pscItem.getFirstImage2())
                            .isEnded(pscItem.getIsEnded() == null ? false : pscItem.getIsEnded())
                            .build();

                    planPlaceDao.insertPlanPlace(newPlace);
                }
            }
            log.info("PlanPlaces 재생성 완료");

            // 새 스냅샷 저장
            List<PlanDay> newDays = planDayDao.selectPlanDaysByPlanId(planId);
            List<PlanPlace> newPlaces = planPlaceDao.selectPlanPlacesByPlanId(planId);
            PlanSnapshot newSnapshot = planSnapshotService.savePlanSnapshot(rollbackPlan, newDays, newPlaces);
            Integer newVersionNo = newSnapshot.getVersionNo();

            log.info("버전 환원 완료");
            return String.format("✅ 이전 버전으로 돌아갔습니다. 버전: %d", newVersionNo);

        } catch (Exception e) {
            log.error("롤백 실패", e);
            return String.format("❌ 버전 환원 중 오류 발생: %s", e.getMessage());
        }
    }

    @Transactional
    @Tool(description = "특정 버전으로 롤백")
    public String rollBackToSpecific(Long planId, Integer versionNo, ToolContext toolContext) {
        log.info("🔧 [VersionManagementTools] rollBackToSpecific: planId={}, version={}", planId, versionNo);

        try {
            Long userId = (Long) toolContext.getContext().get("userId");

            PlanSnapshot toRevert = PlanSnapshot.builder()
                    .userId(userId)
                    .versionNo(versionNo)
                    .build();

            PlanSnapshot planSnapshot = planSnapshotDao.selectPlanSnapshotByUserIdAndVersionNo(toRevert);
            PlanSnapshotContent snapshotContent = planSnapshotUtility.parseSnapshot(planSnapshot.getSnapshotJson());

            Plan plan = planDao.selectPlanById(planId);

            // 기존 데이터 삭제
            List<PlanPlace> existingPlaces = planPlaceDao.selectPlanPlacesByPlanId(planId);
            for (PlanPlace place : existingPlaces) {
                planPlaceDao.deletePlanPlaceById(place.getId());
            }
            log.info("plan_places 삭제 완료");

            List<PlanDay> existingDays = planDayDao.selectPlanDaysByPlanId(planId);
            for (PlanDay day : existingDays) {
                planDayDao.deletePlanDay(day.getId());
            }
            log.info("plan_days 삭제 완료");

            // Plan 업데이트
            Plan rollbackPlan = Plan.builder()
                    .id(planId)
                    .userId(userId)
                    .budget(snapshotContent.getBudget())
                    .startDate(LocalDate.parse(snapshotContent.getStartDate(), formatter1))
                    .endDate(LocalDate.parse(snapshotContent.getEndDate(), formatter1))
                    .createdAt(plan.getCreatedAt())
                    .updatedAt(OffsetDateTime.now())
                    .build();
            planDao.updatePlan(rollbackPlan);
            log.info("Plan 업데이트 완료");

            // PlanDay 재생성
            Map<String, Long> dateToDayId = new HashMap<>();
            for (int i = 0; i < snapshotContent.getDays().size(); i++) {
                PlanSnapshotContent.PlanDay pscDay = snapshotContent.getDays().get(i);

                PlanDay newDay = PlanDay.builder()
                        .planId(planId)
                        .dayIndex(i + 1)
                        .title(pscDay.getTitle())
                        .planDate(LocalDate.parse(pscDay.getDate(), formatter1))
                        .build();

                planDayDao.insertPlanDay(newDay);
                dateToDayId.put(pscDay.getDate(), newDay.getId());
            }
            log.info("PlanDays 재생성 완료");

            // PlanPlace 재생성
            for (PlanSnapshotContent.PlanDay pscDay : snapshotContent.getDays()) {
                Long dayId = dateToDayId.get(pscDay.getDate());

                for (PlanSnapshotContent.PlanDayItem pscItem : pscDay.getSchedules()) {
                    PlanPlace newPlace = PlanPlace.builder()
                            .dayId(dayId)
                            .title(pscItem.getTitle())
                            .startAt(LocalDateTime.parse(pscItem.getStartAt(), formatter2)
                                    .atOffset(ZoneOffset.of("+00:00")))
                            .endAt(LocalDateTime.parse(pscItem.getEndAt(), formatter2)
                                    .atOffset(ZoneOffset.of("+00:00")))
                            .placeName(pscItem.getPlaceName())
                            .address(pscItem.getAddress())
                            .lat(pscItem.getLat())
                            .lng(pscItem.getLng())
                            .expectedCost(pscItem.getExpectedCost())
                            .normalizedCategory(pscItem.getNormalizedCategory())
                            .firstImage(pscItem.getFirstImage())
                            .firstImage2(pscItem.getFirstImage2())
                            .isEnded(pscItem.getIsEnded() == null ? false : pscItem.getIsEnded())
                            .build();

                    planPlaceDao.insertPlanPlace(newPlace);
                }
            }
            log.info("PlanPlaces 재생성 완료");

            // 새 스냅샷 저장
            List<PlanDay> newDays = planDayDao.selectPlanDaysByPlanId(planId);
            List<PlanPlace> newPlaces = planPlaceDao.selectPlanPlacesByPlanId(planId);
            PlanSnapshot newSnapshot = planSnapshotService.savePlanSnapshot(rollbackPlan, newDays, newPlaces);
            Integer newVersionNo = newSnapshot.getVersionNo();

            log.info("버전 환원 완료");
            return String.format("✅ 버전 %d로 돌아갔습니다. 새 버전: %d", versionNo, newVersionNo);

        } catch (Exception e) {
            log.error("롤백 실패", e);
            return String.format("❌ 버전 환원 중 오류 발생: %s", e.getMessage());
        }
    }
}
