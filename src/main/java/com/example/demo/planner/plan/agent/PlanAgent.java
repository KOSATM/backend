package com.example.demo.planner.plan.agent;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.example.demo.common.chat.intent.dto.IntentCommand;
import com.example.demo.common.chat.pipeline.AiAgentResponse;
import com.example.demo.common.global.agent.AiAgent;
import com.example.demo.planner.plan.dto.entity.Plan;
import com.example.demo.planner.plan.service.PlanService;

import lombok.extern.slf4j.Slf4j;

/**
 * Plan Agent - AI 기반 여행 계획 관리
 * Tool을 사용하여 여행 계획 CRUD 작업 수행
 */
@Component
@Slf4j
public class PlanAgent implements AiAgent {

    private final ChatClient chatClient;
    private final PlanService planService;

    public PlanAgent(
            ChatClient.Builder chatClientBuilder,
            PlanService planService) {
        this.chatClient = chatClientBuilder.build();
        this.planService = planService;
    }

    /**
     * AiAgent 인터페이스 구현 - IntentCommand로부터 execute
     */
    @Override
    public AiAgentResponse execute(IntentCommand command) {
        Long userId = (Long) command.getArguments().get("userId");
        if (userId == null) {
            return AiAgentResponse.of("User ID is required to query plans");
        }

        // 직접 DB에서 활성 계획 조회
        Plan plan = planService.findActiveByUserId(userId);
        
        if (plan == null) {
            return AiAgentResponse.of("아직 생성된 여행 계획이 없습니다.\n\"3일 여행 계획 만들어줘\"라고 말씀해주시면 새로운 계획을 만들어드릴게요! 🗺️");
        }

        // 계획 정보를 JSON 형식으로 구성
        String planJson = formatPlanAsJson(plan);
        
        // LLM에게 마크다운 형식으로 예쁘게 출력하도록 요청
        String prompt = """
            다음 여행 계획 정보를 사용자에게 친근하고 이해하기 쉽게 마크다운 형식으로 출력해주세요.
            
            규칙:
            1. 이모지를 적절히 활용하세요 (📅, 💰, 📍 등)
            2. 간결하고 명확하게 작성하세요
            3. 날짜는 "12월 6일 (금)" 형식으로 표시하세요
            4. 금액은 "50만원" 또는 "500,000원" 형식으로 표시하세요
            5. 추가 정보나 도움말을 간단히 안내하세요
            
            계획 정보:
            %s
            
            위 정보를 바탕으로 사용자에게 친근하게 안내해주세요.
            """.formatted(planJson);

        String response = chatClient.prompt()
            .user(prompt)
            .call()
            .content();

        return AiAgentResponse.of(response);
    }
    
    /**
     * Plan 객체를 JSON 형식 문자열로 변환
     */
    private String formatPlanAsJson(Plan plan) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"planId\": ").append(plan.getId()).append(",\n");
        json.append("  \"startDate\": \"").append(plan.getStartDate()).append("\",\n");
        json.append("  \"endDate\": \"").append(plan.getEndDate()).append("\",\n");
        json.append("  \"budget\": ").append(plan.getBudget()).append(",\n");
        json.append("  \"title\": \"").append(plan.getTitle() != null ? plan.getTitle() : "서울 여행").append("\",\n");
        json.append("  \"isEnded\": ").append(plan.getIsEnded()).append("\n");
        json.append("}");
        return json.toString();
    }

    /**
     * 사용자 메시지를 받아 적절한 Tool을 실행하고 응답 생성
     */
    public String chat(String userMessage, Long userId) {
        String systemPrompt = """
            당신은 친절한 서울 여행 계획 도우미입니다.

            중요: 현재 사용자 ID는 %d입니다. 모든 Tool 호출 시 반드시 이 userId를 사용하세요.

            가능한 기능:
            1. 여행 계획 생성 (createPlan tool 사용, userId=%d)
            2. 기존 계획 조회 (getPlan tool 사용)
            3. 내 활성 계획 조회 (getMyActivePlan tool 사용, userId=%d)
            4. 일차별 상세 조회 (getDayDetail tool 사용)
            5. 장소 상세 조회 (getPlaceDetail tool 사용)

            필수 규칙:
            - 모든 응답은 한국어로 친근하게 작성하세요
            - 사용자에게 도움이 되는 정보를 간결하고 명확하게 전달하세요
            - 모든 Tool 호출 시 반드시 userId=%d를 전달하세요
            - Tool을 사용하여 데이터베이스와 상호작용하세요

            응답 형식 가이드:
            - 계획 조회 시: "📅 여행 계획 #123\\n기간: 12월 6일 ~ 12월 8일 (3일)\\n예산: 50만원\\n\\n더 자세한 정보가 필요하시면 말씀해주세요!"
            - 일차별 조회 시: "🗓️ Day 1 (12월 6일)\\n방문 장소: 경복궁, 북촌한옥마을, 광화문..."
            - 장소 조회 시: "📍 경복궁\\n주소: 서울시 종로구...\\n예상 비용: 3,000원"

            PlanDay 생성/이동 정책 (중요 - 반드시 준수):
            - dayIndex를 지정하지 않으면 자동으로 순차 생성됩니다 (1, 2, 3...)
            - 특정 일차를 생성하거나 이동할 때, 현재 계획 기간을 초과하는 경우:
              1. 먼저 preview API를 호출하여 확장 필요 여부와 예상 endDate를 확인
              2. 사용자에게 "여행 기간이 X일에서 Y일로 확장됩니다. 진행하시겠습니까?"와 같이 물어봄
              3. 사용자가 승인하면 confirm=true로 실제 API 호출
              4. 사용자가 거부하면 작업 취소
            
            예시 흐름:
            - 사용자: "5일차 추가해줘" (현재 3일 계획)
            - Agent: previewDayCreation(planId=1, dayIndex=5) 호출
            - 결과: requiresExtension=true, newEndDate=2025-12-09
            - Agent: "여행 기간이 3일에서 5일로 확장됩니다 (종료일: 12월 9일). 진행하시겠습니까?"
            - 사용자: "네" → createDay(planId=1, dayIndex=5, confirm=true)
            
            확장이 필요 없는 경우(현재 기간 내):
            - preview 없이 바로 실행 가능 (confirm 불필요)

            사용자 요청 처리:
            - "계획 만들어줘" → createPlan(userId=%d, days=X, budget=Y) 호출
            - "내 계획 보여줘" → getMyActivePlan(userId=%d) 호출 (planId 모를 때)
            - "1번 계획 보여줘" → getPlan(planId=1) 호출 (planId 알 때)
            - "첫째날 일정 보여줘" → getDayDetail(planId=X, dayIndex=1) 호출
            - "경복궁 정보 보여줘" → getPlaceDetail(placeId=X) 호출
            - "X일차 추가해줘" → previewDayCreation → 사용자 확인 → createDay(confirm=true)
            - "Day를 Y일차로 이동" → previewDayMove → 사용자 확인 → moveDay(confirm=true)
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

        @Tool(description = """
            PlanDay 생성 시 여행 기간 확장이 필요한지 미리 확인합니다.
            사용자가 현재 계획 기간을 초과하는 Day를 추가하려 할 때 반드시 먼저 이 Tool을 호출하세요.
            
            파라미터:
            - planId: 여행 계획 ID (필수)
            - dayIndex: 생성하려는 일차 (필수)
            
            반환: 확장 필요 여부, 예상 종료일, 현재 최대 일차
            """)
        public String previewDayCreation(
                @ToolParam(description = "여행 계획 ID") Long planId,
                @ToolParam(description = "생성하려는 일차 (예: 5)") Integer dayIndex) {
            
            log.info("Tool called: previewDayCreation(planId={}, dayIndex={})", planId, dayIndex);

            try {
                var preview = planService.createDayPreview(planId, dayIndex);
                
                if (preview.isRequiresExtension()) {
                    return String.format("""
                        ⚠️ 여행 기간 확장이 필요합니다
                        
                        현재 최대 일차: %d일차
                        요청 일차: %d일차
                        예상 종료일: %s
                        
                        사용자에게 다음과 같이 물어보세요:
                        "여행 기간을 %d일차까지 확장하시겠습니까? (종료일: %s)"
                        
                        승인 시: createDay tool을 confirm=true로 호출
                        거부 시: 작업 취소
                        """, 
                        preview.getCurrentMaxIndex(), 
                        preview.getRequestedToIndex(), 
                        preview.getNewEndDate(),
                        preview.getRequestedToIndex(),
                        preview.getNewEndDate());
                } else {
                    return String.format("""
                        ✅ 확장 불필요 - 바로 생성 가능
                        
                        현재 최대 일차: %d일차
                        요청 일차: %d일차
                        
                        createDay tool을 바로 호출하세요 (confirm 불필요).
                        """, 
                        preview.getCurrentMaxIndex(), 
                        preview.getRequestedToIndex());
                }

            } catch (Exception e) {
                log.error("Error in previewDayCreation", e);
                return "Failed to preview day creation: " + e.getMessage();
            }
        }

        @Tool(description = """
            PlanDay 이동 시 여행 기간 확장이 필요한지 미리 확인합니다.
            Day를 현재 최대 일차보다 뒤로 이동할 때 반드시 먼저 이 Tool을 호출하세요.
            
            파라미터:
            - dayId: 이동할 Day의 ID (필수)
            - toIndex: 목표 일차 (필수)
            
            반환: 확장 필요 여부, 예상 종료일, 현재 최대 일차
            """)
        public String previewDayMove(
                @ToolParam(description = "이동할 Day의 ID") Long dayId,
                @ToolParam(description = "목표 일차 (예: 5)") Integer toIndex) {
            
            log.info("Tool called: previewDayMove(dayId={}, toIndex={})", dayId, toIndex);

            try {
                var preview = planService.movePreview(dayId, toIndex);
                
                if (preview.isRequiresExtension()) {
                    return String.format("""
                        ⚠️ 여행 기간 확장이 필요합니다
                        
                        현재 최대 일차: %d일차
                        목표 일차: %d일차
                        예상 종료일: %s
                        
                        사용자에게 다음과 같이 물어보세요:
                        "Day를 %d일차로 이동하려면 여행 기간 확장이 필요합니다 (종료일: %s). 진행하시겠습니까?"
                        
                        승인 시: moveDay tool을 confirm=true로 호출
                        거부 시: 작업 취소
                        """, 
                        preview.getCurrentMaxIndex(), 
                        preview.getRequestedToIndex(), 
                        preview.getNewEndDate(),
                        preview.getRequestedToIndex(),
                        preview.getNewEndDate());
                } else {
                    return String.format("""
                        ✅ 확장 불필요 - 바로 이동 가능
                        
                        현재 최대 일차: %d일차
                        목표 일차: %d일차
                        
                        moveDay tool을 바로 호출하세요 (confirm 부8필요).
                        """, 
                        preview.getCurrentMaxIndex(), 
                        preview.getRequestedToIndex());
                }

            } catch (Exception e) {
                log.error("Error in previewDayMove", e);
                return "Failed to preview day move: " + e.getMessage();
            }
        }

        @Tool(description = """
            현재 사용자의 활성(진행 중인) 여행 계획을 조회합니다.
            사용자가 계획 ID를 모르고 "내 계획 보여줘" 같은 요청을 할 때 사용하세요.
            
            파라미터:
            - userId: 사용자 ID (필수)
            
            반환: 활성 계획의 상세 정보
            """)
        public String getMyActivePlan(@ToolParam(description = "사용자 ID") Long userId) {
            log.info("Tool called: getMyActivePlan(userId={})", userId);

            try {
                Plan plan = planService.findActiveByUserId(userId);
                if (plan == null) {
                    return "No active travel plan found. Would you like to create one?";
                }

                return String.format("""
                    📋 Your Active Plan:

                    Plan ID: #%d
                    Duration: %s ~ %s
                    Budget: ₩%,d
                    Status: Active
                    
                    Use getDayDetail to see specific days, or ask me to add/modify places!
                    """, plan.getId(), plan.getStartDate(), plan.getEndDate(),
                    plan.getBudget().longValue());

            } catch (Exception e) {
                log.error("Error getting active plan", e);
                return "Failed to get active plan: " + e.getMessage();
            }
        }

        @Tool(description = """
            특정 일차의 상세 일정을 조회합니다.
            파라미터:
            - planId: 여행 계획 ID (필수)
            - dayIndex: 조회할 일차 (필수, 1부터 시작)
            
            반환: 해당 일차의 장소 목록과 시간 정보
            """)
        public String getDayDetail(
                @ToolParam(description = "여행 계획 ID") Long planId,
                @ToolParam(description = "일차 (1부터 시작)") Integer dayIndex) {
            
            log.info("Tool called: getDayDetail(planId={}, dayIndex={})", planId, dayIndex);

            try {
                var day = planService.getDayByIndex(planId, dayIndex);
                if (day == null) {
                    return String.format("Day %d not found in plan #%d", dayIndex, planId);
                }

                var places = planService.getPlacesByDayId(day.getId());
                
                StringBuilder response = new StringBuilder();
                response.append(String.format("""
                    📅 Day %d Details:
                    Date: %s
                    
                    Places (%d):
                    """, dayIndex, day.getPlanDate(), places.size()));
                
                for (int i = 0; i < places.size(); i++) {
                    var place = places.get(i);
                    response.append(String.format("""
                        %d. %s
                           📍 %s
                           ⏰ %s - %s (%d min)
                           💰 ₩%,d
                        
                        """, 
                        i + 1,
                        place.getTitle() != null ? place.getTitle() : place.getPlaceName(),
                        place.getAddress(),
                        place.getStartAt(),
                        place.getEndAt(),
                        place.getEndAt() != null && place.getStartAt() != null ? 
                            java.time.Duration.between(place.getStartAt(), place.getEndAt()).toMinutes() : 0,
                        place.getExpectedCost() != null ? place.getExpectedCost().longValue() : 0
                    ));
                }

                return response.toString();

            } catch (Exception e) {
                log.error("Error getting day detail", e);
                return "Failed to get day detail: " + e.getMessage();
            }
        }

        @Tool(description = """
            특정 장소의 상세 정보를 조회합니다.
            파라미터:
            - placeId: 장소 ID (필수)
            
            반환: 장소의 상세 정보
            """)
        public String getPlaceDetail(@ToolParam(description = "장소 ID") Long placeId) {
            log.info("Tool called: getPlaceDetail(placeId={})", placeId);

            try {
                var place = planService.getPlaceById(placeId);
                if (place == null) {
                    return "Place not found with ID: " + placeId;
                }

                return String.format("""
                    📍 Place Details:

                    Title: %s
                    Place Name: %s
                    Address: %s
                    Location: %s, %s
                    Time: %s - %s
                    Expected Cost: ₩%,d
                    """,
                    place.getTitle() != null ? place.getTitle() : "(No title)",
                    place.getPlaceName(),
                    place.getAddress(),
                    place.getLat(),
                    place.getLng(),
                    place.getStartAt(),
                    place.getEndAt(),
                    place.getExpectedCost() != null ? place.getExpectedCost().longValue() : 0);

            } catch (Exception e) {
                log.error("Error getting place detail", e);
                return "Failed to get place detail: " + e.getMessage();
            }
        }
    }
}
