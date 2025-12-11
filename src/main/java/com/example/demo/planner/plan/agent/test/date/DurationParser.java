package com.example.demo.planner.plan.agent.test.date;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DurationParser {

    /**
     * duration 문자열을 일수로 파싱
     * 
     * @param duration 자연어 기간 표현 (예: "주말", "1박2일", "3일")
     * @return 일수 (최소 1일, 최대 7일)
     */
    public static int parse(String duration) {
        if (duration == null || duration.trim().isEmpty()) {
            log.info("duration이 없어서 기본값 1일 반환");
            return 1;
        }

        String normalized = duration.trim().toLowerCase()
                .replaceAll("\\s+", ""); // 공백 제거

        log.info("duration 파싱 시작: {}", duration);

        // 1. 명확한 패턴 매칭
        if (normalized.matches(".*하루.*|.*당일.*|.*1일.*")) {
            log.info("1일로 파싱");
            return 1;
        }

        if (normalized.matches(".*주말.*|.*이틀.*|.*2일.*")) {
            log.info("2일로 파싱");
            return 2;
        }

        if (normalized.matches(".*3일.*|.*사흘.*")) {
            log.info("3일로 파싱");
            return 3;
        }

        if (normalized.matches(".*4일.*|.*나흘.*")) {
            log.info("4일로 파싱");
            return 4;
        }

        if (normalized.matches(".*5일.*|.*닷새.*")) {
            log.info("5일로 파싱");
            return 5;
        }

        if (normalized.matches(".*6일.*|.*엿새.*")) {
            log.info("6일로 파싱");
            return 6;
        }

        if (normalized.matches(".*7일.*|.*일주일.*|.*1주일.*")) {
            log.info("7일로 파싱");
            return 7;
        }

        // 2. "N박M일" 패턴 (예: 1박2일, 2박3일)
        Pattern nightDayPattern = Pattern.compile("(\\d+)박(\\d+)일");
        Matcher matcher = nightDayPattern.matcher(normalized);
        if (matcher.find()) {
            int days = Integer.parseInt(matcher.group(2));
            days = Math.min(Math.max(days, 1), 7); // 1~7일 제한
            log.info("{}박{}일 → {}일로 파싱", matcher.group(1), matcher.group(2), days);
            return days;
        }

        // 3. 순수 숫자만 있는 경우 (예: "3", "5")
        Pattern numberPattern = Pattern.compile("^(\\d+)$");
        Matcher numberMatcher = numberPattern.matcher(normalized);
        if (numberMatcher.find()) {
            int days = Integer.parseInt(numberMatcher.group(1));
            days = Math.min(Math.max(days, 1), 7); // 1~7일 제한
            log.info("숫자 {}일로 파싱", days);
            return days;
        }

        // 4. 파싱 실패 시 기본값 1일
        log.warn("duration '{}' 파싱 실패, 기본값 1일 반환", duration);
        return 1;
    }
}