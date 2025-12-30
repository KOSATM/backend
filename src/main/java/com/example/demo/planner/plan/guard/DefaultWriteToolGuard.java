package com.example.demo.planner.plan.guard;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 기본 Write Tool Guard 구현.
 * 
 * <p><strong>현재 정책:</strong>
 * <ul>
 *   <li>위험한 작업(delete, update, add)은 사용자 확인 필요</li>
 *   <li>isConfirmed=true면 즉시 실행</li>
 *   <li>읽기 tool(search, query, get)은 항상 허용</li>
 * </ul>
 * 
 * <p><strong>확장 가능성:</strong>
 * <ul>
 *   <li>특정 tool은 무조건 허용 (add는 확인 없이)</li>
 *   <li>관리자 권한 체크</li>
 *   <li>시간대별 제한 (야간에는 삭제 불가)</li>
 *   <li>대량 작업 감지 (한 번에 10개 이상 삭제)</li>
 * </ul>
 */
@Component
@Slf4j
public class DefaultWriteToolGuard implements WriteToolGuard {

    @Override
    public GuardResult check(String toolName, GuardContext context) {
        
        // 읽기 tool은 항상 허용
        if (isReadTool(toolName)) {
            log.debug("[Guard] Read tool allowed: {}", toolName);
            return GuardResult.allow();
        }
        
        // 위험한 쓰기 tool
        if (isDangerousTool(toolName)) {
            // 이미 확인받았으면 실행
            if (context.isConfirmed()) {
                log.info("[Guard] Confirmed dangerous tool execution: {}", toolName);
                return GuardResult.allow();
            }
            
            // 확인 필요
            String question = buildConfirmationQuestion(toolName);
            log.info("[Guard] Asking confirmation for: {}", toolName);
            return GuardResult.askConfirmation(question);
        }
        
        // 기본: 허용
        log.debug("[Guard] Tool allowed by default: {}", toolName);
        return GuardResult.allow();
    }
    
    /**
     * 읽기 전용 tool인지 판단
     */
    private boolean isReadTool(String toolName) {
        return toolName.startsWith("search")
            || toolName.startsWith("query")
            || toolName.startsWith("get")
            || toolName.startsWith("find")
            || toolName.startsWith("list");
    }
    
    /**
     * 위험한 쓰기 tool인지 판단
     */
    private boolean isDangerousTool(String toolName) {
        return toolName.startsWith("delete")
            || toolName.startsWith("remove")
            || toolName.startsWith("update")
            || toolName.startsWith("modify")
            || toolName.startsWith("add")
            || toolName.startsWith("create");
    }
    
    /**
     * tool별 확인 질문 생성
     */
    private String buildConfirmationQuestion(String toolName) {
        if (toolName.startsWith("delete") || toolName.startsWith("remove")) {
            return "정말 삭제할까요? (삭제 후 복구가 어려울 수 있습니다)";
        }
        
        if (toolName.startsWith("update") || toolName.startsWith("modify")) {
            return "정말 수정할까요?";
        }
        
        if (toolName.startsWith("add") || toolName.startsWith("create")) {
            return "정말 추가할까요?";
        }
        
        return "이 작업을 진행할까요?";
    }
}
