package com.example.demo.planner.plan.dto.entity;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 사용자 확인 대기 중인 작업
 *
 * 설계:
 * - 사용자별 1개만 유지 (동시 Guard 방지)
 * - LLM이 결정하지 않음 (시스템 관리)
 * - TTL: 5분 (자동 만료)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingAction {

    public enum ActionType {
        DELETE_PLACE,
        DELETE_DAY,
        DELETE_PLAN,
        UPDATE_PLACE
    }

    private Long userId;
    private ActionType type;
    private Map<String, Object> params;  // placeName, dayIndex 등 저장
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;    // TTL

    /**
     * TTL 확인 (5분)
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * 파라미터 편의 메서드
     */
    public String getPlaceName() {
        return (String) params.getOrDefault("placeName", null);
    }

    public Integer getDayIndex() {
        return (Integer) params.getOrDefault("dayIndex", null);
    }

    /**
     * 새로운 PendingAction 생성 헬퍼
     */
    public static PendingAction forDeletePlace(Long userId, Long planId, String placeName) {
        return PendingAction.builder()
            .userId(userId)
            .type(ActionType.DELETE_PLACE)
            .params(new HashMap<>(Map.of(
                "planId", planId,
                "placeName", placeName
            )))
            .createdAt(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusMinutes(5))
            .build();
    }

    /**
     * 위치 기반 삭제용
     */
    public static PendingAction forDeletePlace(Long userId, Long planId, String placeName, Integer dayIndex, Integer position) {
        return PendingAction.builder()
            .userId(userId)
            .type(ActionType.DELETE_PLACE)
            .params(new HashMap<>(Map.of(
                "planId", planId,
                "placeName", placeName,
                "dayIndex", dayIndex,
                "position", position
            )))
            .createdAt(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusMinutes(5))
            .build();
    }

    public static PendingAction forDeleteDay(Long userId, Long planId, Integer dayIndex) {
        return PendingAction.builder()
            .userId(userId)
            .type(ActionType.DELETE_DAY)
            .params(new HashMap<>(Map.of(
                "planId", planId,
                "dayIndex", dayIndex
            )))
            .createdAt(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusMinutes(5))
            .build();
    }

    public static PendingAction forDeletePlan(Long userId, Long planId) {
        return PendingAction.builder()
            .userId(userId)
            .type(ActionType.DELETE_PLAN)
            .params(new HashMap<>(Map.of("planId", planId)))
            .createdAt(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusMinutes(5))
            .build();
    }

    public static PendingAction forUpdatePlace(Long userId, Long planId, String placeName, Integer dayIndex, String newStartTime) {
        return PendingAction.builder()
            .userId(userId)
            .type(ActionType.UPDATE_PLACE)
            .params(new HashMap<>(Map.of(
                "planId", planId,
                "placeName", placeName,
                "dayIndex", dayIndex,
                "newStartTime", newStartTime
            )))
            .createdAt(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusMinutes(5))
            .build();
    }
}
