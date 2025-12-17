package com.example.demo.planner.plan.dto.context;

import lombok.extern.slf4j.Slf4j;

/**
 * ThreadLocal 기반 PlanContext 관리자
 * 
 * SmartPlanAgent에서 설정 → Tool/Guard/Action에서 조회
 * 
 * 모든 Tool이 동일한 planId를 사용하도록 보장
 * planId는 절대 Tool 내부에서 새로 만들지 않음
 */
@Slf4j
public class PlanContextHolder {
    
    private static final ThreadLocal<PlanContext> contextHolder = new ThreadLocal<>();
    
    /**
     * PlanContext 설정 (SmartPlanAgent에서 호출)
     */
    public static void set(PlanContext context) {
        if (context == null) {
            throw new IllegalArgumentException("PlanContext cannot be null");
        }
        contextHolder.set(context);
        log.info("🔒 [PlanContextHolder] 설정됨: planId={}", 
            context.getActivePlan().getId());
    }
    
    /**
     * PlanContext 조회 (Tool/Guard/Action에서 호출)
     */
    public static PlanContext get() {
        PlanContext context = contextHolder.get();
        if (context == null) {
            throw new IllegalStateException("PlanContext가 설정되지 않았습니다. SmartPlanAgent에서 먼저 set()을 호출하세요.");
        }
        return context;
    }
    
    /**
     * planId만 편의상 직접 조회
     */
    public static Long getPlanId() {
        return get().getActivePlan().getId();
    }
    
    /**
     * userId 편의상 직접 조회
     */
    public static Long getUserId() {
        return get().getUserId();
    }
    
    /**
     * 초기화 (요청 완료 후)
     */
    public static void clear() {
        Long planId = null;
        try {
            planId = getPlanId();
        } catch (IllegalStateException e) {
            // 이미 clear된 상태
        }
        contextHolder.remove();
        if (planId != null) {
            log.info("🔓 [PlanContextHolder] 초기화됨: planId={}", planId);
        }
    }
}
