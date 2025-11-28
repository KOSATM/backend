package com.example.demo.common.chat.intent;

import lombok.Getter;

@Getter
public enum CategoryType {
    PLANNER("planner"),
    SUPPORTER("supporter"),
    TRAVELGRAM("travelgram"),
    ETC("etc");

    private final String value;

    CategoryType(String value) {
        this.value = value;
    }

    public static CategoryType fromValue(String value) {
        for (CategoryType c : values()) {
            if (c.value.equalsIgnoreCase(value)) {
                return c;
            }
        }
        return ETC;
    }
}