package com.example.demo.planner.plan.guard;

/**
 * WriteToolGuard가 Tool 실행을 허용할지 판단한 결과.
 *
 * <p><strong>설계 철학:</strong>
 * Guard는 LLM의 판단을 "부정"하지 않는다. 단지 "보류"할 뿐이다.
 *
 * <p>예시:
 * <ul>
 *   <li>allowed=true, question=null → 즉시 실행</li>
 *   <li>allowed=false, question="정말 삭제할까요?" → 사용자 확인 필요</li>
 * </ul>
 *
 * @param allowed Tool 실행 허용 여부
 * @param question allowed=false일 때 사용자에게 물을 질문
 */
public record GuardResult(
    boolean allowed,
    String question
) {
    /**
     * 즉시 실행 허용
     */
    public static GuardResult allow() {
        return new GuardResult(true, null);
    }

    /**
     * 사용자 확인 필요
     */
    public static GuardResult askConfirmation(String question) {
        return new GuardResult(false, question);
    }
}
