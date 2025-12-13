package com.example.demo.planner.plan.service;

import org.springframework.stereotype.Component;

import com.example.demo.planner.plan.dto.response.PlanSnapshotContent;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * PlanSnapshot JSON 파싱 유틸리티
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PlanSnapshotUtility {

    /**
     * Snapshot JSON을 파싱하여 PlanSnapshotContent로 변환
     */
    public PlanSnapshotContent parseSnapshot(String snapshotJson) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        PlanSnapshotContent planSnapshotContent = objectMapper.readValue(snapshotJson, PlanSnapshotContent.class);
        return planSnapshotContent;
    }
}
