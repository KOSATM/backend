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
public class TravelService {
  private final PlanDao travelPlanDao;
  private final PlanDayDao travelDayDao;
  private final PlanPlaceDao travelPlaceDao;
  private final PlanSnapshotDao travelPlanSnapshotDao;

  private final InternetSearchAgent internetSearchAgent;

}
