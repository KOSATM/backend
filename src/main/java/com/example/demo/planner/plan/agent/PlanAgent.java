package com.example.demo.planner.plan.agent;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.example.demo.planner.plan.dto.entity.Plan;
import com.example.demo.planner.plan.service.PlanService;

import lombok.extern.slf4j.Slf4j;

/**
 * Plan Agent - AI 기반 여행 계획 관리
 * Tool을 사용하여 여행 계획 CRUD 작업 수행
 */
@Component
@Slf4j
public class PlanAgent {

    private final ChatClient chatClient;
    private final PlanService planService;

    public PlanAgent(
            ChatClient.Builder chatClientBuilder,
            PlanService planService) {
        this.chatClient = chatClientBuilder.build();
        this.planService = planService;
    }

    /**
     * 사용자 메시지를 받아 적절한 Tool을 실행하고 응답 생성
     */
    public String chat(String userMessage, Long userId) {
        String systemPrompt = """
            당신은 서울 여행 계획 도우미입니다.

            중요: 현재 사용자 ID는 %d입니다. 모든 Tool 호출 시 반드시 이 userId를 사용하세요.

            가능한 기능:
            1. 여행 계획 생성 (createPlan tool 사용, userId=%d)
            2. 기존 계획 조회 (getPlan tool 사용, userId=%d)

            필수 규칙:
            - 모든 응답은 반드시 영어로만 작성하세요
            - 모든 Tool 호출 시 반드시 userId=%d를 전달하세요
            - Tool을 사용하여 데이터베이스와 상호작용하세요
            - 친절하고 도움이 되는 태도를 유지하세요

            사용자 요청 처리:
            - "계획 만들어줘" → createPlan(userId=%d, days=X, budget=Y) 호출
            - "내 계획 보여줘" → getPlan(userId=%d, planId=X) 호출
            """.formatted(userId, userId, userId, userId, userId, userId);

        try {
            String response = chatClient.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .tools(new PlanTools())
                .call()
                .content();

            return response;

        } catch (Exception e) {
            log.error("Error in PlanAgent", e);
            return "Sorry, I encountered an error: " + e.getMessage();
        }
    }

    /**
     * Plan 관련 Tools
     */
    class PlanTools {

        @Tool(description = """
            서울 여행 계획을 새로 생성합니다.
            사용자가 새로운 여행 계획을 만들고 싶을 때 이 Tool을 호출하세요.
            파라미터:
            - userId: 사용자 ID (필수)
            - days: 여행 일수 (필수)
            - budget: 예산 (원화, 선택)
            - startDate: 여행 시작일 (YYYY-MM-DD, 선택)

            반환: 생성된 계획의 ID와 요약 정보
            """)
        public String createPlan(
                @ToolParam(description = "사용자 ID") Long userId,
                @ToolParam(description = "여행 일수 (예: 3, 5, 7)") Integer days,
                @ToolParam(description = "예산 (원화, 예: 500000)") Integer budget,
                @ToolParam(description = "여행 시작일 (YYYY-MM-DD)") String startDateStr) {

            log.info("Tool called: createPlan(userId={}, days={}, budget={})", userId, days, budget);

            try {
                BigDecimal budgetDecimal = budget != null ? new BigDecimal(budget) : new BigDecimal("500000");
                LocalDate startDate = startDateStr != null ? LocalDate.parse(startDateStr) : LocalDate.now();

                Plan plan = planService.createPlanWithSampleData(userId, days, budgetDecimal, startDate);

                return String.format("""
                    ✅ Travel plan created successfully!

                    Plan ID: #%d
                    Duration: %s ~ %s (%d days)
                    Budget: ₩%,d
                    Sample places: %d locations created

                    Your Seoul adventure is ready! Each day includes morning and afternoon activities.
                    """, plan.getId(), plan.getStartDate(), plan.getEndDate(), days, budgetDecimal.longValue(), days * 2);

            } catch (Exception e) {
                log.error("Error creating plan", e);
                return "Failed to create plan: " + e.getMessage();
            }
        }

        @Tool(description = """
            특정 여행 계획의 상세 정보를 조회합니다.
            파라미터:
            - planId: 조회할 계획의 ID (필수)

            반환: 계획의 상세 정보
            """)
        public String getPlan(@ToolParam(description = "계획 ID") Long planId) {
            log.info("Tool called: getPlan(planId={})", planId);

            try {
                Plan plan = planService.findById(planId);
                if (plan == null) {
                    return "Plan not found with ID: " + planId;
                }

                return String.format("""
                    📋 Plan Details:

                    Plan ID: #%d
                    Duration: %s ~ %s
                    Budget: ₩%,d
                    Status: %s
                    """, plan.getId(), plan.getStartDate(), plan.getEndDate(),
                    plan.getBudget().longValue(),
                    plan.getIsEnded() ? "Completed" : "Active");

            } catch (Exception e) {
                log.error("Error getting plan", e);
                return "Failed to get plan: " + e.getMessage();
            }
        }
    }
}
