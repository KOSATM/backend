package com.example.demo.planner.plan.agent.test.date;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DateParser {

    private static final DateTimeFormatter[] FORMATTERS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy.MM.dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("yyyyMMdd")
    };

    /**
     * startDate 문자열을 LocalDate로 파싱
     * 
     * @param startDate 자연어 날짜 표현 (예: "오늘", "다음주 토요일", "2025-12-21")
     * @return LocalDate (파싱 실패 시 오늘+7일)
     */
    public static LocalDate parse(String startDate) {
        if (startDate == null || startDate.trim().isEmpty()) {
            LocalDate defaultDate = LocalDate.now().plusDays(7);
            log.info("startDate가 없어서 기본값(7일 뒤) 반환: {}", defaultDate);
            return defaultDate;
        }

        String normalized = startDate.trim().toLowerCase()
                .replaceAll("\\s+", ""); // 공백 제거

        log.info("startDate 파싱 시작: {}", startDate);

        // 1. 상대적 날짜 표현
        if (normalized.matches(".*오늘.*|.*today.*")) {
            log.info("오늘로 파싱");
            return LocalDate.now();
        }

        if (normalized.matches(".*내일.*|.*tomorrow.*")) {
            log.info("내일로 파싱");
            return LocalDate.now().plusDays(1);
        }

        if (normalized.matches(".*모레.*")) {
            log.info("모레로 파싱");
            return LocalDate.now().plusDays(2);
        }

        // 2. "N일 뒤" 패턴
        Pattern daysLaterPattern = Pattern.compile("(\\d+)일.*뒤");
        Matcher daysLaterMatcher = daysLaterPattern.matcher(normalized);
        if (daysLaterMatcher.find()) {
            int daysToAdd = Integer.parseInt(daysLaterMatcher.group(1));
            LocalDate result = LocalDate.now().plusDays(daysToAdd);
            log.info("{}일 뒤 → {}", daysToAdd, result);
            return result;
        }

        // 3. "이번주 X요일" 패턴
        if (normalized.contains("이번주") || normalized.contains("이번주말")) {
            DayOfWeek targetDay = extractDayOfWeek(normalized);
            if (targetDay != null) {
                LocalDate result = getNextDayOfWeek(LocalDate.now(), targetDay, false);
                log.info("이번주 {} → {}", targetDay, result);
                return result;
            }
        }

        // 4. "다음주 X요일" 패턴
        if (normalized.contains("다음주") || normalized.contains("다음주말")) {
            DayOfWeek targetDay = extractDayOfWeek(normalized);
            if (targetDay != null) {
                LocalDate result = getNextDayOfWeek(LocalDate.now(), targetDay, true);
                log.info("다음주 {} → {}", targetDay, result);
                return result;
            }

            // "다음주"만 있고 요일 없으면 다음주 월요일
            LocalDate result = getNextDayOfWeek(LocalDate.now(), DayOfWeek.MONDAY, true);
            log.info("다음주 → {}", result);
            return result;
        }

        // 5. 단순 요일만 있는 경우 (이번주 기준)
        DayOfWeek dayOfWeek = extractDayOfWeek(normalized);
        if (dayOfWeek != null) {
            LocalDate result = getNextDayOfWeek(LocalDate.now(), dayOfWeek, false);
            log.info("요일 {} → {}", dayOfWeek, result);
            return result;
        }

        // 6. 정확한 날짜 형식 (yyyy-MM-dd, yyyy.MM.dd 등)
        for (DateTimeFormatter formatter : FORMATTERS) {
            try {
                LocalDate parsed = LocalDate.parse(startDate.trim(), formatter);
                log.info("날짜 형식 파싱 성공: {}", parsed);
                return parsed;
            } catch (DateTimeParseException e) {
                // 다음 포맷 시도
            }
        }

        // 7. 파싱 실패 시 기본값 (7일 뒤)
        LocalDate defaultDate = LocalDate.now().plusDays(7);
        log.warn("startDate '{}' 파싱 실패, 기본값(7일 뒤) 반환: {}", startDate, defaultDate);
        return defaultDate;
    }

    /**
     * 문자열에서 요일 추출
     */
    private static DayOfWeek extractDayOfWeek(String text) {
        if (text.contains("월요일") || text.contains("월"))
            return DayOfWeek.MONDAY;
        if (text.contains("화요일") || text.contains("화"))
            return DayOfWeek.TUESDAY;
        if (text.contains("수요일") || text.contains("수"))
            return DayOfWeek.WEDNESDAY;
        if (text.contains("목요일") || text.contains("목"))
            return DayOfWeek.THURSDAY;
        if (text.contains("금요일") || text.contains("금"))
            return DayOfWeek.FRIDAY;
        if (text.contains("토요일") || text.contains("토") || text.contains("주말"))
            return DayOfWeek.SATURDAY;
        if (text.contains("일요일") || text.contains("일"))
            return DayOfWeek.SUNDAY;
        return null;
    }

    /**
     * 특정 요일의 다음 날짜 찾기
     * 
     * @param from      기준 날짜
     * @param targetDay 찾을 요일
     * @param nextWeek  true면 다음주, false면 이번주
     */
    private static LocalDate getNextDayOfWeek(LocalDate from, DayOfWeek targetDay, boolean nextWeek) {
        LocalDate result = from;
        int daysToAdd = targetDay.getValue() - from.getDayOfWeek().getValue();

        if (nextWeek) {
            // 다음주: 무조건 7일 이상 더하기
            daysToAdd += 7;
        } else {
            // 이번주: 오늘 이후의 해당 요일
            if (daysToAdd <= 0) {
                daysToAdd += 7;
            }
        }

        return result.plusDays(daysToAdd);
    }
}
