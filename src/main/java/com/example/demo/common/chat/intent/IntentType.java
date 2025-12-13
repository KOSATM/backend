package com.example.demo.common.chat.intent;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.example.demo.common.global.agent.AiAgent;
import com.example.demo.planner.plan.agent.SmartPlanAgent;
import com.example.demo.planner.plan.agent.PlaceSuggestAgent;
import com.example.demo.planner.plan.service.create.TravelPlannerService;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum IntentType {

    TRAVEL_PLAN(
        "travel_plan",
        List.of("create_plan", "make_plan", "new_plan", "start_trip"),
        CategoryType.PLANNER,
        "/planner/edit",
        "여행 일정 생성",
        TravelPlannerService.class
    ),

    PLAN_ACTION(
        "plan_action",
        List.of(
            "edit_plan", "modify_plan", "update_plan",
            "show_plan", "show_my_plan", "view_plan",
            "swap", "move", "change_day", "change_place"
        ),
        CategoryType.PLANNER,
        "/planner",
        "자연어 일정 조작 (LLM Full Reasoning)",
        SmartPlanAgent.class
    ),

    PLAN_PLACE_RECOMMEND(
        "plan_place_recommend",
        List.of("recommend_place", "place_recommend", "suggest_place"),
        CategoryType.PLANNER,
        "/planner/recommend",
        "플레이스 추천",
        PlaceSuggestAgent.class
    ),

    OTHER(
        "other",
        List.of(),
        CategoryType.ETC,
        "/",
        "기타 요청",
        SmartPlanAgent.class
    );

    private final String value;
    private final List<String> aliases;
    private final CategoryType category;
    private final String requiredUrl;
    private final String humanReadable;
    private final Class<? extends AiAgent> agentClass;

    public static IntentType fromValue(String value) {
        value = value.toLowerCase();

        for (IntentType type : values()) {
            if (type.value.equalsIgnoreCase(value)) return type;
            if (type.aliases.contains(value)) return type;
        }
        return OTHER;
    }

    public static String buildIntentList() {
        return Arrays.stream(values())
                .map(type -> String.format("- %s: %s (aliases: %s)",
                    type.value,
                    type.humanReadable,
                    String.join(", ", type.aliases)))
                .collect(Collectors.joining("\n"));
    }
}
