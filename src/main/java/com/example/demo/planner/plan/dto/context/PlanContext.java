package com.example.demo.planner.plan.dto.context;

import java.util.List;

import com.example.demo.planner.plan.dto.entity.Plan;
import com.example.demo.planner.plan.dto.entity.PlanPlace;
import com.example.demo.planner.plan.dto.response.PlanDayWithPlaces;

import lombok.Builder;
import lombok.Getter;

/**
 * SmartPlanAgent가 LLM에게 전달할 여행 일정 컨텍스트
 * 현재 활성화된 Plan + 모든 Day + Place 정보 포함
 */
@Getter
@Builder
public class PlanContext {

    private Long userId;  // ✅ NEW: Tool에서 userId 조회용
    private Plan activePlan;
    private List<PlanDayWithPlaces> allDays;

    /**
     * LLM이 읽기 쉬운 형태로 일정 전체를 포맷팅
     */
    public String toContextString() {
        if (!hasActivePlan()) {
            return "(활성화된 여행 일정 없음)";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("=== 여행 일정 (%s ~ %s) ===\n\n",
            activePlan.getStartDate(),
            activePlan.getEndDate()));

        if (allDays == null || allDays.isEmpty()) {
            sb.append("(일정 없음)");
            return sb.toString();
        }

        for (int i = 0; i < allDays.size(); i++) {
            PlanDayWithPlaces dayData = allDays.get(i);
            sb.append(String.format("📅 %d일차 (%s)\n",
                i + 1,
                dayData.getDay().getPlanDate()));

            List<PlanPlace> places = dayData.getPlaces();
            if (places == null || places.isEmpty()) {
                sb.append("  (일정 없음)\n\n");
                continue;
            }

            for (int j = 0; j < places.size(); j++) {
                PlanPlace place = places.get(j);
                sb.append(String.format("  %d. %s\n", j + 1, place.getPlaceName()));
                // sb.append(String.format("     ⏰ %s ~ %s\n",
                //     place.getStartAt() != null ? place.getStartAt() : "미정",
                //     place.getEndAt() != null ? place.getEndAt() : "미정"));
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * 토큰 절약용 요약 버전
     */
    public String toSummary() {
        if (!hasActivePlan()) return "(일정 없음)";

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%d일 여행\n", allDays != null ? allDays.size() : 0));

        if (allDays != null) {
            for (int i = 0; i < allDays.size(); i++) {
                var day = allDays.get(i);
                int placeCount = day.getPlaces() != null ? day.getPlaces().size() : 0;
                sb.append(String.format("Day%d: %d개 일정\n", i + 1, placeCount));
            }
        }

        return sb.toString();
    }

    public boolean hasActivePlan() {
        return activePlan != null;
    }

    public boolean hasDays() {
        return allDays != null && !allDays.isEmpty();
    }

    /**
     * LLM Full-Reasoning용 JSON 변환
     * LLM이 직접 reasoning할 수 있도록 구조화된 JSON 제공
     */
    public String toJson() {
        if (!hasActivePlan()) {
            return "{\"plan\": null, \"days\": []}";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");

        // Plan 기본 정보
        sb.append("  \"plan\": {\n");
        sb.append(String.format("    \"id\": %d,\n", activePlan.getId()));
        sb.append(String.format("    \"title\": \"%s\",\n",
            activePlan.getTitle() != null ? activePlan.getTitle() : "여행 일정"));
        sb.append(String.format("    \"startDate\": \"%s\",\n", activePlan.getStartDate()));
        sb.append(String.format("    \"endDate\": \"%s\"\n", activePlan.getEndDate()));
        sb.append("  },\n");

        // Days 정보
        sb.append("  \"days\": [\n");

        if (allDays != null && !allDays.isEmpty()) {
            for (int i = 0; i < allDays.size(); i++) {
                PlanDayWithPlaces dayData = allDays.get(i);
                sb.append("    {\n");
                sb.append(String.format("      \"day\": %d,\n", i + 1));
                sb.append(String.format("      \"date\": \"%s\",\n", dayData.getDay().getPlanDate()));
                sb.append("      \"items\": [\n");

                List<PlanPlace> places = dayData.getPlaces();
                if (places != null && !places.isEmpty()) {
                    for (int j = 0; j < places.size(); j++) {
                        PlanPlace place = places.get(j);
                        sb.append("        {\n");
                        sb.append(String.format("          \"index\": %d,\n", j + 1));
                        sb.append(String.format("          \"name\": \"%s\",\n",
                            place.getPlaceName() != null ? place.getPlaceName() : "미정"));
                        // sb.append(String.format("          \"startTime\": \"%s\",\n",
                        //     place.getStartAt() != null ? place.getStartAt() : "미정"));
                        // sb.append(String.format("          \"endTime\": \"%s\"\n",
                        //     place.getEndAt() != null ? place.getEndAt() : "미정"));
                        sb.append("        }");
                        if (j < places.size() - 1) sb.append(",");
                        sb.append("\n");
                    }
                }

                sb.append("      ]\n");
                sb.append("    }");
                if (i < allDays.size() - 1) sb.append(",");
                sb.append("\n");
            }
        }

        sb.append("  ]\n");
        sb.append("}");

        return sb.toString();
    }

    /**
     * 빈 PlanContext 생성
     */
    public static PlanContext empty() {
        return PlanContext.builder().build();
    }
}
