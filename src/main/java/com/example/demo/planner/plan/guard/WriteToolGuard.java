package com.example.demo.planner.plan.guard;

/**
 * Write Tool 실행 직전의 공통 가드(Guard) 인터페이스.
 * 
 * <p><strong>핵심 설계 원칙:</strong>
 * <ul>
 *   <li>Agent 로직과 분리: SmartPlanAgent는 판단만, Guard는 관문만</li>
 *   <li>LLM 자유도 유지: LLM은 tool call을 자유롭게 생성</li>
 *   <li>실행 시점 차단: DB write 직전 단 1지점에서만 검증</li>
 * </ul>
 * 
 * <p><strong>처리 흐름:</strong>
 * <pre>
 * LLM → Tool 메서드 호출
 *   ↓
 * [ WriteToolGuard.check() ] ← 여기서 차단
 *   ↓
 * 실제 DB 작업
 *   ↓
 * Service / DB
 * </pre>
 * 
 * <p><strong>구현 예시:</strong>
 * <ul>
 *   <li>DefaultWriteToolGuard: delete/update는 확인 필요</li>
 *   <li>AdminOnlyGuard: 대량 수정은 관리자만</li>
 *   <li>UndoableGuard: 위험한 작업은 Undo 스택에 저장</li>
 * </ul>
 */
public interface WriteToolGuard {
    
    /**
     * Tool 실행 전 검증.
     * 
     * @param toolName 실행하려는 tool 이름 (예: "deletePlaceFromPlan")
     * @param context 판단에 필요한 컨텍스트 (userId, isConfirmed 등)
     * @return 실행 허용 여부 + 차단 시 질문
     */
    GuardResult check(String toolName, GuardContext context);
}
