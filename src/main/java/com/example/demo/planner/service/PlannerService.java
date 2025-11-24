package com.example.demo.planner.service;

import com.example.demo.dto.travel.TravelPlan;
import com.example.demo.planner.agent.PlannerAgent;
import com.example.demo.planner.dao.TravelPlanDao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PlannerService {

    @Autowired
    private TravelPlanDao plannerDao;

    @Autowired
    private PlannerAgent plannerAgent;

    public void createTravelPlan(TravelPlan travelPlan) {
        plannerDao.insertTravelPlan(travelPlan);
    }

    public String chat(String question) {
        return plannerAgent.generatePlan(question);
    }
}
