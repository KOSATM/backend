package com.example.demo.planner.plan.dto.request;

/**
 * LLM이 제안하는 Plan 변경 작업의 종류.
 *
 * <p>각 액션은 기존 서비스 레이어 메소드와 매핑된다:
 * <ul>
 *   <li>ADD_PLACE → PlanPlaceService.addPlace()</li>
 *   <li>DELETE_PLACE → PlanPlaceService.deletePlace()</li>
 *   <li>UPDATE_PLACE → PlanPlaceService.updatePlace()</li>
 *   <li>SWAP_PLACES → PlanPlaceService.swapPlaces()</li>
 *   <li>MOVE_PLACE → PlanPlaceService.movePlace()</li>
 * </ul>
 *
 * <p><strong>중요:</strong> 이 enum은 LLM의 의도를 표현하는 것이지,
 * DB write 권한을 LLM에게 주는 것이 아니다.
 * 모든 Delta는 시스템이 검증 후 기존 서비스를 통해 실행된다.
 */
public enum PlanDeltaAction {
    /**
     * 새로운 장소를 일정에 추가
     * 필수: dayId, place(payload), position
     * 선택: referenceSequence (position이 before/after일 때 필수)
     */
    ADD_PLACE,

    /**
     * 기존 장소를 일정에서 제거
     * 필수: placeId
     */
    DELETE_PLACE,

    /**
     * 기존 장소의 속성 변경 (시간, 메모 등)
     * 필수: placeId, changes
     */
    UPDATE_PLACE,

    /**
     * 같은 일차 내에서 두 장소의 순서 교환
     * 필수: placeId1, placeId2
     */
    SWAP_PLACES,

    /**
     * 장소를 다른 일차로 이동 또는 같은 일차 내 다른 위치로 이동
     * 필수: placeId, dayId, position
     * 선택: referenceSequence (position이 before/after일 때 필수)
     */
    MOVE_PLACE
}
