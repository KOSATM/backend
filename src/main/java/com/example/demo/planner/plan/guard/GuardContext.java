package com.example.demo.planner.plan.guard;

import lombok.Builder;
import lombok.Data;

/**
 * WriteToolGuard가 판단할 때 필요한 컨텍스트.
 *
 * <p>포함 정보:
 * <ul>
 *   <li>userId, planId - 권한 체크</li>
 *   <li>isConfirmed - 사용자가 이미 확인했는지 여부</li>
 *   <li>previousQuestion - 이전 턴에 물었던 질문 (확인 매칭용)</li>
 * </ul>
 *
 * <p><strong>isConfirmed 사용 시나리오:</strong>
 * <pre>
 * Turn 1: delete tool call → Guard: "정말 삭제할까요?"
 * Turn 2: User: "응" → isConfirmed=true → 실행
 * </pre>
 */
@Data
@Builder
public class GuardContext {
    private Long userId;
    private Long planId;

    /**
     * 사용자가 이전 턴에서 확인 응답을 했는지 여부
     */
    private boolean isConfirmed;

    /**
     * 이전 턴에서 물었던 질문 (확인 매칭용)
     */
    private String previousQuestion;
}
