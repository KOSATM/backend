package com.example.demo.planner.plan.agent;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.example.demo.common.chat.intent.dto.IntentCommand;
import com.example.demo.common.chat.pipeline.AiAgentResponse;
import com.example.demo.common.global.agent.AiAgent;
import com.example.demo.planner.plan.dto.entity.Plan;
import com.example.demo.planner.plan.service.create.PlanService;

import lombok.extern.slf4j.Slf4j;

/**
 * Plan Agent - AI 기반 여행 계획 관리
 * Tool을 사용하여 여행 계획 CRUD 작업 수행
 */
@Component
@Slf4j
public class PlanAgent implements AiAgent {

    private final ChatClient chatClient;
    private final PlanService planService;

    public PlanAgent(
            ChatClient.Builder chatClientBuilder,
            PlanService planService) {
        this.chatClient = chatClientBuilder.build();
        this.planService = planService;
    }

    /**
     * AiAgent 인터페이스 구현 - IntentCommand로부터 execute
     */
    @Override
    public AiAgentResponse execute(IntentCommand command, Long userId) {
        String intentName = command.getIntent() != null ? command.getIntent().name() : "";
        String lang = (String) command.getArguments().getOrDefault("lang", "ko");

        // ========== VIEW INTENTS (조회) ==========
        
        // VIEW_PLAN: 전체 일정 조회
        if ("VIEW_PLAN".equals(intentName)) {
            Plan plan = planService.findActiveByUserId(userId);
            if (plan == null) {
                return AiAgentResponse.of("No active travel plan found.");
            }
            try {
                var allDays = planService.queryAllDays(plan.getId());
                return AiAgentResponse.of(formatFullPlan(plan, allDays));
            } catch (Exception e) {
                return AiAgentResponse.of("Error retrieving plan: " + e.getMessage());
            }
        }

        // VIEW_PLAN_DAY: 특정 일차 조회
        if ("VIEW_PLAN_DAY".equals(intentName)) {
            Integer dayIndex = parseInteger(command.getArguments().get("dayIndex"));
            String dateStr = (String) command.getArguments().get("date");
            
            Plan plan = planService.findActiveByUserId(userId);
            if (plan == null) {
                return AiAgentResponse.of("No active travel plan found.");
            }
            
            try {
                var dayWithPlaces = (dayIndex != null) 
                    ? planService.queryDay(plan.getId(), dayIndex)
                    : planService.queryDayByDate(plan.getId(), dateStr);
                return AiAgentResponse.of(formatDaySchedule(dayWithPlaces, dayIndex != null ? dayIndex : 0, "en"));
            } catch (Exception e) {
                return AiAgentResponse.of("Error: " + e.getMessage());
            }
        }

        // VIEW_PLAN_PLACE: 특정 장소 조회
        if ("VIEW_PLAN_PLACE".equals(intentName)) {
            String placeName = (String) command.getArguments().get("placeName");
            Integer dayIndex = parseInteger(command.getArguments().get("dayIndex"));
            Integer placeIndex = parseInteger(command.getArguments().get("placeIndex"));
            
            Plan plan = planService.findActiveByUserId(userId);
            if (plan == null) {
                return AiAgentResponse.of("No active travel plan found.");
            }
            
            try {
                if (placeName != null) {
                    // 장소명으로 검색
                    var places = planService.queryPlacesByName(plan.getId(), placeName);
                    return AiAgentResponse.of(formatPlaceSearchResults(places, placeName));
                } else if (dayIndex != null && placeIndex != null) {
                    // 일차 + 순서로 검색
                    var place = planService.queryPlace(plan.getId(), dayIndex, placeIndex);
                    return AiAgentResponse.of(formatPlaceDetail(place, dayIndex, placeIndex, "en"));
                } else {
                    return AiAgentResponse.of("Please specify either a place name or day/place index.");
                }
            } catch (Exception e) {
                return AiAgentResponse.of("Error: " + e.getMessage());
            }
        }

        // VIEW_CURRENT_ACTIVITY: 현재 시간 기준 일정
        if ("VIEW_CURRENT_ACTIVITY".equals(intentName)) {
            Plan plan = planService.findActiveByUserId(userId);
            if (plan == null) {
                return AiAgentResponse.of("No active travel plan found.");
            }
            
            try {
                var currentActivity = planService.queryCurrentActivity(plan.getId());
                if (currentActivity == null) {
                    return AiAgentResponse.of("No activity scheduled for the current time.");
                }
                return AiAgentResponse.of(formatCurrentActivity(currentActivity));
            } catch (Exception e) {
                return AiAgentResponse.of("Error: " + e.getMessage());
            }
        }

        // VIEW_NEXT_ACTIVITY: 다음 일정
        if ("VIEW_NEXT_ACTIVITY".equals(intentName)) {
            Plan plan = planService.findActiveByUserId(userId);
            if (plan == null) {
                return AiAgentResponse.of("No active travel plan found.");
            }
            
            try {
                var nextActivity = planService.queryNextActivity(plan.getId());
                if (nextActivity == null) {
                    return AiAgentResponse.of("No upcoming activities.");
                }
                return AiAgentResponse.of(formatNextActivity(nextActivity));
            } catch (Exception e) {
                return AiAgentResponse.of("Error: " + e.getMessage());
            }
        }

        // VIEW_PLAN_SUMMARY: 여행 요약 조회
        if ("VIEW_PLAN_SUMMARY".equals(intentName)) {
            Plan plan = planService.findActiveByUserId(userId);
            if (plan == null) {
                return AiAgentResponse.of("No active travel plan found.");
            }
            
            try {
                return AiAgentResponse.of(formatPlanSummary(plan));
            } catch (Exception e) {
                return AiAgentResponse.of("Error: " + e.getMessage());
            }
        }

        // VIEW_PLAN_TIME_RANGE: 시간대별 일정 조회 (아침/점심/저녁)
        if ("VIEW_PLAN_TIME_RANGE".equals(intentName)) {
            String timeRange = (String) command.getArguments().get("range");
            
            if (timeRange == null || timeRange.isEmpty()) {
                return AiAgentResponse.of("Please specify a time range (morning, lunch, or evening).");
            }
            
            try {
                var places = planService.getPlansByTimeRange(userId, timeRange);
                if (places.isEmpty()) {
                    return AiAgentResponse.of("No activities found for " + timeRange + ".");
                }
                
                // LLM으로 한 줄 요약 생성 (맨 위에 표시)
                String summary = generateTimeRangeSummary(timeRange, places.size());
                
                // 서버에서 시간대 일정 렌더링
                String schedule = formatTimeRangeSchedule(timeRange, places);
                
                return AiAgentResponse.of("⭐ " + summary + "\n\n" + schedule);
            } catch (Exception e) {
                return AiAgentResponse.of("Error: " + e.getMessage());
            }
        }

        // VIEW_PLACE_DAY: 특정 장소가 몇일차에 있는지 조회
        if ("VIEW_PLACE_DAY".equals(intentName)) {
            String placeName = (String) command.getArguments().get("placeName");
            
            if (placeName == null || placeName.isEmpty()) {
                return AiAgentResponse.of("Please specify which place you want to find.");
            }
            
            try {
                var position = planService.findPlacePosition(placeName, userId);
                if (position == null) {
                    return AiAgentResponse.of("I couldn't find \"" + placeName + "\" in your travel plan.");
                }
                
                // LLM으로 한 줄 요약 생성 (맨 위에 표시)
                String summary = generatePlaceSummary(position);
                
                // 해당 Day의 전체 장소 목록 조회
                var dayPlaces = planService.getDayPlaces(position.getDayId());
                
                // Day 정보 조회
                var dayInfo = planService.queryDay(
                    planService.findActiveByUserId(userId).getId(), 
                    position.getDayIndex()
                );
                
                // 서버에서 직접 Day 전체 일정 렌더링 (타겟 장소만 Bold)
                String daySchedule = buildHighlightedDaySchedule(
                    dayPlaces,
                    position.getPlaceName(),
                    position.getDayIndex(),
                    position.getDate(),
                    dayInfo.getDay().getTitle(),
                    position.getOrder()
                );
                
                // LLM 요약을 맨 위에, 그 다음 전체 일정
                return AiAgentResponse.of("⭐ " + summary + "\n\n" + daySchedule);
            } catch (Exception e) {
                return AiAgentResponse.of("Error: " + e.getMessage());
            }
        }

        // ========== MODIFICATION INTENTS (수정) ==========

        // PLAN_DAY_SWAP: 일차 통째로 교체
        if ("PLAN_DAY_SWAP".equals(intentName)) {
            Integer dayA = parseInteger(command.getArguments().get("dayIndexA"));
            Integer dayB = parseInteger(command.getArguments().get("dayIndexB"));
            if (dayA == null || dayB == null) {
                return AiAgentResponse.of(getMessage(lang, "일차 번호를 정확히 이해하지 못했어요. 예: '1일차와 3일차 바꿔줘'",
                    "I couldn't understand the day numbers. Example: 'swap day 1 and day 3'"));
            }
            Plan plan = planService.findActiveByUserId(userId);
            if (plan == null) {
                return AiAgentResponse.of(getMessage(lang, "현재 활성화된 여행 계획이 없어요.",
                    "No active travel plan found."));
            }
            try {
                planService.swapDay(plan.getId(), dayA, dayB);
            } catch (Exception e) {
                return AiAgentResponse.of(getMessage(lang, "일차 교체 중 오류가 발생했습니다: " + e.getMessage(),
                    "Error swapping days: " + e.getMessage()));
            }
            return AiAgentResponse.of(getMessage(lang,
                dayA + "일차와 " + dayB + "일차 일정을 서로 교체했어요!",
                "Day " + dayA + " and Day " + dayB + " have been swapped!"));
        }

        // PLAN_QUERY_DAY: 특정 일차 조회
        if ("PLAN_QUERY_DAY".equals(intentName)) {
            Integer dayIndex = parseInteger(command.getArguments().get("dayIndex"));
            if (dayIndex == null) {
                return AiAgentResponse.of(getMessage(lang, "일차 번호를 정확히 이해하지 못했어요. 예: '3일차 보여줘'",
                    "I couldn't understand the day number. Example: 'show me day 3'"));
            }
            Plan plan = planService.findActiveByUserId(userId);
            if (plan == null) {
                return AiAgentResponse.of(getMessage(lang, "현재 활성화된 여행 계획이 없어요.",
                    "No active travel plan found."));
            }
            try {
                var dayWithPlaces = planService.queryDay(plan.getId(), dayIndex);
                return AiAgentResponse.of(formatDaySchedule(dayWithPlaces, dayIndex, lang));
            } catch (Exception e) {
                return AiAgentResponse.of(getMessage(lang, e.getMessage(), e.getMessage()));
            }
        }

        // PLAN_QUERY_PLACE: 특정 장소 조회
        if ("PLAN_QUERY_PLACE".equals(intentName)) {
            Integer dayIndex = parseInteger(command.getArguments().get("dayIndex"));
            Integer placeIndex = parseInteger(command.getArguments().get("placeIndex"));
            if (dayIndex == null || placeIndex == null) {
                return AiAgentResponse.of(getMessage(lang, "일차와 장소 번호를 정확히 이해하지 못했어요. 예: '2일차 첫번째 장소'",
                    "I couldn't understand the day and place numbers. Example: 'day 2 first place'"));
            }
            Plan plan = planService.findActiveByUserId(userId);
            if (plan == null) {
                return AiAgentResponse.of(getMessage(lang, "현재 활성화된 여행 계획이 없어요.",
                    "No active travel plan found."));
            }
            try {
                var place = planService.queryPlace(plan.getId(), dayIndex, placeIndex);
                return AiAgentResponse.of(formatPlaceDetail(place, dayIndex, placeIndex, lang));
            } catch (Exception e) {
                return AiAgentResponse.of(getMessage(lang, e.getMessage(), e.getMessage()));
            }
        }

        return AiAgentResponse.of(getMessage(lang, "지원하지 않는 기능이에요.", "Unsupported feature."));
    }

    /**
     * Object를 Integer로 변환 (null-safe)
     */
    private Integer parseInteger(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Integer) return (Integer) obj;
        try {
            return Integer.parseInt(obj.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 다국어 메시지 반환
     */
    private String getMessage(String lang, String ko, String en) {
        return "en".equalsIgnoreCase(lang) ? en : ko;
    }

    /**
     * 일차별 일정 포맷팅 (아이콘 + 마크다운)
     */
    private String formatDaySchedule(com.example.demo.planner.plan.dto.response.PlanDayWithPlaces dayWithPlaces, int dayIndex, String lang) {
        var day = dayWithPlaces.getDay();
        var places = dayWithPlaces.getPlaces();

        StringBuilder sb = new StringBuilder();
        sb.append("🗓️ **").append(getMessage(lang, dayIndex + "일차 일정", "Day " + dayIndex + " Schedule"));
        sb.append("** — ").append(day.getPlanDate()).append("\n");
        
        // Day 제목이 있고 기본값이 아니면 표시
        if (day.getTitle() != null && !day.getTitle().isEmpty() && !day.getTitle().equals("Day " + dayIndex)) {
            sb.append("   Theme: _").append(day.getTitle()).append("_\n");
        }
        sb.append("\n");

        if (places.isEmpty()) {
            sb.append(getMessage(lang, "_아직 장소가 추가되지 않았어요._", "_No places added yet._"));
        } else {
            for (int i = 0; i < places.size(); i++) {
                var place = places.get(i);
                sb.append("**").append(i + 1).append(". ").append(place.getTitle()).append("**\n");
                sb.append("📍 ").append(place.getPlaceName()).append("\n");
                if (place.getStartAt() != null) {
                    sb.append("⏰ ").append(formatTime(place.getStartAt().toLocalTime()));
                    if (place.getEndAt() != null) {
                        sb.append(" - ").append(formatTime(place.getEndAt().toLocalTime()));
                    }
                    sb.append("\n");
                }
                if (place.getExpectedCost() != null && place.getExpectedCost().longValue() > 0) {
                    sb.append("💰 ₩").append(String.format("%,d", place.getExpectedCost().longValue())).append("\n");
                }
                if (place.getAddress() != null && !place.getAddress().isEmpty()) {
                    sb.append("🏠 ").append(place.getAddress()).append("\n");
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * 특정 장소 상세 정보 포맷팅 (아이콘 + 마크다운)
     */
    private String formatPlaceDetail(com.example.demo.planner.plan.dto.entity.PlanPlace place, int dayIndex, int placeIndex, String lang) {
        StringBuilder sb = new StringBuilder();
        sb.append("📍 **").append(place.getTitle()).append("**\n\n");
        sb.append(getMessage(lang, dayIndex + "일차 " + placeIndex + "번째 장소", "Day " + dayIndex + ", Place #" + placeIndex)).append("\n\n");
        sb.append("**").append(getMessage(lang, "장소명", "Location")).append(":** ").append(place.getPlaceName()).append("\n");

        if (place.getStartAt() != null) {
            sb.append("**").append(getMessage(lang, "시간", "Time")).append(":** ");
            sb.append(formatTime(place.getStartAt().toLocalTime()));
            if (place.getEndAt() != null) {
                sb.append(" - ").append(formatTime(place.getEndAt().toLocalTime()));
            }
            sb.append("\n");
        }

        if (place.getExpectedCost() != null && place.getExpectedCost().longValue() > 0) {
            sb.append("**").append(getMessage(lang, "예상 비용", "Expected Cost")).append(":** ₩")
                .append(String.format("%,d", place.getExpectedCost().longValue())).append("\n");
        }

        if (place.getAddress() != null && !place.getAddress().isEmpty()) {
            sb.append("**").append(getMessage(lang, "주소", "Address")).append(":** ").append(place.getAddress()).append("\n");
        }

        sb.append("**").append(getMessage(lang, "좌표", "Coordinates")).append(":** ")
            .append(String.format("%.6f, %.6f", place.getLat(), place.getLng())).append("\n");

        return sb.toString();
    }

    /**
     * 시간 포맷팅 (HH:mm)
     */
    private String formatTime(java.time.LocalTime time) {
        return time.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
    }

    /**
     * 전체 일정 포맷팅 - 모든 세부 정보 포함 (영어 전용)
     * LLM을 거치지 않고 서버에서 직접 포맷팅하여 100% 정확한 전체 일정 반환
     */
    private String formatFullPlan(Plan plan, java.util.List<com.example.demo.planner.plan.dto.response.PlanDayWithPlaces> allDays) {
        StringBuilder sb = new StringBuilder();
        
        // 헤더
        sb.append("📅 **Your Complete Seoul Travel Plan**\n\n");
        
        // 여행 기본 정보
        sb.append("**Travel Duration:** ").append(plan.getStartDate()).append(" to ").append(plan.getEndDate()).append("\n");
        long totalDays = java.time.temporal.ChronoUnit.DAYS.between(plan.getStartDate(), plan.getEndDate()) + 1;
        sb.append("**Total Days:** ").append(totalDays).append(" days\n");
        
        if (plan.getBudget() != null && plan.getBudget().longValue() > 0) {
            sb.append("**Total Budget:** ₩").append(String.format("%,d", plan.getBudget().longValue())).append("\n");
        }
        
        if (plan.getTitle() != null && !plan.getTitle().isEmpty()) {
            sb.append("**Plan Title:** ").append(plan.getTitle()).append("\n");
        }
        
        sb.append("\n");
        sb.append("═══════════════════════════════════════════════════\n\n");

        // 일차별 상세 일정
        int totalPlaces = 0;
        for (com.example.demo.planner.plan.dto.response.PlanDayWithPlaces dayWithPlaces : allDays) {
            var day = dayWithPlaces.getDay();
            var places = dayWithPlaces.getPlaces();
            totalPlaces += places.size();
            
            sb.append("🗓️ **DAY ").append(day.getDayIndex()).append("** — ").append(day.getPlanDate()).append("\n");
            
            if (day.getTitle() != null && !day.getTitle().isEmpty() && !day.getTitle().equals("Day " + day.getDayIndex())) {
                sb.append("   Theme: _").append(day.getTitle()).append("_\n");
            }
            sb.append("\n");
            
            if (places.isEmpty()) {
                sb.append("   _No activities scheduled for this day._\n\n");
            } else {
                for (int i = 0; i < places.size(); i++) {
                    var place = places.get(i);
                    
                    // 장소 번호 및 제목
                    sb.append("   **").append(i + 1).append(". ").append(place.getTitle()).append("**\n");
                    
                    // 장소명
                    sb.append("      📍 ").append(place.getPlaceName()).append("\n");
                    
                    // 시간
                    if (place.getStartAt() != null) {
                        sb.append("      ⏰ ");
                        sb.append(formatTime(place.getStartAt().toLocalTime()));
                        if (place.getEndAt() != null) {
                            sb.append(" - ").append(formatTime(place.getEndAt().toLocalTime()));
                            long duration = java.time.Duration.between(
                                place.getStartAt().toLocalTime(), 
                                place.getEndAt().toLocalTime()
                            ).toMinutes();
                            sb.append(" (").append(duration).append(" min)");
                        }
                        sb.append("\n");
                    }
                    
                    // 주소
                    if (place.getAddress() != null && !place.getAddress().isEmpty()) {
                        sb.append("      🏠 ").append(place.getAddress()).append("\n");
                    }
                    
                    // 예상 비용
                    if (place.getExpectedCost() != null && place.getExpectedCost().longValue() > 0) {
                        sb.append("      💰 ₩").append(String.format("%,d", place.getExpectedCost().longValue())).append("\n");
                    }
                    
                    sb.append("\n");
                }
            }
            
            sb.append("───────────────────────────────────────────────────\n\n");
        }

        // 푸터 - 전체 요약
        sb.append("📊 **Trip Summary**\n");
        sb.append("   • Total days: ").append(totalDays).append("\n");
        sb.append("   • Total activities: ").append(totalPlaces).append("\n");
        if (plan.getBudget() != null && plan.getBudget().longValue() > 0) {
            sb.append("   • Budget: ₩").append(String.format("%,d", plan.getBudget().longValue())).append("\n");
        }
        sb.append("\n");

        return sb.toString();
    }

    /**
     * 장소 검색 결과 포맷팅 (영어 전용)
     */
    private String formatPlaceSearchResults(java.util.List<com.example.demo.planner.plan.dto.entity.PlanPlace> places, String searchTerm) {
        if (places.isEmpty()) {
            return "No places found matching \"" + searchTerm + "\"";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("🔍 **Places matching \"").append(searchTerm).append("\"**\n\n");
        
        for (int i = 0; i < places.size(); i++) {
            var place = places.get(i);
            sb.append(i + 1).append(". **").append(place.getTitle()).append("**\n");
            sb.append("   📍 ").append(place.getPlaceName()).append("\n");
            if (place.getStartAt() != null) {
                sb.append("   ⏰ ").append(formatTime(place.getStartAt().toLocalTime()));
                if (place.getEndAt() != null) {
                    sb.append(" - ").append(formatTime(place.getEndAt().toLocalTime()));
                }
                sb.append("\n");
            }
            if (place.getAddress() != null && !place.getAddress().isEmpty()) {
                sb.append("   🏠 ").append(place.getAddress()).append("\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * 현재 활동 포맷팅 (영어 전용)
     */
    private String formatCurrentActivity(com.example.demo.planner.plan.dto.entity.PlanPlace activity) {
        StringBuilder sb = new StringBuilder();
        sb.append("⏰ **Current Activity**\n\n");
        sb.append("**").append(activity.getTitle()).append("**\n");
        sb.append("📍 ").append(activity.getPlaceName()).append("\n");
        if (activity.getStartAt() != null && activity.getEndAt() != null) {
            sb.append("🕐 ").append(formatTime(activity.getStartAt().toLocalTime()))
              .append(" - ").append(formatTime(activity.getEndAt().toLocalTime())).append("\n");
        }
        if (activity.getAddress() != null && !activity.getAddress().isEmpty()) {
            sb.append("🏠 ").append(activity.getAddress()).append("\n");
        }
        return sb.toString();
    }

    /**
     * 다음 활동 포맷팅 (영어 전용)
     */
    private String formatNextActivity(com.example.demo.planner.plan.dto.entity.PlanPlace activity) {
        StringBuilder sb = new StringBuilder();
        sb.append("⏭️ **Next Activity**\n\n");
        sb.append("**").append(activity.getTitle()).append("**\n");
        sb.append("📍 ").append(activity.getPlaceName()).append("\n");
        if (activity.getStartAt() != null) {
            sb.append("🕐 Starts at ").append(formatTime(activity.getStartAt().toLocalTime())).append("\n");
        }
        if (activity.getAddress() != null && !activity.getAddress().isEmpty()) {
            sb.append("🏠 ").append(activity.getAddress()).append("\n");
        }
        return sb.toString();
    }

    /**
     * 여행 요약 포맷팅 (영어 전용)
     */
    private String formatPlanSummary(Plan plan) {
        StringBuilder sb = new StringBuilder();
        sb.append("📊 **Travel Plan Summary**\n\n");
        sb.append("**Trip Duration:** ").append(plan.getStartDate()).append(" to ").append(plan.getEndDate()).append("\n");
        
        long days = java.time.temporal.ChronoUnit.DAYS.between(plan.getStartDate(), plan.getEndDate()) + 1;
        sb.append("**Total Days:** ").append(days).append(" days\n");
        
        if (plan.getBudget() != null) {
            sb.append("**Total Budget:** ₩").append(String.format("%,d", plan.getBudget().longValue())).append("\n");
        }
        
        if (plan.getIsEnded() != null && plan.getIsEnded()) {
            sb.append("**Status:** Completed ✅\n");
        } else {
            sb.append("**Status:** In Progress 🚀\n");
        }
        
        return sb.toString();
    }

    /**
     * 특정 장소를 포함한 Day 전체 일정 렌더링 (해당 장소만 Bold)
     * 기존 formatFullPlan과 동일한 템플릿 사용, LLM 요약은 맨 위 추가
     */
    private String buildHighlightedDaySchedule(
            java.util.List<com.example.demo.planner.plan.dto.entity.PlanPlace> places,
            String targetPlaceName,
            Integer dayIndex,
            java.time.LocalDate date,
            String dayTitle,
            int targetOrder) {
        
        StringBuilder sb = new StringBuilder();
        
        // Day 헤더
        sb.append("🗓️ **DAY ").append(dayIndex).append("** — ").append(date).append("\n");
        
        if (dayTitle != null && !dayTitle.isEmpty() && !dayTitle.equals("Day " + dayIndex)) {
            sb.append("   Theme: _").append(dayTitle).append("_\n");
        }
        sb.append("\n");
        
        // 장소 목록 (타겟 장소만 Bold 처리)
        for (int i = 0; i < places.size(); i++) {
            var place = places.get(i);
            boolean isTarget = place.getPlaceName().equalsIgnoreCase(targetPlaceName);
            
            // 장소 번호 및 제목
            if (isTarget) {
                sb.append("👉 **").append(i + 1).append(". ").append(place.getTitle()).append("**\n");
            } else {
                sb.append("   ").append(i + 1).append(". ").append(place.getTitle()).append("\n");
            }
            
            // 장소명
            if (isTarget) {
                sb.append("      📍 **").append(place.getPlaceName()).append("**\n");
            } else {
                sb.append("      📍 ").append(place.getPlaceName()).append("\n");
            }
            
            // 시간
            if (place.getStartAt() != null) {
                sb.append("      ⏰ ");
                if (isTarget) sb.append("**");
                sb.append(formatTime(place.getStartAt().toLocalTime()));
                if (place.getEndAt() != null) {
                    sb.append(" - ").append(formatTime(place.getEndAt().toLocalTime()));
                    long duration = java.time.Duration.between(
                        place.getStartAt().toLocalTime(),
                        place.getEndAt().toLocalTime()
                    ).toMinutes();
                    sb.append(" (").append(duration).append(" min)");
                }
                if (isTarget) sb.append("**");
                sb.append("\n");
            }
            
            // 주소
            if (place.getAddress() != null && !place.getAddress().isEmpty()) {
                if (isTarget) {
                    sb.append("      🏠 **").append(place.getAddress()).append("**\n");
                } else {
                    sb.append("      🏠 ").append(place.getAddress()).append("\n");
                }
            }
            
            // 예상 비용
            if (place.getExpectedCost() != null && place.getExpectedCost().longValue() > 0) {
                if (isTarget) {
                    sb.append("      💰 **₩").append(String.format("%,d", place.getExpectedCost().longValue())).append("**\n");
                } else {
                    sb.append("      💰 ₩").append(String.format("%,d", place.getExpectedCost().longValue())).append("\n");
                }
            }
            
            sb.append("\n");
        }
        
        return sb.toString();
    }

    /**
     * LLM으로 한 줄 요약 생성
     * "You will visit [placeName] on Day [X] as the [Y]th stop."
     */
    private String generatePlaceSummary(com.example.demo.planner.plan.dto.response.PlacePosition position) {
        String prompt = String.format(
            "Return exactly one short English sentence describing: " +
            "'You will visit %s on Day %d as the %s stop.' " +
            "No lists, no explanation, no markdown except plain text. " +
            "Use ordinal numbers correctly (1st, 2nd, 3rd, 4th, etc.).",
            position.getPlaceName(),
            position.getDayIndex(),
            getOrdinal(position.getOrder())
        );
        
        return chatClient.prompt()
            .user(prompt)
            .call()
            .content();
    }

    /**
     * 숫자를 서수(ordinal)로 변환 (1st, 2nd, 3rd, 4th...)
     */
    private String getOrdinal(int number) {
        if (number % 100 >= 11 && number % 100 <= 13) {
            return number + "th";
        }
        switch (number % 10) {
            case 1: return number + "st";
            case 2: return number + "nd";
            case 3: return number + "rd";
            default: return number + "th";
        }
    }

    /**
     * 시간대 일정 렌더링
     */
    private String formatTimeRangeSchedule(String timeRange, java.util.List<com.example.demo.planner.plan.dto.entity.PlanPlace> places) {
        StringBuilder sb = new StringBuilder();
        
        // 시간대 헤더
        String rangeDisplay = getTimeRangeDisplay(timeRange);
        sb.append("🗓️ **").append(rangeDisplay).append(" Schedule**\n\n");
        
        // 장소 목록
        for (int i = 0; i < places.size(); i++) {
            var place = places.get(i);
            
            // 장소 번호 및 제목
            sb.append("   **").append(i + 1).append(". ").append(place.getTitle()).append("**\n");
            
            // 장소명
            sb.append("      📍 ").append(place.getPlaceName()).append("\n");
            
            // 시간
            if (place.getStartAt() != null) {
                sb.append("      ⏰ ");
                sb.append(formatTime(place.getStartAt().toLocalTime()));
                if (place.getEndAt() != null) {
                    sb.append(" - ").append(formatTime(place.getEndAt().toLocalTime()));
                    long duration = java.time.Duration.between(
                        place.getStartAt().toLocalTime(),
                        place.getEndAt().toLocalTime()
                    ).toMinutes();
                    sb.append(" (").append(duration).append(" min)");
                }
                sb.append("\n");
            }
            
            // 주소
            if (place.getAddress() != null && !place.getAddress().isEmpty()) {
                sb.append("      🏠 ").append(place.getAddress()).append("\n");
            }
            
            // 예상 비용
            if (place.getExpectedCost() != null && place.getExpectedCost().longValue() > 0) {
                sb.append("      💰 ₩").append(String.format("%,d", place.getExpectedCost().longValue())).append("\n");
            }
            
            sb.append("\n");
        }
        
        return sb.toString();
    }

    /**
     * 시간대 표시명 반환
     */
    private String getTimeRangeDisplay(String timeRange) {
        switch (timeRange.toLowerCase()) {
            case "morning": return "Morning (05:00 - 11:00)";
            case "lunch": return "Lunch (11:00 - 15:00)";
            case "evening": return "Evening (17:00 - 23:59)";
            default: return timeRange;
        }
    }

    /**
     * LLM으로 시간대 요약 생성
     */
    private String generateTimeRangeSummary(String timeRange, int count) {
        String prompt = String.format(
            "Return exactly one short English sentence like: " +
            "'Here are your %s plans' or 'You have %d %s activities scheduled.' " +
            "No lists, no explanation, no markdown except plain text.",
            timeRange,
            count,
            timeRange
        );
        
        return chatClient.prompt()
            .user(prompt)
            .call()
            .content();
    }

    /**
     * 사용자 메시지를 받아 적절한 Tool을 실행하고 응답 생성
     */
    public String chat(String userMessage, Long userId) {
        String systemPrompt = """
            당신은 서울 여행 계획 도우미입니다.

            중요: 현재 사용자 ID는 %d입니다. 모든 Tool 호출 시 반드시 이 userId를 사용하세요.

            가능한 기능:
            1. 여행 계획 생성: createPlan(userId=%d, days=X, budget=Y)
            2. 사용자의 현재 계획 조회: getMyPlan(userId=%d) - "내 계획", "show my plan" 등
            3. 특정 계획 조회: getPlan(planId=X) - planId를 알고 있을 때만

            필수 규칙:
            - 모든 응답은 반드시 영어로만 작성하세요
            - 사용자가 "내 계획" 또는 "my plan"을 요청하면 반드시 getMyPlan(userId=%d)을 사용하세요
            - planId 없이 계획 조회 시에는 getPlan이 아닌 getMyPlan을 사용하세요
            - Tool을 사용하여 데이터베이스와 상호작용하세요
            - 친절하고 도움이 되는 태도를 유지하세요

            PlanDay 생성/이동 정책 (중요 - 반드시 준수):
            - dayIndex를 지정하지 않으면 자동으로 순차 생성됩니다 (1, 2, 3...)
            - 특정 일차를 생성하거나 이동할 때, 현재 계획 기간을 초과하는 경우:
              1. 먼저 preview API를 호출하여 확장 필요 여부와 예상 endDate를 확인
              2. 사용자에게 "여행 기간이 X일에서 Y일로 확장됩니다. 진행하시겠습니까?"와 같이 물어봄
              3. 사용자가 승인하면 confirm=true로 실제 API 호출
              4. 사용자가 거부하면 작업 취소

            예시 흐름:
            - 사용자: "5일차 추가해줘" (현재 3일 계획)
            - Agent: previewDayCreation(planId=1, dayIndex=5) 호출
            - 결과: requiresExtension=true, newEndDate=2025-12-09
            - Agent: "여행 기간이 3일에서 5일로 확장됩니다 (종료일: 12월 9일). 진행하시겠습니까?"
            - 사용자: "네" → createDay(planId=1, dayIndex=5, confirm=true)

            확장이 필요 없는 경우(현재 기간 내):
            - preview 없이 바로 실행 가능 (confirm 불필요)

            사용자 요청 처리:
            - "계획 만들어줘" → createPlan(userId=%d, days=X, budget=Y) 호출
            - "내 계획 보여줘", "show my plan" → getMyPlan(userId=%d) 호출
            - "계획 #5 보여줘" → getPlan(planId=5) 호출
            - "X일차 추가해줘" → previewDayCreation → 사용자 확인 → createDay(confirm=true)
            - "Day를 Y일차로 이동" → previewDayMove → 사용자 확인 → moveDay(confirm=true)
            """.formatted(userId, userId, userId, userId, userId, userId, userId);

        try {
            String response = chatClient.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .tools(new PlanTools())
                .call()
                .content();

            log.info("PlanAgent response: {}", response);

            if (response == null || response.trim().isEmpty()) {
                log.warn("Empty response from LLM, returning default message");
                return "I couldn't generate a proper response. Please try again.";
            }

            return response;

        } catch (Exception e) {
            log.error("Error in PlanAgent", e);
            return "Sorry, I encountered an error: " + e.getMessage();
        }
    }

    /**
     * Plan 관련 Tools
     */
    class PlanTools {

        @Tool(description = """
            서울 여행 계획을 새로 생성합니다.
            사용자가 새로운 여행 계획을 만들고 싶을 때 이 Tool을 호출하세요.
            파라미터:
            - userId: 사용자 ID (필수)
            - days: 여행 일수 (필수)
            - budget: 예산 (원화, 선택)
            - startDate: 여행 시작일 (YYYY-MM-DD, 선택)

            반환: 생성된 계획의 ID와 요약 정보
            """)
        public String createPlan(
                @ToolParam(description = "사용자 ID") Long userId,
                @ToolParam(description = "여행 일수 (예: 3, 5, 7)") Integer days,
                @ToolParam(description = "예산 (원화, 예: 500000)") Integer budget,
                @ToolParam(description = "여행 시작일 (YYYY-MM-DD)") String startDateStr) {

            log.info("Tool called: createPlan(userId={}, days={}, budget={})", userId, days, budget);

            try {
                BigDecimal budgetDecimal = budget != null ? new BigDecimal(budget) : new BigDecimal("500000");
                LocalDate startDate = startDateStr != null ? LocalDate.parse(startDateStr) : LocalDate.now();

                Plan plan = planService.createPlanWithSampleData(userId, days, budgetDecimal, startDate);

                return String.format("""
                    ✅ Travel plan created successfully!

                    Plan ID: #%d
                    Duration: %s ~ %s (%d days)
                    Budget: ₩%,d
                    Sample places: %d locations created

                    Your Seoul adventure is ready! Each day includes morning and afternoon activities.
                    """, plan.getId(), plan.getStartDate(), plan.getEndDate(), days, budgetDecimal.longValue(), days * 2);

            } catch (Exception e) {
                log.error("Error creating plan", e);
                return "Failed to create plan: " + e.getMessage();
            }
        }

        @Tool(description = """
            사용자의 현재 활성화된 여행 계획을 조회합니다.
            사용자가 "내 계획 보여줘", "show my plan", "현재 여행" 등으로 요청할 때 사용하세요.

            파라미터:
            - userId: 사용자 ID (필수)

            반환: 현재 활성 계획의 상세 정보
            """)
        public String getMyPlan(@ToolParam(description = "사용자 ID") Long userId) {
            log.info("Tool called: getMyPlan(userId={})", userId);

            try {
                Plan plan = planService.findActiveByUserId(userId);
                if (plan == null) {
                    return """
                        📋 No active travel plan found.

                        Would you like to create a new travel plan? Just let me know:
                        - Where you want to go
                        - How many days
                        - Your budget (optional)
                        """;
                }

                return String.format("""
                    📋 Your Active Travel Plan

                    Plan ID: #%d
                    Duration: %s ~ %s
                    Budget: ₩%,d
                    Status: Active

                    Need to see specific days? Ask me "show day 1" or "show day 2"!
                    """, plan.getId(), plan.getStartDate(), plan.getEndDate(),
                    plan.getBudget().longValue());

            } catch (Exception e) {
                log.error("Error getting my plan", e);
                return "Failed to get your plan: " + e.getMessage();
            }
        }

        @Tool(description = """
            특정 여행 계획의 상세 정보를 조회합니다.
            planId를 알고 있을 때만 사용하세요.

            파라미터:
            - planId: 조회할 계획의 ID (필수)

            반환: 계획의 상세 정보
            """)
        public String getPlan(@ToolParam(description = "계획 ID") Long planId) {
            log.info("Tool called: getPlan(planId={})", planId);

            try {
                Plan plan = planService.findById(planId);
                if (plan == null) {
                    return "Plan not found with ID: " + planId;
                }

                return String.format("""
                    📋 Plan Details:

                    Plan ID: #%d
                    Duration: %s ~ %s
                    Budget: ₩%,d
                    Status: %s
                    """, plan.getId(), plan.getStartDate(), plan.getEndDate(),
                    plan.getBudget().longValue(),
                    plan.getIsEnded() ? "Completed" : "Active");

            } catch (Exception e) {
                log.error("Error getting plan", e);
                return "Failed to get plan: " + e.getMessage();
            }
        }

        @Tool(description = """
            PlanDay 생성 시 여행 기간 확장이 필요한지 미리 확인합니다.
            사용자가 현재 계획 기간을 초과하는 Day를 추가하려 할 때 반드시 먼저 이 Tool을 호출하세요.

            파라미터:
            - planId: 여행 계획 ID (필수)
            - dayIndex: 생성하려는 일차 (필수)

            반환: 확장 필요 여부, 예상 종료일, 현재 최대 일차
            """)
        public String previewDayCreation(
                @ToolParam(description = "여행 계획 ID") Long planId,
                @ToolParam(description = "생성하려는 일차 (예: 5)") Integer dayIndex) {

            log.info("Tool called: previewDayCreation(planId={}, dayIndex={})", planId, dayIndex);

            try {
                var preview = planService.createDayPreview(planId, dayIndex);

                if (preview.isRequiresExtension()) {
                    return String.format("""
                        ⚠️ 여행 기간 확장이 필요합니다

                        현재 최대 일차: %d일차
                        요청 일차: %d일차
                        예상 종료일: %s

                        사용자에게 다음과 같이 물어보세요:
                        "여행 기간을 %d일차까지 확장하시겠습니까? (종료일: %s)"

                        승인 시: createDay tool을 confirm=true로 호출
                        거부 시: 작업 취소
                        """,
                        preview.getCurrentMaxIndex(),
                        preview.getRequestedToIndex(),
                        preview.getNewEndDate(),
                        preview.getRequestedToIndex(),
                        preview.getNewEndDate());
                } else {
                    return String.format("""
                        ✅ 확장 불필요 - 바로 생성 가능

                        현재 최대 일차: %d일차
                        요청 일차: %d일차

                        createDay tool을 바로 호출하세요 (confirm 불필요).
                        """,
                        preview.getCurrentMaxIndex(),
                        preview.getRequestedToIndex());
                }

            } catch (Exception e) {
                log.error("Error in previewDayCreation", e);
                return "Failed to preview day creation: " + e.getMessage();
            }
        }

        @Tool(description = """
            PlanDay 이동 시 여행 기간 확장이 필요한지 미리 확인합니다.
            Day를 현재 최대 일차보다 뒤로 이동할 때 반드시 먼저 이 Tool을 호출하세요.

            파라미터:
            - dayId: 이동할 Day의 ID (필수)
            - toIndex: 목표 일차 (필수)

            반환: 확장 필요 여부, 예상 종료일, 현재 최대 일차
            """)
        public String previewDayMove(
                @ToolParam(description = "이동할 Day의 ID") Long dayId,
                @ToolParam(description = "목표 일차 (예: 5)") Integer toIndex) {

            log.info("Tool called: previewDayMove(dayId={}, toIndex={})", dayId, toIndex);

            try {
                var preview = planService.movePreview(dayId, toIndex);

                if (preview.isRequiresExtension()) {
                    return String.format("""
                        ⚠️ 여행 기간 확장이 필요합니다

                        현재 최대 일차: %d일차
                        목표 일차: %d일차
                        예상 종료일: %s

                        사용자에게 다음과 같이 물어보세요:
                        "Day를 %d일차로 이동하려면 여행 기간 확장이 필요합니다 (종료일: %s). 진행하시겠습니까?"

                        승인 시: moveDay tool을 confirm=true로 호출
                        거부 시: 작업 취소
                        """,
                        preview.getCurrentMaxIndex(),
                        preview.getRequestedToIndex(),
                        preview.getNewEndDate(),
                        preview.getRequestedToIndex(),
                        preview.getNewEndDate());
                } else {
                    return String.format("""
                        ✅ 확장 불필요 - 바로 이동 가능

                        현재 최대 일차: %d일차
                        목표 일차: %d일차

                        moveDay tool을 바로 호출하세요 (confirm 부8필요).
                        """,
                        preview.getCurrentMaxIndex(),
                        preview.getRequestedToIndex());
                }

            } catch (Exception e) {
                log.error("Error in previewDayMove", e);
                return "Failed to preview day move: " + e.getMessage();
            }
        }
    }
}
