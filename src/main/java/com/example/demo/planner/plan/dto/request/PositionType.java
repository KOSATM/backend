package com.example.demo.planner.plan.dto.request;

/**
 * 장소를 추가/이동할 때 위치를 지정하는 방식.
 * 
 * <p><strong>규칙:</strong>
 * <ul>
 *   <li>START, END → referenceSequence 무시</li>
 *   <li>BEFORE, AFTER → referenceSequence 필수</li>
 * </ul>
 * 
 * <p>예시:
 * <pre>
 * position: START, referenceSequence: null → 일차의 맨 앞
 * position: AFTER, referenceSequence: 2 → sequence=2인 장소 뒤
 * position: END, referenceSequence: null → 일차의 맨 뒤
 * </pre>
 */
public enum PositionType {
    /**
     * 일차의 맨 앞에 배치
     */
    START,
    
    /**
     * 특정 장소 앞에 배치 (referenceSequence 필수)
     */
    BEFORE,
    
    /**
     * 특정 장소 뒤에 배치 (referenceSequence 필수)
     */
    AFTER,
    
    /**
     * 일차의 맨 뒤에 배치
     */
    END
}
