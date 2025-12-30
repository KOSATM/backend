package com.example.demo.planner.plan.agent.tools;

import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import com.example.demo.planner.plan.dao.PlanDao;
import com.example.demo.planner.plan.dao.PlanDayDao;
import com.example.demo.planner.plan.dao.PlanPlaceDao;
import com.example.demo.planner.plan.dto.entity.Plan;
import com.example.demo.planner.plan.dto.entity.PlanDay;
import com.example.demo.planner.plan.dto.entity.PlanPlace;
import com.example.demo.planner.plan.dto.entity.PendingAction;
import com.example.demo.planner.plan.dto.entity.PlanSnapshot;
import com.example.demo.planner.plan.service.PlanSnapshotService;
import com.example.demo.planner.plan.service.PendingActionService;
import com.example.demo.planner.plan.service.action.PlanDeleteAction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 날짜 관리 도구
 * - 실제 서비스 함수 호출
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DayManagementTools {

    private final PlanDeleteAction deleteAction;
    private final PlanDao planDao;
    private final PlanDayDao planDayDao;
    private final PlanPlaceDao planPlaceDao;
    private final PlanSnapshotService planSnapshotService;
    private final PendingActionService pendingActionService;

    @Tool(description = "특정 날짜를 완전히 삭제합니다")
    public String deleteDay(Long planId, Integer dayIndex) {
        log.info("🔧 [DayManagementTools] deleteDay: planId={}, dayIndex={}", planId, dayIndex);

        try {
            deleteAction.deleteDay(planId, dayIndex);

            // 스냅샷 저장 (삭제 후)
            Plan plan = planDao.selectPlanById(planId);
            List<PlanDay> planDays = planDayDao.selectPlanDaysByPlanId(planId);
            List<PlanPlace> planPlaces = planPlaceDao.selectPlanPlacesByPlanId(planId);
            PlanSnapshot newSnapshot = planSnapshotService.savePlanSnapshot(plan, planDays, planPlaces);
            Integer versionNo = newSnapshot.getVersionNo();

            return String.format("✅ %d일차 일정을 삭제했습니다. 버전: %d", dayIndex, versionNo);
        } catch (Exception e) {
            log.error("날짜 삭제 실패", e);
            return String.format("❌ 날짜 삭제 중 오류 발생: %s", e.getMessage());
        }
    }

    /**
     * Guard: 특정 날짜 삭제 사전 확인 메시지
     * ✅ 스냅샷 저장 + PendingAction 생성
     */
    public String deleteDayGuard(Long planId, Integer dayIndex) {
        // ✅ PlanContextHolder에서 planId/userId 가져오기
        Long contextPlanId = com.example.demo.planner.plan.dto.context.PlanContextHolder.getPlanId();
        Long userId = com.example.demo.planner.plan.dto.context.PlanContextHolder.getUserId();
        log.info("🔐 [DayManagementTools Guard] 사용자 확인 대기 중: planId={} (context={}), dayIndex={}", 
            planId, contextPlanId, dayIndex);

        // ✅ 삭제 전 스냅샷 저장 (Guard 단계에서)
        try {
            Plan plan = planDao.selectPlanById(contextPlanId);
            if (plan == null) {
                log.error("❌ Plan을 찾을 수 없음: planId={}", contextPlanId);
                return "Plan을 찾을 수 없습니다.";
            }
            
            List<PlanDay> planDays = planDayDao.selectPlanDaysByPlanId(contextPlanId);
            List<PlanPlace> planPlaces = planPlaceDao.selectPlanPlacesByPlanId(contextPlanId);
            PlanSnapshot beforeSnapshot = planSnapshotService.savePlanSnapshot(plan, planDays, planPlaces);
            if (beforeSnapshot == null) {
                return "스냅샷 저장에 실패했습니다. 잠시 후 다시 시도해주세요.";
            }
            log.info("🔐 삭제 전 스냅샷 저장됨: version={}", beforeSnapshot.getVersionNo());
        } catch (Exception e) {
            log.error("❌ 스냅샷 저장 실패: {}", e.getMessage());
            return "스냅샷 저장에 실패했습니다. 잠시 후 다시 시도해주세요.";
        }
        
        // ✅ PendingAction 생성 (PlanContextHolder에서 userId 조회)
        PendingAction action = PendingAction.forDeleteDay(userId, contextPlanId, dayIndex);
        pendingActionService.save(action);
        log.info("✅ [PendingAction] 생성됨: userId={}, type=DELETE_DAY, dayIndex={}", userId, dayIndex);

        return String.format(
            "%d일차를 정말로 삭제해드릴까요? ⚠️ 해당 일자의 모든 일정이 삭제됩니다. 확인해 주세요!",
            dayIndex);
    }

    /**
     * Confirm: 특정 날짜 실제 삭제
     */
    public String confirmDeleteDay(Long planId, Integer dayIndex) {
        // ✅ PlanContextHolder에서 planId 가져오기
        Long contextPlanId = com.example.demo.planner.plan.dto.context.PlanContextHolder.getPlanId();
        log.info("🛑 [DayManagementTools] 실제 삭제: planId={} (context={}), dayIndex={}", 
            planId, contextPlanId, dayIndex);

        try {
            deleteAction.deleteDay(contextPlanId, dayIndex);
            
            // 스냅샷 저장 (삭제 후)
            Plan plan = planDao.selectPlanById(contextPlanId);
            if (plan == null) {
                log.error("❌ Plan을 찾을 수 없음: planId={}", contextPlanId);
                return String.format("{\"success\": false, \"message\": \"Plan을 찾을 수 없습니다: planId=%d\"}", contextPlanId);
            }
            
            List<PlanDay> planDays = planDayDao.selectPlanDaysByPlanId(contextPlanId);
            List<PlanPlace> planPlaces = planPlaceDao.selectPlanPlacesByPlanId(contextPlanId);
            PlanSnapshot afterSnapshot = planSnapshotService.savePlanSnapshot(plan, planDays, planPlaces);
            
            return String.format(
                "{\"success\": true, \"message\": \"Day %d가 삭제되었습니다. 나머지 일정의 dayIndex가 자동으로 재정렬되었습니다. 버전: %d\", \"deletedDayIndex\": %d, \"versionNo\": %d}",
                dayIndex, afterSnapshot.getVersionNo(), dayIndex, afterSnapshot.getVersionNo());
        } catch (Exception e) {
            log.error("날짜 삭제 실패", e);
            return String.format("{\"success\": false, \"message\": \"날짜 삭제 중 오류 발생: %s\"}", e.getMessage());
        }
    }

    /**
     * Guard: 전체 일정 삭제 사전 확인 메시지
     * ✅ 스냅샷 저장 + PendingAction 생성
     */
    public String deleteEntirePlanGuard(Long planId) {
        // ✅ PlanContextHolder에서 planId/userId 가져오기
        Long contextPlanId = com.example.demo.planner.plan.dto.context.PlanContextHolder.getPlanId();
        Long userId = com.example.demo.planner.plan.dto.context.PlanContextHolder.getUserId();
        log.info("🔐 [DayManagementTools Guard] 사용자 확인 대기 중: planId={} (context={})", planId, contextPlanId);

        // ✅ 삭제 전 스냅샷 저장
        try {
            Plan plan = planDao.selectPlanById(contextPlanId);
            if (plan == null) {
                log.error("❌ Plan을 찾을 수 없음: planId={}", contextPlanId);
                return "Plan을 찾을 수 없습니다.";
            }
            
            List<PlanDay> planDays = planDayDao.selectPlanDaysByPlanId(contextPlanId);
            List<PlanPlace> planPlaces = planPlaceDao.selectPlanPlacesByPlanId(contextPlanId);
            PlanSnapshot beforeSnapshot = planSnapshotService.savePlanSnapshot(plan, planDays, planPlaces);
            if (beforeSnapshot == null) {
                return "스냅샷 저장에 실패했습니다. 잠시 후 다시 시도해주세요.";
            }
            log.info("🔐 삭제 전 스냅샷 저장됨: version={}", beforeSnapshot.getVersionNo());
        } catch (Exception e) {
            log.error("❌ 스냅샷 저장 실패: {}", e.getMessage());
            return "스냅샷 저장에 실패했습니다. 잠시 후 다시 시도해주세요.";
        }
        
        // ✅ PendingAction 생성 (PlanContextHolder에서 userId 조회)
        PendingAction action = PendingAction.forDeletePlan(userId, contextPlanId);
        pendingActionService.save(action);
        log.info("✅ [PendingAction] 생성됨: userId={}, type=DELETE_PLAN", userId);

        return "전체 여행 일정을 정말로 삭제해드릴까요? ⚠️ 복구할 수 없습니다. 확인해 주세요!";
    }

    /**
     * Confirm: 전체 일정 실제 삭제
     */
    public String confirmDeletePlan(Long planId) {
        // ✅ PlanContextHolder에서 planId 가져오기
        Long contextPlanId = com.example.demo.planner.plan.dto.context.PlanContextHolder.getPlanId();
        log.info("🛑 [DayManagementTools] 실제 삭제: planId={} (context={})", planId, contextPlanId);

        try {
            deleteAction.deleteAllDaysAndPlaces(contextPlanId);
            return String.format(
                "{\"success\": true, \"message\": \"Plan %d가 완전히 삭제되었습니다. 새로운 일정을 생성할 수 있습니다.\", \"deletedPlanId\": %d}",
                contextPlanId, contextPlanId);
        } catch (Exception e) {
            log.error("전체 일정 삭제 실패", e);
            return String.format("{\"success\": false, \"message\": \"일정 삭제 중 오류 발생: %s\"}", e.getMessage());
        }
    }

    @Tool(description = "여행 일정 전체를 완전히 삭제합니다. Plan과 모든 Day, Place가 삭제됩니다.")
    public String deleteEntirePlan(Long planId) {
        // ✅ PlanContextHolder에서 planId 가져오기
        Long contextPlanId = com.example.demo.planner.plan.dto.context.PlanContextHolder.getPlanId();
        log.info("🔧 [DayManagementTools] deleteEntirePlan: planId={} (context={})", planId, contextPlanId);

        try {
            deleteAction.deleteAllDaysAndPlaces(contextPlanId);
            return String.format(
                "{\"success\": true, \"message\": \"Plan %d가 완전히 삭제되었습니다. 새로운 일정을 생성할 수 있습니다.\", \"deletedPlanId\": %d}",
                contextPlanId, contextPlanId);
        } catch (Exception e) {
            log.error("전체 일정 삭제 실패", e);
            return String.format("{\"success\": false, \"message\": \"일정 삭제 중 오류 발생: %s\"}", e.getMessage());
        }
    }
}
