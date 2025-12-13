package com.example.demo.planner.plan.controller;

import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.planner.plan.dao.PlanDayDao;
import com.example.demo.planner.plan.dao.PlanPlaceDao;
import com.example.demo.planner.plan.dto.entity.Plan;
import com.example.demo.planner.plan.dto.entity.PlanDay;
import com.example.demo.planner.plan.dto.entity.PlanPlace;
import com.example.demo.planner.plan.service.PlanSnapshotService;
import com.example.demo.planner.plan.service.PlanCrudService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/snapshot")
@RequiredArgsConstructor
@Slf4j
public class TestPlanSnapshotController {
  private final PlanSnapshotService planSnapshotService;
  private final PlanCrudService planCrudService;
  private final PlanDayDao planDayDao;
  private final PlanPlaceDao planPlaceDao;

  @PostMapping("/create-test")
  public Object createTest() throws Exception {
    Plan plan = planCrudService.findById(114L);
    List<PlanDay> planDays = planDayDao.selectPlanDaysByPlanId(114L);
    List<PlanPlace> planPlaces = planPlaceDao.selectPlanPlacesByPlanId(114L);

    return planSnapshotService.savePlanSnapshot(plan, planDays, planPlaces);
  }

}
