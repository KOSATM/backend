package com.example.demo.travelgram.aiReview.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PlanAITool {

    private final PlanAiInputService service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Tool(name = "load_plan_context", description = "Load entire travel plan (days, places, activities, review) for a given planId.")
    public String loadPlanContext(@ToolParam Long planId) {
        try {
            PlanAiInput input = service.buildByPlanId(planId);
            return objectMapper.writeValueAsString(input);
        } catch (Exception e) {
            throw new RuntimeException("JSON building failed", e);
        }
    }
}