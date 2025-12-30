package com.example.demo.planner.plan.util.date;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
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
     * 사용자 자연어 날짜를 LocalDate로 변환
     * 
     * 정책:
     * - 파싱 실패 시 → 오늘 + 7일
     * - 절대 silent today 반환 ❌
     */
    public static LocalDate parse(String input) {

        // 0️⃣ null / empty → 정책 기본값
        if (input == null || input.trim().isEmpty()) {
            return defaultDate("입력 없음");
        }

        String normalized = normalize(input);
        log.info("📅 startDate 파싱 시도: 원본='{}'", input);

        // 1️⃣ 1차: 단순 자연어 Resolver (빠른 성공 케이스)
        LocalDate simpleResolved = tryResolveSimple(normalized);
        if (simpleResolved != null) {
            log.info("✅ 단순 자연어 파싱 성공 → {}", simpleResolved);
            return simpleResolved;
        }

        // 2️⃣ "N일 뒤"
        Matcher daysLater = Pattern.compile("(\\d+)일.*뒤").matcher(normalized);
        if (daysLater.find()) {
            int days = Integer.parseInt(daysLater.group(1));
            LocalDate result = LocalDate.now().plusDays(days);
            log.info("✅ '{}일 뒤' 파싱 → {}", days, result);
            return result;
        }

        // 3️⃣ 이번주 / 다음주 + 요일
        DayOfWeek dayOfWeek = extractDayOfWeek(normalized);
        if (dayOfWeek != null) {
            boolean nextWeek = normalized.contains("다음주");
            LocalDate result = resolveWeekday(dayOfWeek, nextWeek);
            log.info("✅ 주차+요일 파싱 → {}", result);
            return result;
        }

        // 4️⃣ 정확한 날짜 포맷
        for (DateTimeFormatter formatter : FORMATTERS) {
            try {
                LocalDate parsed = LocalDate.parse(input.trim(), formatter);
                log.info("✅ 날짜 포맷 파싱 성공 → {}", parsed);
                return parsed;
            } catch (DateTimeParseException ignore) {
            }
        }

        // 5️⃣ 최종 실패 → 정책 기본값
        return defaultDate("모든 파싱 실패");
    }

    // ==================================================
    // 내부 Helper
    // ==================================================

    private static String normalize(String input) {
        return input.trim()
                .toLowerCase()
                .replaceAll("\\s+", "");
    }

    /**
     * 아주 단순한 자연어 처리 (성공 확률 높은 것만)
     */
    private static LocalDate tryResolveSimple(String text) {

        LocalDate today = LocalDate.now();

        if (text.contains("오늘"))
            return today;

        if (text.contains("내일"))
            return today.plusDays(1);

        if (text.contains("모레"))
            return today.plusDays(2);

        if (text.equals("주말") || text.equals("이번주말"))
            return today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));

        if (text.contains("다음주말"))
            return today.with(TemporalAdjusters.next(DayOfWeek.SATURDAY));

        return null; // ❗ 성공 못 하면 다음 단계로 넘김
    }

    private static DayOfWeek extractDayOfWeek(String text) {
        if (text.contains("월")) return DayOfWeek.MONDAY;
        if (text.contains("화")) return DayOfWeek.TUESDAY;
        if (text.contains("수")) return DayOfWeek.WEDNESDAY;
        if (text.contains("목")) return DayOfWeek.THURSDAY;
        if (text.contains("금")) return DayOfWeek.FRIDAY;
        if (text.contains("토")) return DayOfWeek.SATURDAY;
        if (text.contains("일")) return DayOfWeek.SUNDAY;
        return null;
    }

    private static LocalDate resolveWeekday(DayOfWeek target, boolean nextWeek) {
        LocalDate today = LocalDate.now();
        if (nextWeek) {
            return today.with(TemporalAdjusters.next(target));
        }
        return today.with(TemporalAdjusters.nextOrSame(target));
    }

    private static LocalDate defaultDate(String reason) {
        LocalDate fallback = LocalDate.now().plusDays(7);
        log.warn("⚠️ 날짜 파싱 실패 ({}). 기본값 반환 → {}", reason, fallback);
        return fallback;
    }
}
