package com.example.demo.planner.plan.agent.tools;

import java.time.format.DateTimeFormatter;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.planner.plan.agent.common.PlanToolSupport;
import com.example.demo.planner.plan.dao.PlanSnapshotDao;
import com.example.demo.planner.plan.dto.entity.PlanSnapshot;
import com.example.demo.planner.plan.dto.response.PlanSnapshotContent;
import com.example.demo.planner.plan.service.PlanSnapshotService;
import com.example.demo.planner.plan.service.PlanSnapshotUtility;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 버전 관리 관련 Tool
 * - 버전 조회
 * - 버전 복구 (rollback)
 * - 버전 미리보기
 */
@Component("planVersionTools")
@RequiredArgsConstructor
@Slf4j
public class PlanVersionTools {

    private final PlanToolSupport support;
    private final PlanSnapshotDao planSnapshotDao;
    private final PlanSnapshotService planSnapshotService;
    private final PlanSnapshotUtility planSnapshotUtility;

    DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ========== 버전 조회 ==========
    
    @Tool(description = """
            현재 일정의 버전 번호를 조회합니다.
            
            사용 예:
            - "현재 버전 몇이야?"
            - "지금 버전 알려줘"
            """)
    public String getVersionNumber(ToolContext toolContext) {
        Long userId = (Long) toolContext.getContext().get("userId");
        int currentVersionNo = planSnapshotService.getLatestVersionNo(userId);
        return "현재 일정은 " + currentVersionNo + "버전입니다.";
    }
    
    @Tool(description = """
            특정 버전의 일정을 간략하게 미리 봅니다.
            
            사용 예:
            - "버전 3 어떤 일정이었는지 보여줘"
            - "5번 버전 확인해줘"
            - "이전 버전 뭐였는지 보여줘"
            
            주의:
            - 되돌리기 전에 미리 확인할 때 사용하세요
            - 각 날짜당 최대 3개 장소만 표시됩니다
            """)
    public String viewSnapshotVersion(
            @ToolParam(description = "조회할 버전 번호") int versionNo,
            ToolContext toolContext) {
        
        try {
            Long userId = (Long) toolContext.getContext().get("userId");
            
            return planSnapshotService.renderSnapshotSummary(versionNo, userId);
            
        } catch (Exception e) {
            log.error("버전 조회 실패", e);
            return String.format("버전 %d 조회 중 오류: %s", versionNo, e.getMessage());
        }
    }
    
    @Tool(description = """
            저장된 모든 버전의 목록을 간략하게 보여줍니다.
            
            사용 예:
            - "지금까지 저장된 버전들 보여줘"
            - "버전 목록 알려줘"
            - "히스토리 확인해줘"
            
            각 버전별로:
            - 버전 번호
            - 여행 기간
            - 일차별 장소 개수
            를 표시합니다.
            """)
    public String listAllVersions(ToolContext toolContext) {
        try {
            Long userId = (Long) toolContext.getContext().get("userId");
            
            return planSnapshotService.listAllVersionsSummary(userId);
            
        } catch (Exception e) {
            log.error("버전 목록 조회 실패", e);
            return "버전 목록 조회 중 오류: " + e.getMessage();
        }
    }

    // ========== 버전 복구 ==========
    
    @Transactional
    @Tool(description = """
            바로 이전 버전으로 일정을 되돌립니다.
            
            사용 예:
            - "이전 버전으로 되돌려줘"
            - "일정을 이전으로 돌려줘"
            
            주의:
            - 되돌리기 전 사용자에게 한번 더 물어보세요
            - viewSnapshotVersion으로 먼저 확인하는 것을 권장합니다
            """)
    public String rollBack(ToolContext toolContext) {
        try {
            Long userId = (Long) toolContext.getContext().get("userId");
            int versionNo = planSnapshotService.getLatestVersionNo(userId);
            
            if (1 == versionNo) {
                return "일정 버전이 1이기 때문에 이전 버전으로 돌아갈 수 없습니다.";
            }

            // 1. 이전 버전 스냅샷 조회
            PlanSnapshot planSnapshot = planSnapshotService.getPlanSnapshotByVersionNo(
                    versionNo - 1, userId);
            PlanSnapshotContent snapshotContent = planSnapshotUtility.parseSnapshot(
                    planSnapshot.getSnapshotJson());

            // 2. 현재 planId
            String conversationId = getConversationId(toolContext);
            Long planId = support.getPlanId(conversationId);

            // 3. 복원
            planSnapshotService.restorePlanFromSnapshot(planId, snapshotContent, userId);

            // 4. 새 스냅샷 저장
            Integer newVersionNo = support.saveSnapshot(planId);

            return String.format("이전 버전으로 돌아갔습니다. 버전: %d", newVersionNo);

        } catch (Exception e) {
            log.error("버전 환원 실패", e);
            return String.format("버전 환원 중 오류 발생: %s", e.getMessage());
        }
    }
    
    @Transactional
    @Tool(description = """
            지정한 버전으로 일정을 되돌립니다.
            
            사용 예:
            - "버전 3으로 돌아가줘"
            - "5번 버전으로 복구해줘"
            
            주의:
            - 되돌리기 전 사용자에게 한번 더 물어보세요
            - viewSnapshotVersion으로 먼저 확인하는 것을 권장합니다
            """)
    public String rollBackToSpecific(
            @ToolParam(description = "돌아가고자 하는 버전 번호") Integer versionNo,
            ToolContext toolContext) {

        try {
            Long userId = (Long) toolContext.getContext().get("userId");
            int currentVersionNo = planSnapshotService.getLatestVersionNo(userId);

            log.info("돌아갈 버전: {}", versionNo);

            // 1. Validation
            if (versionNo == currentVersionNo) {
                return "현재 버전과 같은 버전이기 때문에 돌아갈 수 없습니다.";
            }

            // 2. 스냅샷 조회
            PlanSnapshot toRevert = PlanSnapshot.builder()
                    .userId(userId)
                    .versionNo(versionNo)
                    .build();

            PlanSnapshot planSnapshot = planSnapshotDao.selectPlanSnapshotByUserIdAndVersionNo(toRevert);
            if (planSnapshot == null) {
                return String.format("버전 %d를 찾을 수 없습니다.", versionNo);
            }
            
            PlanSnapshotContent snapshotContent = planSnapshotUtility.parseSnapshot(
                    planSnapshot.getSnapshotJson());

            // 3. 현재 planId
            String conversationId = getConversationId(toolContext);
            Long planId = support.getPlanId(conversationId);

            // 4. 복원
            planSnapshotService.restorePlanFromSnapshot(planId, snapshotContent, userId);

            // 5. 새 스냅샷 저장
            Integer newVersionNo = support.saveSnapshot(planId);

            return String.format("버전 %d로 돌아갔습니다. 새 버전: %d", versionNo, newVersionNo);

        } catch (Exception e) {
            log.error("버전 환원 실패", e);
            return String.format("버전 환원 중 오류 발생: %s", e.getMessage());
        }
    }

    // ========== Helper ==========
    
    private String getConversationId(ToolContext toolContext) {
        Object v = toolContext.getContext().get("conversationId");
        if (v == null) {
            throw new IllegalStateException("conversationId가 ToolContext에 없습니다.");
        }
        return String.valueOf(v);
    }
}