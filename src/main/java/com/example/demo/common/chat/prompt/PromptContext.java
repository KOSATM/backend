package com.example.demo.common.chat.prompt;

import com.example.demo.common.chat.dto.MemoryBundle;
import com.example.demo.common.chat.intent.IntentType;
import com.example.demo.planner.plan.dto.context.PlanContext;

import lombok.Builder;
import lombok.Data;

/**
 * 프롬프트를 만들기 위한 재료를 담는 컨텍스트 객체
 *
 * 필수 정보:
 * - userId: 사용자 ID
 * - userMessage: 사용자 메시지
 *
 * 선택적 정보:
 * - planContext: 여행 계획 컨텍스트
 * - memoryBundle: 대화 메모리 번들
 * - intentType: 의도 타입
 *
 * PendingAction 필드는 의도적으로 제외:
 *
 * 이유:
 * 1. chatMemory가 충분한 컨텍스트 제공
 *    - "국밥집 3개 중 2번" → memory에 후보 목록 있음
 *    - "응", "그래" → memory에 이전 질문 있음
 *
 * 2. 대부분의 작업이 2턴 이내 완료
 *    - 탐색 → 확정
 *    - 질문 → 응답
 *
 * 3. LLM이 자연스럽게 확인 프로세스 수행
 *    - 명시적 상태 관리보다 대화 흐름 우선
 *
 * 4. PendingAction이 필요한 경우는 별도 처리
 *    - 위험한 작업 (전체 삭제, 큰 변경)
 *    - 복잡한 다단계 작업 (5턴 이상)
 *    - 명시적 트랜잭션 필요 (결제, 예약)
 *
 * TODO: 위 4번 케이스가 발생하면 PendingAction 필드 추가
 */
@Data
@Builder
public class PromptContext {
    /**
     * 사용자 ID (필수)
     */
    private Long userId;

    /**
     * 사용자 메시지 (필수)
     */
    private String userMessage;

    // ===== Optional Fields =====

    /**
     * 여행 계획 컨텍스트 (선택)
     */
    private PlanContext planContext;

    /**
     * 대화 메모리 번들 (선택)
     */
    private MemoryBundle memoryBundle;

    /**
     * 의도 타입 (선택)
     */
    private IntentType intentType;
}
