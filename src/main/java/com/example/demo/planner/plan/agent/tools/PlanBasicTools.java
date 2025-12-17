package com.example.demo.planner.plan.agent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.planner.plan.agent.common.PlanToolSupport;
import com.example.demo.planner.plan.service.action.PlanAddAction;
import com.example.demo.planner.plan.service.action.PlanDeleteAction;
import com.example.demo.planner.plan.service.action.PlanModifyAction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component("planBasicTools")
@RequiredArgsConstructor
@Slf4j
public class PlanBasicTools {
    
    private final PlanToolSupport support;
    private final PlanDeleteAction deleteAction;
    private final PlanAddAction addAction;
    private final PlanModifyAction modifyAction;
    
    @Transactional
    @Tool(description = "특정 장소를 일정에서 삭제합니다")
    public String deletePlace(String placeName) {
        Long planId = support.getPlanId();
        log.info("🔧 [Tool] deletePlace: planId={}, placeName={}", planId, placeName);
        
        try {
            deleteAction.deletePlaceByName(planId, placeName);
            Integer version = support.saveSnapshot(planId);  // ✅ 공통 로직!
            
            return String.format("✅ '%s' 장소를 일정에서 삭제했습니다. 버전: %d", placeName, version);
            
        } catch (IllegalArgumentException e) {
            return String.format("❌ '%s' 장소를 찾을 수 없습니다.", placeName);
        } catch (Exception e) {
            log.error("장소 삭제 실패", e);
            return String.format("❌ 장소 삭제 중 오류 발생: %s", e.getMessage());
        }
    }
    
    @Transactional
    @Tool(description = "특정 날짜에 새로운 장소를 추가합니다. dayIndex는 1부터 시작")
    public String addPlace(int dayIndex, String placeName, String startTime) {
        Long planId = support.getPlanId();
        log.info("🔧 [Tool] addPlace: planId={}, dayIndex={}, placeName={}", planId, dayIndex, placeName);
        
        try {
            String result = addAction.addPlace(planId, dayIndex, placeName, startTime);
            Integer version = support.saveSnapshot(planId);  // ✅ 공통 로직!
            
            return String.format("✅ %d일차에 '%s'을(를) 추가했습니다. 버전: %d", dayIndex, result, version);
            
        } catch (Exception e) {
            log.error("장소 추가 실패", e);
            return String.format("❌ 장소 추가 중 오류 발생: %s", e.getMessage());
        }
    }
    
    @Transactional
    @Tool(description = "기존 장소를 다른 장소로 교체합니다")
    public String replacePlace(String oldPlaceName, String newPlaceName) {
        Long planId = support.getPlanId();
        log.info("🔧 [Tool] replacePlace: planId={}, old={}, new={}", planId, oldPlaceName, newPlaceName);
        
        try {
            String newName = modifyAction.replacePlaceWithSearch(planId, oldPlaceName, newPlaceName);
            Integer version = support.saveSnapshot(planId);  // ✅ 공통 로직!
            
            return String.format("✅ '%s'를 '%s'(으)로 변경했습니다. 버전: %d", oldPlaceName, newName, version);
            
        } catch (Exception e) {
            log.error("장소 교체 실패", e);
            return String.format("❌ 장소 교체 중 오류 발생: %s", e.getMessage());
        }
    }


    @Transactional
    @Tool(description = "특정 장소의 시간을 변경합니다")
    public String updatePlaceTime(String placeName, String newTime) {
        Long planId = support.getPlanId();
        log.info("🔧 [Tool] updatePlaceTime: planId={}, placeName={}, newTime={}", planId, placeName, newTime);
        
        try {
            modifyAction.updatePlaceTime(planId, placeName, newTime);
            Integer version = support.saveSnapshot(planId);  // ✅ 공통 로직!
            
            return String.format("✅ '%s'의 시간을 %s(으)로 변경했습니다. 버전: %d", placeName, newTime, version);
            
        } catch (Exception e) {
            log.error("시간 변경 실패", e);
            return String.format("❌ 시간 변경 중 오류 발생: %s", e.getMessage());
        }
    }
    
    @Transactional
    @Tool(description = "특정 날짜 전체를 삭제합니다 (dayIndex는 1부터 시작)")
    public String deleteDay(int dayIndex) {
        Long planId = support.getPlanId();
        log.info("🔧 [Tool] deleteDay: planId={}, dayIndex={}", planId, dayIndex);
        
        try {
            deleteAction.deleteDay(planId, dayIndex);
            Integer version = support.saveSnapshot(planId);  // ✅ 공통 로직!
            
            return String.format("✅ %d일차 일정을 삭제했습니다. 버전: %d", dayIndex, version);
            
        } catch (Exception e) {
            log.error("날짜 삭제 실패", e);
            return String.format("❌ 날짜 삭제 중 오류 발생: %s", e.getMessage());
        }
    }
}