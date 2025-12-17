package com.example.demo.planner.plan.agent;

import java.time.LocalDate;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.example.demo.planner.plan.agent.tools.TravelPlanTools;
import com.example.demo.planner.plan.utils.date.DateParser;
import com.example.demo.planner.plan.utils.date.DurationParser;

import lombok.extern.slf4j.Slf4j;

/**
 * TravelPlanAgent - 여행 계획 생성 Agent
 * 
 * 구조 (단일 LLM 호출 원칙):
 * SmartPlanAgent (ChatClient.call() ← 유일한 LLM 호출!)
 *   ↓ @Tool
 * TravelPlanAgent (순수 Java)
 *   ↓ 직접 실행
 * TravelPlanTools
 *   ↓
 * TravelPlanGenerationService (business logic)
 */
@Component
@Slf4j
public class TravelPlanAgent {

    private final TravelPlanTools travelPlanTools;

    public TravelPlanAgent(TravelPlanTools travelPlanTools) {
        this.travelPlanTools = travelPlanTools;
        log.info("✅ TravelPlanAgent 초기화 완료 (순수 Java, LLM 호출 없음)");
    }

    // =========================================================
    // ✅ 일정 생성 Tool (직접 실행, LLM 호출 없음)
    // =========================================================
    @Tool(name = "createSeoulTravelPlanStructured", description = "서울 여행 일정을 생성하고 DB에 저장합니다")
    public String createSeoulTravelPlanStructured(
            @ToolParam(description = "여행 기간(일). 필수!") Integer duration,
            @ToolParam(description = "여행 스타일", required = false) String style,
            @ToolParam(description = "선호 지역", required = false) String location,
            @ToolParam(description = "일정 강도", required = false) String pace,
            @ToolParam(description = "시작일 자연어", required = false) String startDateText,
            @ToolParam(description = "사용자 ID") Long userId) {

        log.info("🔧 [TravelPlanAgent @Tool] 일정 생성 요청: userId={}, duration={}, style={}, location={}, pace={}",
                userId, duration, style, location, pace);

        try {
            int safeDuration = Math.min(
                    duration != null && duration > 0 ? duration : DurationParser.parse(null),
                    7);

            // TravelPlanTools를 직접 호출 (LLM 없이)
            String result = travelPlanTools.generateAndSavePlan(
                    userId, safeDuration, style, location, pace, startDateText);

            log.info("✅ [TravelPlanAgent @Tool] 일정 생성 완료");
            return result;

        } catch (Exception e) {
            log.error("❌ [TravelPlanAgent @Tool] 일정 생성 실패", e);
            return "❌ 일정 생성에 실패했습니다: " + e.getMessage();
        }
    }

    /**
     * 일정 강도 Enum (TravelPlanGenerationService에서 사용)
     */
    public enum Pace {
        RELAXED(3),
        NORMAL(5),
        TIGHT(7);

        private final int placesPerDay;

        Pace(int placesPerDay) {
            this.placesPerDay = placesPerDay;
        }

        public int getPlacesPerDay() {
            return placesPerDay;
        }

        public String getLabel() {
            return switch (this) {
                case RELAXED -> "널널";
                case NORMAL -> "보통";
                case TIGHT -> "빡빡";
            };
        }

        public static Pace fromString(String str) {
            if (str == null)
                return NORMAL;
            if (str.contains("빡빡"))
                return TIGHT;
            if (str.contains("널널"))
                return RELAXED;
            return NORMAL;
        }
    }
}
