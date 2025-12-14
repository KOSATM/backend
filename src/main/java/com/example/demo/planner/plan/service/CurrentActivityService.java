package com.example.demo.planner.plan.service;

import org.springframework.stereotype.Service;

import com.example.demo.planner.plan.dao.CurrentActivityDao;
import com.example.demo.planner.plan.dto.entity.CurrentActivity;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CurrentActivityService {
    private final CurrentActivityDao dao;

    public void saveCurrentActivity(CurrentActivity currentActivity) {
        dao.insertCurrentActivity(currentActivity);
    }
}
