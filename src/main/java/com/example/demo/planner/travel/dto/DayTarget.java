package com.example.demo.planner.travel.dto;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DayTarget {

    private final Map<String, Integer> categoryTargets; // 카테고리별 목표
    private final int maxTotal; // 하루 최대 장소 수

    /** 특정 카테고리 목표량 가져오기 */
    public int getTarget(String category) {
        return categoryTargets.getOrDefault(category, 0);
    }
    
    public Map<String, Integer> getTargets() {
        return categoryTargets;
    }
}