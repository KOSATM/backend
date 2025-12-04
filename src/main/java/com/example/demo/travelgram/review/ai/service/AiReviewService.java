package com.example.demo.travelgram.review.ai.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.demo.planner.plan.dao.PlanDao;
import com.example.demo.planner.plan.dao.PlanDayDao;
import com.example.demo.planner.plan.dao.PlanPlaceDao;
import com.example.demo.planner.plan.dto.entity.Plan;
import com.example.demo.planner.plan.dto.entity.PlanDay;
import com.example.demo.planner.plan.dto.entity.PlanPlace;
import com.example.demo.travelgram.review.ai.agent.PlanTitleGenerateAgent;
import com.example.demo.travelgram.review.ai.builder.ReviewInputJsonBuilder;
import com.example.demo.travelgram.review.ai.dao.AiReviewDao;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class AiReviewService {
    private final PlanDao planDao;
    private final PlanDayDao dayDao;
    private final PlanPlaceDao placeDao;
    private final ReviewInputJsonBuilder builder;


    private final PlanTitleGenerateAgent planTitleGenerateAgent;

    private final AiReviewDao aiReviewDao;

    public ObjectNode createPlanInputJson(Long planId){
        // 🟦 1) plan 전체 조회
        Plan plan = planDao.selectPlanById(planId);

        // 🟦 2) days 조회
        List<PlanDay> planDays = dayDao.selectPlanDaysByPlanId(planId);

        // 🟦 3) map<Long, List<PlanPlace>> 형태로 정리
        Map<Long, List<PlanPlace>> placesByDayId = new HashMap<>();

        for (PlanDay day : planDays) {
            List<PlanPlace> places = placeDao.selectPlanPlacesByPlanDayId(day.getId());
            placesByDayId.put(day.getId(), places);
        }

        // 🟦 4) builder 호출해서 JsonNode 생성
        return builder.build(plan, planDays, placesByDayId);
        
    }

    public String generatePlanTitle(Long planId) {
        ObjectNode inputJson = createPlanInputJson(planId);
        
        // LLM에게 보내기 쉽게 String으로 변환
        String inputJsonString = inputJson.toPrettyString();
        // Title을 agent 통해 생성
        String title = planTitleGenerateAgent.generatePlanTitle(inputJsonString);
        
        return title;
    }
}

