package com.example.demo.planner.plan.agent;

import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import com.example.demo.planner.plan.agent.tools.PlaceManagementTools;

import lombok.extern.slf4j.Slf4j;

/**
 * 장소 관리 전문 에이전트
 * - Tool-Calling 방식으로 추가/삭제/교체 수행
 * - SmartPlanAgent → PlaceManagementAgent → PlaceManagementTools
 */
@Component
@Slf4j
public class PlaceManagementAgent {

    private final ChatClient chatClient;
    private final PlaceManagementTools placeManagementTools;

    public PlaceManagementAgent(
            ChatClient.Builder builder,
            PlaceManagementTools placeManagementTools) {
        this.chatClient = builder.build();
        this.placeManagementTools = placeManagementTools;
    }

    /**
     * ✅ SmartPlanAgent에서 호출되는 @Tool 메서드
     * 장소 추가 (Tool-Calling 방식)
     */
    @Tool(description = "여행 일정에 새로운 장소를 추가합니다 (자동으로 네이버에서 검색하여 추가)")
    public String addPlace(Long planId, String placeName, Integer dayIndex, String startTime) {
        log.info("➕ [PlaceManagementAgent @Tool] 장소 추가: planId={}, place={}, day={}, time={}",
                planId, placeName, dayIndex, startTime);

        String systemPrompt = """
                당신은 여행 일정 관리 전문가입니다.
                장소 추가 작업을 수행합니다.

                ## 사용 가능한 도구:
                1. searchAndAddPlace: 네이버에서 검색 후 첫 번째 결과 자동 추가

                ## 작업 절차:
                1. searchAndAddPlace 도구로 장소 검색 및 추가
                2. 결과를 사용자 친화적인 한국어로 설명

                ## 응답 형식:
                - "✅ [장소명]을(를) [N일차] 일정에 추가했습니다."
                - 실패 시: "❌ [사유]"
                """;

        String result = chatClient.prompt()
                .system(systemPrompt)
                .user("'%s'을(를) %d일차 %s에 추가해주세요.".formatted(placeName, dayIndex, startTime))
                .tools(placeManagementTools)
                .toolContext(Map.of(
                    "planId", planId,
                    "placeName", placeName,
                    "dayIndex", dayIndex,
                    "startTime", startTime
                ))
                .call()
                .content();

        log.info("➕ [PlaceManagementAgent @Tool] 추가 완료");
        return result;
    }

    /**
     * ✅ SmartPlanAgent에서 호출되는 @Tool 메서드
     * 장소 삭제 (Tool-Calling 방식)
     */
    @Tool(description = "여행 일정에서 특정 장소를 삭제합니다")
    public String deletePlace(Long planId, String placeName) {
        log.info("🗑️ [PlaceManagementAgent @Tool] 장소 삭제: planId={}, place={}", planId, placeName);

        String systemPrompt = """
                당신은 여행 일정 관리 전문가입니다.
                장소 삭제 작업을 수행합니다.

                ## 사용 가능한 도구:
                1. deletePlaceFromPlan: 일정에서 장소 제거

                ## 응답 형식:
                - "✅ [장소명]을(를) 일정에서 삭제했습니다."
                - 실패 시: "❌ [장소명]을(를) 찾을 수 없습니다."
                """;

        String result = chatClient.prompt()
                .system(systemPrompt)
                .user("'%s'을(를) 삭제해주세요.".formatted(placeName))
                .tools(placeManagementTools)
                .toolContext(Map.of("planId", planId, "placeName", placeName))
                .call()
                .content();

        log.info("🗑️ [PlaceManagementAgent @Tool] 삭제 완료");
        return result;
    }

    /**
     * ✅ SmartPlanAgent에서 호출되는 @Tool 메서드
     * 장소 교체 (Tool-Calling 방식)
     */
    @Tool(description = "여행 일정의 기존 장소를 새로운 장소로 교체합니다")
    public String replacePlace(Long planId, String oldPlaceName, String newPlaceName) {
        log.info("🔄 [PlaceManagementAgent @Tool] 장소 교체: planId={}, old={}, new={}",
                planId, oldPlaceName, newPlaceName);

        String systemPrompt = """
                당신은 여행 일정 관리 전문가입니다.
                장소 교체 작업을 수행합니다.

                ## 사용 가능한 도구:
                1. replacePlaceInPlan: 기존 장소를 새 장소로 교체

                ## 응답 형식:
                - "✅ [기존장소]를 [새장소]로 변경했습니다."
                - 실패 시: "❌ [사유]"
                """;

        String result = chatClient.prompt()
                .system(systemPrompt)
                .user("'%s'를 '%s'로 교체해주세요.".formatted(oldPlaceName, newPlaceName))
                .tools(placeManagementTools)
                .toolContext(Map.of(
                    "planId", planId,
                    "oldPlaceName", oldPlaceName,
                    "newPlaceName", newPlaceName
                ))
                .call()
                .content();

        log.info("🔄 [PlaceManagementAgent @Tool] 교체 완료");
        return result;
    }
}
