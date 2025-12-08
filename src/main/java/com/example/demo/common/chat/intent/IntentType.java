package com.example.demo.common.chat.intent;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.example.demo.common.global.agent.AiAgent;
import com.example.demo.planner.plan.agent.PlaceSuggestAgent;
import com.example.demo.planner.plan.agent.PlanAgent;
import com.example.demo.planner.plan.service.create.TravelPlannerService;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum IntentType {

    // -------------------- PLANNER --------------------
    TRAVEL_PLAN("travel_plan", CategoryType.PLANNER, "/planner", "여행 일정 추천", TravelPlannerService.class),
    PLAN_ADD("plan_add", CategoryType.PLANNER, "/planner", "일정에 장소 추가", null),
    PLAN_DELETE("plan_delete", CategoryType.PLANNER, "/planner", "일정에서 장소 삭제", null),
    PLAN_MODIFY("plan_modify", CategoryType.PLANNER, "/planner", "일정 수정", null),
    PLAN_DAY_SWAP("plan_day_swap", CategoryType.PLANNER, "/planner", "일차 통째로 교체", PlanAgent.class),
    
    // ========== 조회 관련 Intent (VIEW) ==========
    VIEW_PLAN("view_plan", CategoryType.PLANNER, "/planner", "전체 일정 조회", PlanAgent.class),
    VIEW_PLAN_DAY("view_plan_day", CategoryType.PLANNER, "/planner", "특정 날짜의 일정 조회 (day 1, 첫날, 12월 6일)", PlanAgent.class),
    VIEW_PLAN_PLACE("view_plan_place", CategoryType.PLANNER, "/planner", "특정 장소 조회 (강남, 명동교자 등)", PlanAgent.class),
    VIEW_PLACE_DAY("view_place_day", CategoryType.PLANNER, "/planner", "특정 장소가 몇일차에 있는지 조회", PlanAgent.class),
    VIEW_PLAN_TIME_RANGE("view_plan_time_range", CategoryType.PLANNER, "/planner", "시간대별 일정 조회 (아침, 점심, 저녁)", PlanAgent.class),
    VIEW_CURRENT_ACTIVITY("view_current_activity", CategoryType.PLANNER, "/planner", "현재 시간 기준 일정 조회", PlanAgent.class),
    VIEW_NEXT_ACTIVITY("view_next_activity", CategoryType.PLANNER, "/planner", "다음 일정 조회", PlanAgent.class),
    VIEW_PLAN_SUMMARY("view_plan_summary", CategoryType.PLANNER, "/planner", "여행 요약 조회", PlanAgent.class),
    
    PLAN_PLACE_RECOMMEND("plan_place_recommend", CategoryType.PLANNER, "/planner/recommend", "여행지 추천",
            PlaceSuggestAgent.class),
    // ATTRACTION_RECOMMEND("attraction_recommend", CategoryType.PLANNER, "/planner/recommend", "여행지 추천",
    //         SampleAiAgent.class),
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
