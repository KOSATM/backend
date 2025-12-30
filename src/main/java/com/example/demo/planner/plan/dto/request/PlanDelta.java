package com.example.demo.planner.plan.dto.request;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * LLM이 제안하는 Plan 변경 의도를 표현하는 불변 Delta 객체.
 * 
 * <p><strong>핵심 설계 원칙:</strong>
 * <ul>
 *   <li>LLM은 Delta를 생성한다 (의사결정)</li>
 *   <li>시스템은 Delta를 검증하고 기존 서비스로 실행한다 (실행)</li>
 *   <li>LLM은 DB에 직접 쓰지 않는다</li>
 * </ul>
 * 
 * <p><strong>dayId vs dayIndex 규칙:</strong>
 * dayId를 사용한다. dayIndex는 정렬 결과에 따라 변할 수 있지만,
 * dayId는 영구 불변이므로 안전하다.
 * 
 * <p><strong>필드 사용 규칙 (action별):</strong>
 * <ul>
 *   <li>ADD_PLACE: planId, dayId, place, position, [referenceSequence]</li>
 *   <li>DELETE_PLACE: planId, placeId</li>
 *   <li>UPDATE_PLACE: planId, placeId, changes</li>
 *   <li>SWAP_PLACES: planId, placeId1, placeId2</li>
 *   <li>MOVE_PLACE: planId, placeId, dayId, position, [referenceSequence]</li>
 * </ul>
 * 
 * <p><strong>변경 가능한 필드 (changes 맵):</strong>
 * <pre>
 * {
 *   "startTime": "10:00",
 *   "endTime": "12:00",
 *   "memo": "점심 식사",
 *   "expectedCost": 15000
 * }
 * </pre>
 * 
 * @see PlanDeltaAction
 * @see PlacePayload
 * @see PositionType
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanDelta {
    /**
     * 변경 작업 종류
     */
    private PlanDeltaAction action;
    
    /**
     * 대상 Plan ID (모든 action에 필수)
     */
    private Long planId;
    
    // ===== 타겟 식별 =====
    
    /**
     * 대상 일차 ID
     * 사용: ADD_PLACE, MOVE_PLACE
     * 
     * <strong>중요:</strong> dayIndex가 아닌 dayId를 사용한다.
     */
    private Long dayId;
    
    /**
     * 대상 장소 ID (단일)
     * 사용: DELETE_PLACE, UPDATE_PLACE, MOVE_PLACE
     */
    private Long placeId;
    
    /**
     * 대상 장소 ID 1 (교환용)
     * 사용: SWAP_PLACES
     */
    private Long placeId1;
    
    /**
     * 대상 장소 ID 2 (교환용)
     * 사용: SWAP_PLACES
     */
    private Long placeId2;
    
    // ===== 페이로드 =====
    
    /**
     * 추가할 장소 정보
     * 사용: ADD_PLACE
     */
    private PlacePayload place;
    
    /**
     * 변경할 필드와 값
     * 사용: UPDATE_PLACE
     * 
     * 예: {"startTime": "10:00", "memo": "점심"}
     */
    private Map<String, Object> changes;
    
    // ===== 위치 지정 =====
    
    /**
     * 배치 위치 유형
     * 사용: ADD_PLACE, MOVE_PLACE
     * 
     * <strong>규칙:</strong>
     * - START/END → referenceSequence 무시
     * - BEFORE/AFTER → referenceSequence 필수
     */
    private PositionType position;
    
    /**
     * 기준이 되는 장소의 sequence 값
     * 사용: ADD_PLACE, MOVE_PLACE (position이 BEFORE/AFTER일 때만)
     * 
     * 예: referenceSequence=2 → sequence가 2인 장소를 기준으로 배치
     */
    private Integer referenceSequence;
}
