package com.example.demo.planner.plan.agent.tools;

import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import com.example.demo.planner.plan.dao.PlanDao;
import com.example.demo.planner.plan.dao.PlanDayDao;
import com.example.demo.planner.plan.dao.PlanPlaceDao;
import com.example.demo.planner.plan.dto.context.PlanContextHolder;
import com.example.demo.planner.plan.dto.entity.PendingAction;
import com.example.demo.planner.plan.dto.entity.Plan;
import com.example.demo.planner.plan.dto.entity.PlanDay;
import com.example.demo.planner.plan.dto.entity.PlanPlace;
import com.example.demo.planner.plan.dto.entity.PlanSnapshot;
import com.example.demo.planner.plan.guard.WriteToolGuard;
import com.example.demo.planner.plan.service.PendingActionService;
import com.example.demo.planner.plan.service.PlanSnapshotService;
import com.example.demo.planner.plan.service.action.PlanAddAction;
import com.example.demo.planner.plan.service.action.PlanDeleteAction;
import com.example.demo.planner.plan.service.action.PlanModifyAction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 장소 관리 도구 모음
 * - PlaceManagementAgent가 사용
 *
 * <p><strong>WriteToolGuard 통합:</strong>
 * 모든 쓰기 tool은 실제 DB 작업 직전에 Guard를 통과해야 한다.
 * Guard가 막으면 → 확인 질문 반환 → 다음 턴에서 재실행
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PlaceManagementTools {

    private final PlanAddAction addAction;
    private final PlanDeleteAction deleteAction;
    private final PlanModifyAction modifyAction;
    private final PlanDao planDao;
    private final PlanDayDao planDayDao;
    private final PlanPlaceDao planPlaceDao;
    private final PlanSnapshotService planSnapshotService;
    private final PendingActionService pendingActionService;
    private final WriteToolGuard writeToolGuard;

    // @Tool(description = "네이버에서 장소를 검색하고 첫 번째 결과를 일정에 자동으로 추가합니다")
    // public String searchAndAddPlace(Long planId, String placeName, Integer dayIndex, String startTime) {
    //     log.info("🔧 [Tool] searchAndAddPlace: planId={}, place={}, day={}, time={}",
    //             planId, placeName, dayIndex, startTime);
    //     try {
    //         // PlanAddAction.addPlace는 이미 네이버 검색 + 첫 번째 결과 추가를 수행함
    //         String actualName = addAction.addPlace(planId, dayIndex, placeName, startTime);

    //         // 스냅샷 저장
    //         saveSnapshot(planId);

    //         return String.format("✅ '%s'을(를) %d일차 일정에 추가했습니다.", actualName, dayIndex);
    //     } catch (Exception e) {
    //         log.error("❌ 장소 추가 실패", e);
    //         return String.format("❌ 장소 추가 실패: %s", e.getMessage());
    //     }
    // }

    // @Tool(description = "이미 선택된 장소를 일정에 추가합니다 (검색 없이 바로 추가)")
    // public String confirmAddPlace(Long planId, String placeName, Integer dayIndex, String startTime) {
    //     log.info("🔧 [Tool] confirmAddPlace: planId={}, place={}, day={}, time={}",
    //             planId, placeName, dayIndex, startTime);
    //     try {
    //         // 검색 없이 바로 추가 (이미 사용자가 선택한 장소)
    //         addAction.addPlace(planId, dayIndex, placeName, startTime);

    //         // 스냅샷 저장
    //         saveSnapshot(planId);

    //         return String.format("✅ '%s'을(를) %d일차 일정에 추가했습니다.", placeName, dayIndex);
    //     } catch (Exception e) {
    //         log.error("❌ 장소 추가 실패", e);
    //         return String.format("❌ 장소 추가 실패: %s", e.getMessage());
    //     }
    // }
        // 🚨 Guard 체크 (실행 관문)
        // TODO: chatMemory에서 isConfirmed 조회하여 Guard에 전달
        // 현재는 개념 증명을 위해 주석 처리
        //
        // GuardContext context = GuardContext.builder()
        //     .planId(planId)
        //     .isConfirmed(false) // TODO: chatMemory에서 확인 여부 조회
        //     .build();
        //
        // GuardResult result = writeToolGuard.check("deletePlaceFromPlan", context);
        //
        // if (!result.allowed()) {
        //     return result.question(); // 확인 질문 반환
        // }



    @Tool(description = "일정에서 장소를 삭제합니다")
    public String deletePlaceFromPlan(Long planId, String placeName) {
        log.info("🔧 [Tool] deletePlaceFromPlan: planId={}, place={}", planId, placeName);
        try {
            deleteAction.deletePlaceByName(planId, placeName);

            // 스냅샷 저장
            saveSnapshot(planId);

            return String.format("✅ '%s'을(를) 일정에서 삭제했습니다.", placeName);
        } catch (IllegalArgumentException e) {
            return String.format("❌ '%s'을(를) 찾을 수 없습니다.", placeName);
        } catch (Exception e) {
            log.error("❌ 장소 삭제 실패", e);
            return String.format("❌ 장소 삭제 실패: %s", e.getMessage());
        }
    }

    /**
     * Guard: 장소 삭제 사전 확인 메시지
     * ✅ 스냅샷 저장 + PendingAction 생성
     */
    public String deletePlaceGuard(Long planId, String placeName) {
        log.info("🔐 [PlaceManagementTools Guard] 사용자 확인 대기 중: planId={}, place={}", planId, placeName);

        // ✅ 삭제 전 스냅샷 저장 (Guard 단계에서)
        // 실패하면 바로 반환
        if (!saveSnapshot(planId)) {
            return String.format("{\"success\": false, \"message\": \"스냅샷 저장에 실패했습니다. 잠시 후 다시 시도해주세요.\"}");
        }

        // ✅ PendingAction 생성 (PlanContextHolder에서 userId 조회)
        Long userId = PlanContextHolder.getUserId();
        PendingAction action = PendingAction.forDeletePlace(userId, planId, placeName);
        pendingActionService.save(action);
        log.info("✅ [PendingAction] 생성됨: userId={}, type=DELETE_PLACE, placeName={}", userId, placeName);

        return String.format(
            "'%s'을(를) 정말로 삭제해드릴까요? ⚠️ 삭제 시 일정 시간이 자동으로 조정됩니다. 확인해 주세요!",
            placeName);
    }

    /**
     * Confirm: 장소 실제 삭제
     */
    public String confirmDeletePlace(Long planId, String placeName) {
        log.info("🛑 [PlaceManagementTools] 실제 삭제: planId={}, place={}", planId, placeName);

        try {
            deleteAction.deletePlaceByName(planId, placeName);

            // 스냅샷 저장 (삭제 후)
            Plan plan = planDao.selectPlanById(planId);
            List<PlanDay> planDays = planDayDao.selectPlanDaysByPlanId(planId);
            List<PlanPlace> planPlaces = planPlaceDao.selectPlanPlacesByPlanId(planId);
            PlanSnapshot afterSnapshot = planSnapshotService.savePlanSnapshot(plan, planDays, planPlaces);

            return String.format(
                "{\"success\": true, \"message\": \"'%s'이(가) 삭제되었습니다. 버전: %d\", \"deletedPlace\": \"%s\", \"versionNo\": %d}",
                placeName, afterSnapshot.getVersionNo(), placeName, afterSnapshot.getVersionNo());
        } catch (IllegalArgumentException e) {
            return String.format("{\"success\": false, \"message\": \"'%s'을(를) 찾을 수 없습니다.\"}", placeName);
        } catch (Exception e) {
            log.error("❌ 장소 삭제 실패", e);
            return String.format("{\"success\": false, \"message\": \"장소 삭제 중 오류 발생: %s\"}", e.getMessage());
        }
    }

    /**
     * Guard: 위치 기반 장소 삭제 전 확인
     * ✅ 스냅샷 저장 + PendingAction 생성
     */
    public String deletePlaceByPositionGuard(Long planId, Integer dayIndex, Integer position) {
        log.info("🔐 [PlaceManagementTools Guard] 위치 기반 삭제 확인: planId={}, day={}, pos={}", planId, dayIndex, position);

        // 삭제할 장소 정보 먼저 조회
        try {
            PlanDay targetDay = planDayDao.selectPlanDayByPlanIdAndDayIndex(planId, dayIndex);
            if (targetDay == null) {
                return String.format("Day %d를 찾을 수 없습니다.", dayIndex);
            }

            List<PlanPlace> places = planPlaceDao.selectPlanPlacesByPlanDayId(targetDay.getId());
            if (position < 1 || position > places.size()) {
                return String.format("Day %d에는 %d개의 일정만 있습니다.", dayIndex, places.size());
            }

            PlanPlace targetPlace = places.get(position - 1);

            // ✅ 삭제 전 스냅샷 저장 (실패하면 바로 반환)
            if (!saveSnapshot(planId)) {
                return "스냅샷 저장에 실패했습니다. 잠시 후 다시 시도해주세요.";
            }

            // ✅ PendingAction 생성 (PlanContextHolder에서 userId 조회)
            Long userId = PlanContextHolder.getUserId();
            PendingAction action = PendingAction.forDeletePlace(userId, planId, targetPlace.getTitle(), dayIndex, position);
            pendingActionService.save(action);
            log.info("✅ [PendingAction] 생성됨: userId={}, type=DELETE_PLACE, placeName={}, dayIndex={}, position={}",
                userId, targetPlace.getTitle(), dayIndex, position);

            return String.format(
                "Day %d의 %d번째 일정 '%s'을(를) 정말로 삭제해드릴까요? ⚠️ 일정 시간이 자동으로 조정됩니다. 확인해 주시면 삭제를 진행하겠습니다!",
                dayIndex, position, targetPlace.getTitle());
        } catch (Exception e) {
            log.error("❌ 장소 조회 실패", e);
            return String.format("장소 조회 중 오류 발생: %s", e.getMessage());
        }
    }

    /**
     * Confirm: 위치 기반 장소 실제 삭제
     */
    public String confirmDeletePlaceByPosition(Long planId, Integer dayIndex, Integer position) {
        log.info("🛑 [PlaceManagementTools] 위치 기반 실제 삭제: planId={}, day={}, pos={}", planId, dayIndex, position);

        try {
            deleteAction.deletePlaceByPosition(planId, dayIndex, position);

            // 스냅샷 저장 (삭제 후)
            Plan plan = planDao.selectPlanById(planId);
            List<PlanDay> planDays = planDayDao.selectPlanDaysByPlanId(planId);
            List<PlanPlace> planPlaces = planPlaceDao.selectPlanPlacesByPlanId(planId);
            PlanSnapshot afterSnapshot = planSnapshotService.savePlanSnapshot(plan, planDays, planPlaces);

            return String.format(
                "{\"success\": true, \"message\": \"Day %d의 %d번째 일정이 삭제되었습니다. 버전: %d\", \"deletedDayIndex\": %d, \"deletedPosition\": %d, \"versionNo\": %d}",
                dayIndex, position, afterSnapshot.getVersionNo(), dayIndex, position, afterSnapshot.getVersionNo());
        } catch (Exception e) {
            log.error("❌ 위치 기반 장소 삭제 실패", e);
            return String.format("{\"success\": false, \"message\": \"장소 삭제 중 오류 발생: %s\"}", e.getMessage());
        }
    }

    @Tool(description = "기존 장소를 새로운 장소로 교체합니다 (검색 후 첫 번째 결과 사용)")
    public String replacePlaceInPlan(Long planId, String oldPlaceName, String newPlaceName) {
        log.info("🔧 [Tool] replacePlaceInPlan: planId={}, old={}, new={}", planId, oldPlaceName, newPlaceName);
        try {
            String actualName = modifyAction.replacePlaceWithSearch(planId, oldPlaceName, newPlaceName);

            // 스냅샷 저장
            saveSnapshot(planId);

            return String.format("✅ '%s'를 '%s'로 변경했습니다.", oldPlaceName, actualName);
        } catch (Exception e) {
            log.error("❌ 장소 교체 실패", e);
            return String.format("❌ 장소 교체 실패: %s", e.getMessage());
        }
    }

    @Tool(description = "검색 결과에서 선택한 장소로 교체합니다")
    public String replacePlaceWithSelection(Long planId, String oldPlaceName, String newPlaceName, Integer selectedIndex) {
        log.info("🔧 [Tool] replacePlaceWithSelection: planId={}, old={}, new={}, index={}",
                planId, oldPlaceName, newPlaceName, selectedIndex);
        try {
            String actualName = modifyAction.replacePlaceWithSelection(planId, oldPlaceName, newPlaceName, selectedIndex);

            // 스냅샷 저장
            saveSnapshot(planId);

            return String.format("✅ '%s'를 '%s'로 변경했습니다.", oldPlaceName, actualName);
        } catch (Exception e) {
            log.error("❌ 장소 교체 실패", e);
            return String.format("❌ 장소 교체 실패: %s", e.getMessage());
        }
    }

    @Tool(description = "특정 위치에 장소를 삽입하고 이후 일정을 자동 조정합니다")
    public String addPlaceAtPosition(Long planId, Integer dayIndex, Integer position, String placeName, Integer duration) {
        log.info("🔧 [Tool] addPlaceAtPosition: planId={}, day={}, pos={}, place={}, duration={}",
                planId, dayIndex, position, placeName, duration);
        try {
            String actualName = addAction.addPlaceAtPosition(planId, dayIndex, position, placeName, duration);

            // 스냅샷 저장
            saveSnapshot(planId);

            return String.format("✅ %d일차 %d번째에 '%s'을(를) 추가했습니다.", dayIndex, position, actualName);
        } catch (Exception e) {
            log.error("❌ 장소 삽입 실패", e);
            return String.format("❌ 장소 삽입 실패: %s", e.getMessage());
        }
    }

    /**
     * ✅ 스냅샷 저장 (boolean 반환으로 명시적 실패 처리)
     * Guard 단계에서 실패하면 바로 fail 반환해야 함
     */
    private boolean saveSnapshot(Long planId) {
        try {
            // ✅ PlanContextHolder에서 planId 가져오기 (Tool 내에서 새로 조회하지 않음)
            Long contextPlanId = com.example.demo.planner.plan.dto.context.PlanContextHolder.getPlanId();
            if (!contextPlanId.equals(planId)) {
                log.warn("⚠️ planId 불일치: request={}, context={}", planId, contextPlanId);
                planId = contextPlanId;
            }

            Plan plan = planDao.selectPlanById(planId);
            if (plan == null) {
                log.error("❌ Plan을 찾을 수 없음: planId={}", planId);
                return false;
            }

            List<PlanDay> planDays = planDayDao.selectPlanDaysByPlanId(planId);
            List<PlanPlace> planPlaces = planPlaceDao.selectPlanPlacesByPlanId(planId);
            planSnapshotService.savePlanSnapshot(plan, planDays, planPlaces);
            log.info("✅ 스냅샷 저장 완료: planId={}", planId);
            return true;
        } catch (Exception e) {
            log.error("❌ 스냅샷 저장 실패", e);
            return false;
        }
    }

    /**
     * Guard: 장소 시간 수정 전 확인
     * ✅ 스냅샷 저장 + PendingAction 생성
     */
    public String updatePlaceTimeGuard(Long planId, String placeName, Integer dayIndex, String newStartTime) {
        log.info("🔐 [PlaceManagementTools Guard] 장소 시간 수정 확인: planId={}, place={}, day={}, newTime={}",
                planId, placeName, dayIndex, newStartTime);

        // ✅ 수정 전 스냅샷 저장 (실패하면 바로 반환)
        if (!saveSnapshot(planId)) {
            return String.format("{\"success\": false, \"message\": \"스냅샷 저장에 실패했습니다. 잠시 후 다시 시도해주세요.\"}");
        }

        // ✅ PendingAction 생성
        Long userId = PlanContextHolder.getUserId();
        PendingAction action = PendingAction.forUpdatePlace(userId, planId, placeName, dayIndex, newStartTime);
        pendingActionService.save(action);
        log.info("✅ [PendingAction] 생성됨: userId={}, type=UPDATE_PLACE, placeName={}, newTime={}", userId, placeName, newStartTime);

        return String.format(
            "'%s'의 시간을 %s로 변경해드릴까요? ⚠️ 이후 일정 시간이 자동으로 조정됩니다.",
            placeName, newStartTime);
    }

    /**
     * Confirm: 장소 시간 실제 수정
     */
    public String confirmUpdatePlaceTime(Long planId, String placeName, String newStartTime) {
        log.info("🛑 [PlaceManagementTools] 실제 시간 수정: planId={}, place={}, newTime={}", planId, placeName, newStartTime);

        try {
            modifyAction.updatePlaceTime(planId, placeName, newStartTime);

            // 스냅샷 저장 (수정 후)
            Plan plan = planDao.selectPlanById(planId);
            List<PlanDay> planDays = planDayDao.selectPlanDaysByPlanId(planId);
            List<PlanPlace> planPlaces = planPlaceDao.selectPlanPlacesByPlanId(planId);
            PlanSnapshot afterSnapshot = planSnapshotService.savePlanSnapshot(plan, planDays, planPlaces);

            return String.format(
                "{\"success\": true, \"message\": \"'%s'의 시간이 %s로 변경되었습니다. 버전: %d\", \"updatedPlace\": \"%s\", \"newStartTime\": \"%s\", \"versionNo\": %d}",
                placeName, newStartTime, afterSnapshot.getVersionNo(), placeName, newStartTime, afterSnapshot.getVersionNo());
        } catch (IllegalArgumentException e) {
            return String.format("{\"success\": false, \"message\": \"'%s'을(를) 찾을 수 없습니다.\"}", placeName);
        } catch (Exception e) {
            log.error("❌ 장소 시간 수정 실패", e);
            return String.format("{\"success\": false, \"message\": \"시간 수정 중 오류 발생: %s\"}", e.getMessage());
        }
    }
}
