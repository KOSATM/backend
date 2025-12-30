package com.example.demo.common.util;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 사용자 입력에서 숫자/위치 정보를 파싱하는 유틸리티
 */
public class UserInputParser {

    private UserInputParser() {
        // 인스턴스화 방지
    }

    /**
     * "5번", "3번째" → 숫자 추출
     */
    public static Integer parseIndex(String text) {
        if (text == null) return null;

        Matcher m = Pattern.compile("(\\d+)\\s*번").matcher(text);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignore) {
            }
        }
        return null;
    }

    /**
     * "2일차", "둘째날" → 일차 추출
     */
    public static Integer parseDayIndex(String text) {
        if (text == null) return null;

        // 1) "2일차", "2일", "2번째날", "2째날" 등
        Matcher m1 = Pattern.compile("(\\d+)\\s*(일차|일|번째\\s*날|째\\s*날|째날|번째날)").matcher(text);
        if (m1.find()) {
            try {
                int d = Integer.parseInt(m1.group(1));
                return d >= 1 ? d : null;
            } catch (NumberFormatException ignore) {
            }
        }

        // 2) 한글 서수
        if (text.contains("첫째")) return 1;
        if (text.contains("둘째")) return 2;
        if (text.contains("셋째")) return 3;
        if (text.contains("넷째")) return 4;
        if (text.contains("다섯째")) return 5;

        return null;
    }

    /**
     * "3번째", "맨 뒤" → 위치 추출
     */
    public static Integer parsePosition(String text) {
        if (text == null) return null;

        // "맨 뒤", "마지막", "끝"
        if (text.contains("맨 뒤") || text.contains("마지막") || text.contains("끝")) {
            return Integer.MAX_VALUE;
        }

        // "맨 앞", "처음", "첫"
        if (text.contains("맨 앞") || text.contains("처음") || text.contains("첫")) {
            return 1;
        }

        // "3번째", "3번"
        Matcher m = Pattern.compile("(\\d+)\\s*(번째|번)").matcher(text);
        if (m.find()) {
            try {
                int p = Integer.parseInt(m.group(1));
                return p >= 1 ? p : null;
            } catch (NumberFormatException ignore) {
            }
        }

        // 한글 서수
        if (text.contains("두번째") || text.contains("두 번째")) return 2;
        if (text.contains("세번째") || text.contains("세 번째")) return 3;
        if (text.contains("네번째") || text.contains("네 번째")) return 4;
        if (text.contains("다섯번째") || text.contains("다섯 번째")) return 5;

        return null;
    }
}