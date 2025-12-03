package com.example.demo.planner.plan.service;

import org.springframework.stereotype.Service;

import com.example.demo.planner.plan.agent.InternetSearchAgent;
import com.example.demo.planner.plan.dao.PlanDao;
import com.example.demo.planner.plan.dao.PlanDayDao;
import com.example.demo.planner.plan.dao.PlanPlaceDao;
import com.example.demo.planner.plan.dao.PlanSnapshotDao;
import com.example.demo.planner.plan.dto.entity.PlanSnapshot;
import com.example.demo.planner.plan.dto.response.PlanSnapshotContent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class PlanService {
  private final PlanDao planDao;
  private final PlanDayDao planDayDao;
  private final PlanPlaceDao planPlaceDao;
  private final PlanSnapshotDao planSnapshotDao;
  
  // 스냅샷을 여행 계획, 여행 일자, 여행 장소로 분리
  public PlanSnapshotContent parseSnapshot(String snapshotJson) throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    PlanSnapshotContent planSnapshotContent = objectMapper.readValue(snapshotJson, PlanSnapshotContent.class);
    return planSnapshotContent;
  }

}
