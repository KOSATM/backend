package com.example.demo.planner.plan.service;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.example.demo.planner.plan.dto.entity.PendingAction;

import lombok.extern.slf4j.Slf4j;

/**
 * 사용자별 확인 대기 작업 관리
 * 
 * 규칙:
 * - 사용자별 최대 1개만 유지 (동시성 해결)
 * - 새로운 작업 시 기존 작업 폐기
 * - TTL 만료 자동 감지
 */
@Service
@Slf4j
public class PendingActionService {
    
    private final ConcurrentHashMap<Long, PendingAction> store = new ConcurrentHashMap<>();
    
    /**
     * 새로운 PendingAction 생성
     * 기존 작업이 있으면 폐기 (단일화)
     */
    public void save(PendingAction action) {
        Long userId = action.getUserId();
        
        // ✅ 기존 작업 제거 (단일화!)
        if (store.containsKey(userId)) {
            log.info("🗑️ 기존 PendingAction 제거: userId={}, type={}", 
                userId, store.get(userId).getType());
            store.remove(userId);
        }
        
        store.put(userId, action);
        log.info("💾 새로운 PendingAction 저장: userId={}, type={}, expires={}", 
            userId, action.getType(), action.getExpiresAt());
    }
    
    /**
     * 사용자의 최신 PendingAction 조회
     * TTL 만료 시 자동 제거
     */
    public PendingAction getLatest(Long userId) {
        PendingAction action = store.get(userId);
        
        if (action == null) {
            return null;
        }
        
        // ✅ TTL 확인
        if (action.isExpired()) {
            log.info("⏰ PendingAction 만료됨: userId={}, type={}", userId, action.getType());
            store.remove(userId);
            return null;
        }
        
        return action;
    }
    
    /**
     * 사용자의 모든 PendingAction 폐기
     * 새로운 명령 또는 취소 시 호출
     */
    public void clearAll(Long userId) {
        if (store.containsKey(userId)) {
            PendingAction removed = store.remove(userId);
            log.info("🛑 PendingAction 폐기: userId={}, type={}", userId, removed.getType());
        }
    }
    
    /**
     * 작업 완료 마크
     */
    public void complete(Long userId) {
        store.remove(userId);
        log.info("✅ PendingAction 완료: userId={}", userId);
    }
    
    /**
     * 테스트용: 현재 상태 조회
     */
    public PendingAction peek(Long userId) {
        return store.get(userId);
    }
}
