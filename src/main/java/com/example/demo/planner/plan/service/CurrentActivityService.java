package com.example.demo.planner.plan.service;

import org.springframework.stereotype.Service;

import com.example.demo.planner.plan.dao.CurrentActivityDao;
import com.example.demo.planner.plan.dto.entity.CurrentActivity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class CurrentActivityService {
    private final CurrentActivityDao dao;

    // planPlaceId로 current activity 조회 
    public CurrentActivity getActivityByPlanPlaceId(Long planPlaceId) {
        CurrentActivity activity = dao.selectCurrentActivityByPlanPlaceId(planPlaceId);
        if (activity == null) {
            throw new IllegalArgumentException("해당 일정의 활동이 없습니다. planPlaceId=" + planPlaceId);
        }
        return activity;
    }

    // placeId를 기준으로 기존 데이터 확인
    public void saveOrUpdateCurrentActivity(CurrentActivity currentActivity) {
    CurrentActivity existing = dao.selectCurrentActivityByPlanPlaceId(currentActivity.getPlanPlaceId());
    
    if (existing == null) {
        // 새로 생성
        dao.insertCurrentActivity(currentActivity);
        log.info("새로운 활동 저장: placeId={}", currentActivity.getPlanPlaceId());
    } else {
        // 기존 데이터 덮어쓰기 (id 유지)
        currentActivity.setId(existing.getId());
        dao.updateCurrentActivity(currentActivity);
        log.info("활동 업데이트: placeId={}", currentActivity.getPlanPlaceId());
    }
    dao.updatePlanplaceIsEnded(currentActivity.getPlanPlaceId());
}
}
