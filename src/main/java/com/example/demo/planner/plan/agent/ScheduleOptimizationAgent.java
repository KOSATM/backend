package com.example.demo.planner.plan.agent;

import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import com.example.demo.planner.plan.agent.tools.ScheduleOptimizationTools;

import lombok.extern.slf4j.Slf4j;

/**
 * 일정 최적화 전문 에이전트
 * - Tool-Calling 방식으로 순서 변경, 일정 확장 등 수행
 * - SmartPlanAgent → ScheduleOptimizationAgent → ScheduleOptimizationTools
 */
@Component
@Slf4j
public class ScheduleOptimizationAgent {

    private final ChatClient chatClient;
    private final ScheduleOptimizationTools scheduleOptimizationTools;

    public ScheduleOptimizationAgent(
            ChatClient.Builder builder,
            ScheduleOptimizationTools scheduleOptimizationTools) {
        this.chatClient = builder.build();
        this.scheduleOptimizationTools = scheduleOptimizationTools;
    }

    /**
     * ✅ SmartPlanAgent에서 호출되는 @Tool 메서드
     * 장소 순서 교환 (같은 날 내)
     */
    @Tool(description = "같은 날짜 내에서 두 장소의 순서를 교환합니다")
    public String swapPlaces(Long planId, Integer dayIndex, Integer index1, Integer index2) {
        log.info("🔀 [ScheduleOptimizationAgent @Tool] 장소 순서 교환: planId={}, day={}, idx1={}, idx2={}",
                planId, dayIndex, index1, index2);

        String systemPrompt = """
                당신은 여행 일정 최적화 전문가입니다.
                같은 날짜 내에서 두 장소의 순서를 교환합니다.

                ## 사용 가능한 도구:
                1. swapPlacesInSameDay: 같은 날 내 장소 순서 교환

                ## 응답 형식:
                - "✅ [N일차]의 [X번째]와 [Y번째] 장소 순서를 교환했습니다."
                """;

        String result = chatClient.prompt()
                .system(systemPrompt)
                .user("%d일차의 %d번째와 %d번째 장소를 교환해주세요.".formatted(dayIndex, index1, index2))
                .tools(scheduleOptimizationTools)
                .toolContext(Map.of("planId", planId, "dayIndex", dayIndex, "index1", index1, "index2", index2))
                .call()
                .content();

        log.info("🔀 [ScheduleOptimizationAgent @Tool] 교환 완료");
        return result;
    }

    /**
     * ✅ SmartPlanAgent에서 호출되는 @Tool 메서드
     * 장소 순서 교환 (다른 날짜 간)
     */
    @Tool(description = "서로 다른 날짜 간 장소를 교환합니다")
    public String swapPlacesBetweenDays(Long planId, Integer day1, Integer index1, Integer day2, Integer index2) {
        log.info("🔀 [ScheduleOptimizationAgent @Tool] 날짜 간 교환: planId={}, day1={}, idx1={}, day2={}, idx2={}",
                planId, day1, index1, day2, index2);

        String systemPrompt = """
                당신은 여행 일정 최적화 전문가입니다.
                서로 다른 날짜 간 장소를 교환합니다.

                ## 사용 가능한 도구:
                1. swapPlacesBetweenDifferentDays: 다른 날짜 간 장소 교환

                ## 응답 형식:
                - "✅ [N일차]의 [X번째] 장소와 [M일차]의 [Y번째] 장소를 교환했습니다."
                """;

        String result = chatClient.prompt()
                .system(systemPrompt)
                .user("%d일차 %d번째와 %d일차 %d번째 장소를 교환해주세요.".formatted(day1, index1, day2, index2))
                .tools(scheduleOptimizationTools)
                .toolContext(Map.of("planId", planId, "day1", day1, "index1", index1, "day2", day2, "index2", index2))
                .call()
                .content();

        log.info("🔀 [ScheduleOptimizationAgent @Tool] 교환 완료");
        return result;
    }

    /**
     * ✅ SmartPlanAgent에서 호출되는 @Tool 메서드
     * 여행 기간 확장
     */
    @Tool(description = "여행 기간을 연장합니다 (일수 추가)")
    public String extendPlan(Long planId, Integer extraDays) {
        log.info("📅 [ScheduleOptimizationAgent @Tool] 여행 기간 확장: planId={}, extraDays={}", planId, extraDays);

        String systemPrompt = """
                당신은 여행 일정 최적화 전문가입니다.
                여행 기간을 연장합니다.

                ## 사용 가능한 도구:
                1. extendTravelPlan: 여행 일수 추가

                ## 응답 형식:
                - "✅ 여행 기간을 [N일] 연장했습니다. 현재 [M일] 여행입니다."
                """;

        String result = chatClient.prompt()
                .system(systemPrompt)
                .user("여행 기간을 %d일 연장해주세요.".formatted(extraDays))
                .tools(scheduleOptimizationTools)
                .toolContext(Map.of("planId", planId, "extraDays", extraDays))
                .call()
                .content();

        log.info("📅 [ScheduleOptimizationAgent @Tool] 확장 완료");
        return result;
    }
}
