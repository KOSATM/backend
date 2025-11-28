package com.example.demo.common.chat.intent;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CategoryType {
    PLANNER("planner", "여행 일정 생성·관리·추천 등 일정 관련 기능"),
    SUPPORTER("supporter", "번역, 환율, 날씨 등 여행 도우미 기능"),
    TRAVELGRAM("travelgram", "여행 기록 작성, 사진 업로드 등 기록 기능"),
    ETC("etc", "기타 요청");

    private final String value;
    private final String description;

    public static CategoryType fromValue(String value) {
        for (CategoryType c : values()) {
            if (c.value.equalsIgnoreCase(value)) {
                return c;
            }
        }
        return ETC;
    }

    public static String buildCategoryList() {
    StringBuilder builder = new StringBuilder();
    for (CategoryType category : CategoryType.values()) {
        builder.append("- ")
               .append(category.getValue())
               .append(" : ")
               .append(category.getDescription())
               .append("\n");
    }
    return builder.toString();
}

}