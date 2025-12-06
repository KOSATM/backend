package com.example.demo.travelgram.review.ai.dto.response;

import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GeneratedStyleResponse {
    // AI가 반환할 4개의 스타일 리스트
    private List<StyleItem> styles;

    @Data
    @NoArgsConstructor
    public static class StyleItem {
        private String toneCode;   // "EMOTIONAL", "INFORMATIVE", "WITTY", "SIMPLE"
        private String toneName;   // "감성적인", "정보가 가득한", "재치있는", "심플한"
        private String caption;    // 생성된 본문
        private List<String> hashtags; // 해시태그 10개
    }
}
