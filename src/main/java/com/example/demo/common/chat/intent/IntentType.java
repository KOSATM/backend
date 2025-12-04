package com.example.demo.common.chat.intent;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.example.demo.common.global.agent.AiAgent;
import com.example.demo.common.global.agent.SampleAiAgent;
import com.example.demo.planner.plan.agent.PlaceSuggestAgent;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum IntentType {

    // -------------------- PLANNER --------------------
    TRAVEL_PLAN("travel_plan", CategoryType.PLANNER, "/planner", "여행 일정 추천", null),
    PLAN_ADD("plan_add", CategoryType.PLANNER, "/planner", "일정에 장소 추가", null),
    PLAN_DELETE("plan_delete", CategoryType.PLANNER, "/planner", "일정에서 장소 삭제", null),
    PLAN_MODIFY("plan_modify", CategoryType.PLANNER, "/planner", "일정 수정", null),
    PLAN_PLACE_RECOMMEND("plan_place_recommend", CategoryType.PLANNER, "/planner/recommend", "여행지 추천",
            PlaceSuggestAgent.class),
    ATTRACTION_RECOMMEND("attraction_recommend", CategoryType.PLANNER, "/planner/recommend", "여행지 추천",
            SampleAiAgent.class),
    HOTEL_RECOMMEND("hotel_recommend", CategoryType.PLANNER, "/planner/hotel", "호텔 추천", null),

    // -------------------- SUPPORTER --------------------
    CURRENCY_EXCHANGE("currency_exchange", CategoryType.SUPPORTER, "/supporter", "환율 정보", null),
    TRANSLATION("translation", CategoryType.SUPPORTER, "/supporter", "번역 기능", null),
    WEATHER("weather", CategoryType.SUPPORTER, "/supporter", "날씨 정보", null),

    // -------------------- TRAVELGRAM --------------------
    CREATE_POST("create_post", CategoryType.TRAVELGRAM, "/travelgram", "여행 기록 작성", null),
    ADD_PHOTO("add_photo", CategoryType.TRAVELGRAM, "/travelgram", "여행 사진 업로드", null),

    // -------------------- ETC --------------------
    ETC("etc", CategoryType.ETC, "/", "기타 요청", null);

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
        return ETC;
    }

    public static Map<CategoryType, List<IntentType>> groupByCategory() {
        return Arrays.stream(values())
                .collect(Collectors.groupingBy(IntentType::getCategory));
    }

    public static String buildIntentList() {
        StringBuilder sb = new StringBuilder();
        Map<CategoryType, List<IntentType>> grouped = groupByCategory();

        for (CategoryType category : CategoryType.values()) {
            sb.append("## ").append(category.getValue())
                    .append(" : ").append(category.getDescription()).append("\n");

            for (IntentType type : grouped.get(category)) {
                sb.append("- ").append(type.getValue())
                        .append(" : ").append(type.getHumanReadable()).append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }


}
