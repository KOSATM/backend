package com.example.demo.planner.service;

import com.example.demo.dto.travel.TravelPlan;
import com.example.demo.planner.dao.PlannerDao;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlannerService {

    private final PlannerDao plannerDao;

    public void createTravelPlan(TravelPlan travelPlan) {
        plannerDao.insertTravelPlan(travelPlan);
    }
}
