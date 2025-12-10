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

    // -------------------- PLANNER --------------------
    TRAVEL_PLAN(
        "travel_plan",
        CategoryType.PLANNER,
        "/planner/edit",
        "여행 일정 생성",
        TravelPlannerService.class
    ),

    PLAN_ACTION(
        "plan_action",
        CategoryType.PLANNER,
        "/planner",
        "일정 관련 자연어 요청 (LLM Full-Reasoning)",
        SmartPlanAgent.class   // LLM이 전체 JSON 보고 직접 reasoning
    ),

    PLAN_PLACE_RECOMMEND(
        "plan_place_recommend",
        CategoryType.PLANNER,
        "/planner/recommend",
        "여행지/장소 추천",
        PlaceSuggestAgent.class
    ),

    // -------------------- ETC --------------------
    OTHER(
        "other",
        CategoryType.ETC,
        "/",
        "기타 요청 (SmartPlanAgent가 fallback으로 처리)",
        SmartPlanAgent.class  // Unknown/불확실한 요청을 LLM Full-Reasoning으로 처리
    );

    private final String value;
    private final CategoryType category;
    private final String requiredUrl;
    private final String humanReadable;
    private final Class<? extends AiAgent> agentClass;

    public static IntentType fromValue(String value) {
        for (IntentType t : values()) {
            if (t.value.equalsIgnoreCase(value)) {
                return t;
            }
        }
        return OTHER;
    }

    public static Map<CategoryType, List<IntentType>> groupByCategory() {
        return Arrays.stream(values())
                .collect(Collectors.groupingBy(IntentType::getCategory));
    }

    /**
     * Intent 목록을 Documentation 용으로 반환
     */
    public static String buildIntentList() {
        StringBuilder sb = new StringBuilder();
        Map<CategoryType, List<IntentType>> grouped = groupByCategory();

        for (CategoryType category : CategoryType.values()) {
            sb.append("## ").append(category.getValue())
              .append(" : ").append(category.getDescription()).append("\n");

            List<IntentType> list = grouped.get(category);
            if (list == null) continue;

            for (IntentType type : list) {
                sb.append("- ").append(type.getValue())
                  .append(" : ").append(type.getHumanReadable()).append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
