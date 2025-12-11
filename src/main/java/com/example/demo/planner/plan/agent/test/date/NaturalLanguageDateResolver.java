package com.example.demo.planner.plan.agent.test.date;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NaturalLanguageDateResolver {

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 사용자 자연어를 실제 LocalDate로 변환.
     * 
     * @param text 사용자 입력 (e.g. "주말", "3일 뒤", "다음 주말")
     * @return LocalDate
     */
    public static LocalDate resolve(String text) {
        text = text.trim();

        LocalDate today = LocalDate.now();

        // 1) 절대 날짜 입력 ("2025-02-03")
        if (text.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return LocalDate.parse(text, YMD);
        }

        // 2) "2월 3일" 같은 입력
        Pattern mmdd = Pattern.compile("(\\d{1,2})월\\s*(\\d{1,2})일");
        Matcher m = mmdd.matcher(text);
        if (m.find()) {
            int month = Integer.parseInt(m.group(1));
            int day = Integer.parseInt(m.group(2));
            int year = today.getYear();
            return LocalDate.of(year, month, day);
        }

        // 3) 상대적 날짜: 오늘/내일/모레
        if (text.contains("오늘"))
            return today;
        if (text.contains("내일"))
            return today.plusDays(1);
        if (text.contains("모레"))
            return today.plusDays(2);

        // 4) "N일 뒤"
        Pattern afterNDays = Pattern.compile("(\\d+)일\\s*뒤");
        Matcher after = afterNDays.matcher(text);
        if (after.find()) {
            return today.plusDays(Integer.parseInt(after.group(1)));
        }

        // 5) "이번 주말", "주말"
        if (text.equals("주말") || text.equals("이번 주말")) {
            return findNextSaturday(today);
        }

        // 6) "다음 주말"
        if (text.contains("다음 주말") || text.contains("다음주말")) {
            LocalDate thisSat = findNextSaturday(today);
            return thisSat.plusWeeks(1);
        }

        // 7) "토요일", "일요일"
        if (text.contains("토요일"))
            return findNextWeekday(today, DayOfWeek.SATURDAY);
        if (text.contains("일요일"))
            return findNextWeekday(today, DayOfWeek.SUNDAY);

        // 8) "다음주"
        if (text.contains("다음주")) {
            return today.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        }

        // 기본: 오늘 반환 (fail-safe)
        return today;
    }

    // ------------------------------
    // 헬퍼 메소드
    // ------------------------------

    /** 이번/다음 토요일 계산 */
    private static LocalDate findNextSaturday(LocalDate base) {
        return base.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
    }

    /** 특정 요일의 다음 날짜 반환 */
    private static LocalDate findNextWeekday(LocalDate base, DayOfWeek target) {
        return base.with(TemporalAdjusters.nextOrSame(target));
    }
}