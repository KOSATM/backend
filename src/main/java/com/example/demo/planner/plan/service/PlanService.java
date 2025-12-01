package com.example.demo.planner.plan.service;

import com.example.demo.planner.plan.agent.InternetSearchAgent;
import com.example.demo.planner.plan.dao.PlanDao;
import com.example.demo.planner.plan.dao.PlanDayDao;
import com.example.demo.planner.plan.dao.PlanPlaceDao;
import com.example.demo.planner.plan.dao.PlanSnapshotDao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class PlanService {
  private final PlanDao planDao;
  private final PlanDayDao planDayDao;
  private final PlanPlaceDao planPlaceDao;
  private final PlanSnapshotDao planSnapshotDao;

  private final InternetSearchAgent internetSearchAgent;

}
