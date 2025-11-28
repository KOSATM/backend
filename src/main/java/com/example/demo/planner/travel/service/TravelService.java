package com.example.demo.planner.travel.service;

import com.example.demo.planner.travel.dao.TravelDayDao;
import com.example.demo.planner.travel.dao.TravelPlaceDao;
import com.example.demo.planner.travel.dao.TravelPlanDao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class TravelService {
  private final TravelPlanDao travelPlanDao;
  private final TravelDayDao travelDayDao;
  private final TravelPlaceDao travelPlaceDao;

  
}
