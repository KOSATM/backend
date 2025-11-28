package com.example.demo.common.chat.intent;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum IntentType {

    // -------------------- PLANNER --------------------
    TRAVEL_PLAN("travel_plan", CategoryType.PLANNER, "/planner"),
    PLAN_ADD("plan_add", CategoryType.PLANNER, "/planner"),
    PLAN_DELETE("plan_delete", CategoryType.PLANNER, "/planner"),
    PLAN_MODIFY("plan_modify", CategoryType.PLANNER, "/planner"),
    PLAN_PLACE_RECOMMEND("plan_place_recommend", CategoryType.PLANNER, "/planner/recommend"),
    ATTRACTION_RECOMMEND("attraction_recommend", CategoryType.PLANNER, "/planner/recommend"),
    HOTEL_RECOMMEND("hotel_recommend", CategoryType.PLANNER, "/planner/hotel"),

    // -------------------- SUPPORTER --------------------
    CURRENCY_EXCHANGE("currency_exchange", CategoryType.SUPPORTER, "/supporter"),
    TRANSLATION("translation", CategoryType.SUPPORTER, "/supporter"),
    WEATHER("weather", CategoryType.SUPPORTER, "/supporter"),

    // -------------------- TRAVELGRAM --------------------
    CREATE_POST("create_post", CategoryType.TRAVELGRAM, "/travelgram"),
    ADD_PHOTO("add_photo", CategoryType.TRAVELGRAM, "/travelgram"),

    // -------------------- ETC --------------------
    ETC("etc", CategoryType.ETC, "/");

    private final String value;
    private final CategoryType category;
    private final String requiredUrl;
    
    public static IntentType fromValue(String value) {
        for (IntentType t : values()) {
            if (t.value.equalsIgnoreCase(value)) {
                return t;
            }
        }
        return ETC;
    }
}
