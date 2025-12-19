package com.example.demo.planner.plan.agent.tools;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.planner.plan.agent.TravelPlanAgent;
import com.example.demo.planner.plan.agent.common.PlanToolSupport;
import com.example.demo.planner.plan.dao.PlanDao;
import com.example.demo.planner.plan.dao.PlanDayDao;
import com.example.demo.planner.plan.dao.PlanPlaceDao;
import com.example.demo.planner.plan.dto.entity.GeneratedTravelPlan;
import com.example.demo.planner.plan.dto.entity.Plan;
import com.example.demo.planner.plan.dto.entity.PlanDay;
import com.example.demo.planner.plan.dto.entity.PlanPlace;
import com.example.demo.planner.plan.service.PlanSnapshotService;
import com.example.demo.planner.plan.service.TravelPlanSaveService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component("planCreateTools")
@RequiredArgsConstructor
@Slf4j
public class PlanCreateTools {

    private final TravelPlanAgent travelPlanAgent;
    private final TravelPlanSaveService travelPlanSaveService;
    private final PlanSnapshotService planSnapshotService;
    private final PlanPlaceDao planPlaceDao;
    private final PlanDayDao planDayDao;
    private final PlanDao planDao;
    private final PlanToolSupport support;

    @Tool(name = "createSeoulTravelPlan", description = """
            서울 여행 일정을 자동으로 생성합니다.
            사용자가 "N박N일 계획 짜줘", "여행 일정 만들어줘"라고 요청할 때 사용하세요.
            사용자의 요청에서 다음 정보를 추출하여 전달하세요:
            - duration: 여행 기간 (필수)
            - style: 여행 스타일 (선택)
            - location: 선호 지역 (선택)
            - pace: 일정 강도 (선택)
            - startDateText: 여행 시작 시점 (선택)

            예시:
            "3일 kpop 강남 여행"
            → duration=3, style="kpop", location="강남", pace=null

            "서울 당일치기"
            → duration=1, style=null, location=null, pace=null

            "5일 힐링 여행 빡빡하게"
            → duration=5, style="힐링", location=null, pace="빡빡"

            주의:
            - 처음부터 새로운 여행 계획을 생성할 때만 사용하세요
            - 특정 일차만 다시 만들고 싶다면 regenerateDay를 사용하세요
            - 여행 기간이 없다면 사용자에게 다시 물어보세요
            """)
    public String createTravelPlan(
            @ToolParam(description = "여행 기간(일). 반드시 사용자 발화에 명시되어야 함", required = true) Integer duration,
            @ToolParam(description = "여행 스타일. 예: 'kpop', '힐링'. 없으면 null", required = false) String style,
            @ToolParam(description = "선호 지역. 예: '강남', '강남, 홍대'. 없으면 null", required = false) String location,
            @ToolParam(description = "일정 강도. '빡빡', '널널'. 없으면 null", required = false) String pace,
            @ToolParam(description = "사용자가 말한 여행 시작 시점의 원문 표현 그대로. 예: '3일뒤', '다음주 월요일'", required = false) String startDateText,
            ToolContext toolContext) {

        log.info("🗼 [Tool] 서울 여행 일정 생성 시작");
        log.info("   파라미터: duration={}, style={}, location={}, pace={}",
                duration, style, location, pace);

        try {
            // 1. Validation
            if (duration == null || duration <= 0) {
                return "여행 기간을 지정해주세요. 며칠 동안 여행하실 예정인가요?";
            }

            Long userId = (Long) toolContext.getContext().get("userId");
            String conversationId = String.valueOf(
                    toolContext.getContext().get("conversationId"));

            // 2. 새 일정 생성 = conversation 상태 리셋
            support.clear(conversationId);
            log.info("🧹 [초기화] 새 일정 생성으로 conversation 상태 초기화");

            // 3. Agent 호출
            GeneratedTravelPlan plan = travelPlanAgent.createSeoulTravelPlanStructured(
                    duration, style, location, pace, startDateText);

            if (plan.days().isEmpty()) {
                return "일정 생성에 실패했습니다. 다시 시도해주세요.";
            }

            log.info("일정 생성 완료 - {}일", plan.days().size());

            // 4. DB 저장
            Long planId = travelPlanSaveService.save(userId, plan, new ObjectMapper());

            // 5. conversationId ↔ planId 연결 (가장 중요)
            support.setPlanId(conversationId, planId);
            log.info("conversationId={} → planId={} 연결 완료", conversationId, planId);

            // 6. 스냅샷 정리 & 저장
            support.deleteAllSnapshot(userId);
            Integer versionNo = support.saveSnapshot(planId);

            // 7. 렌더링
            return renderPlan(plan, versionNo);

        } catch (Exception e) {
            log.error("서울 여행 일정 생성 실패", e);
            return "일정 생성 중 오류가 발생했습니다: " + e.getMessage();
        }
    }

    @Transactional
    @Tool(description = """
            특정 일차의 일정만 다시 생성합니다.

            사용 예시:
            - "2일차 일정이 마음에 안 들어. 다시 짜줘"
            - "1일차만 카페 위주로 바꿔줘"
            - "마지막 날 다시 만들어줘"

            파라미터:
            - dayIndex: 재생성할 일차 (필수, 1부터 시작)
            - style: 새로운 스타일 (선택, 예: "kpop", "힐링")
            - pace: 새로운 강도 (선택, "빡빡"/"널널")
            - location: 선호 지역 (선택, 예: "강남")

            주의:
            - 해당 일차의 기존 일정은 완전히 삭제됩니다
            - 다른 날짜는 영향 받지 않습니다
            - 버전이 증가합니다 (rollback 가능)
            """)
    public String regenerateDay(
            @ToolParam(description = "재생성할 일차 (1부터 시작)") int dayIndex,
            @ToolParam(description = "여행 스타일. 없으면 null", required = false) String style,
            @ToolParam(description = "일정 강도. 없으면 null", required = false) String pace,
            @ToolParam(description = "선호 지역. 없으면 null", required = false) String location,
            ToolContext toolContext) {

        String conversationId = String.valueOf(toolContext.getContext().get("conversationId"));
        Long planId = support.getPlanId(conversationId);

        log.info("🔄 [Tool] regenerateDay: planId={}, dayIndex={}, style={}, pace={}",
                planId, dayIndex, style, pace);

        try {
            // 1. 기존 Plan 조회
            Plan plan = planDao.selectPlanById(planId);
            List<PlanDay> allDays = planDayDao.selectPlanDaysByPlanId(planId);

            // 2. Validation
            if (dayIndex < 1 || dayIndex > allDays.size()) {
                return String.format("❌ %d일차는 존재하지 않습니다. (전체 %d일)",
                        dayIndex, allDays.size());
            }

            // 3. 해당 일차의 PlanDay
            PlanDay targetDay = allDays.stream()
                    .filter(d -> d.getDayIndex() == dayIndex)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(dayIndex + "일차를 찾을 수 없습니다."));

            // 4. 기존 Places 삭제
            List<PlanPlace> oldPlaces = planPlaceDao.selectPlanPlacesByPlanDayId(targetDay.getId());
            for (PlanPlace place : oldPlaces) {
                planPlaceDao.deletePlanPlaceById(place.getId());
            }
            log.info("기존 {}일차 장소 {}개 삭제 완료", dayIndex, oldPlaces.size());

            // 5. 다른 날짜 사용 중인 장소 수집 (중복 방지)
            Set<Long> usedContentIds = new HashSet<>();
            for (PlanDay day : allDays) {
                if (day.getDayIndex() == dayIndex)
                    continue; // 재생성 대상 제외

                List<PlanPlace> places = planPlaceDao.selectPlanPlacesByPlanDayId(day.getId());
                for (PlanPlace p : places) {
                    usedContentIds.add(p.getId());
                }
            }
            log.info("다른 날짜 사용 중인 장소: {}개", usedContentIds.size());

            // 6. GeneratedTravelPlan 구성 (Agent 호출용)
            GeneratedTravelPlan existingPlan = new GeneratedTravelPlan(
                    allDays.size(),
                    "보통", // pace는 파라미터로 받으므로 여기선 중요하지 않음
                    List.of(),
                    plan.getStartDate(),
                    plan.getEndDate());

            // 7. Agent로 새 일정 생성
            List<GeneratedTravelPlan.GeneratedPlace> newPlaces = travelPlanAgent.regenerateSingleDay(
                    dayIndex,
                    existingPlan,
                    usedContentIds,
                    style,
                    pace,
                    location);

            if (newPlaces.isEmpty()) {
                return "❌ 일정 생성에 실패했습니다. 다시 시도해주세요.";
            }

            log.info("새로운 {}일차 일정 생성: {}개 장소", dayIndex, newPlaces.size());

            // 8. ✅ Service 호출 (깔끔!)
            travelPlanSaveService.saveSingleDay(targetDay.getId(), newPlaces);

            // 9. 스냅샷 저장
            Integer version = support.saveSnapshot(planId);

            // 10. 렌더링
            return renderRegeneratedDay(dayIndex, newPlaces, version);

        } catch (Exception e) {
            log.error("❌ {}일차 재생성 실패", dayIndex, e);
            return String.format("❌ %d일차 재생성 중 오류: %s", dayIndex, e.getMessage());
        }
    }

    /**
     * 재생성된 일차 렌더링
     */
    private String renderRegeneratedDay(int dayIndex,
            List<GeneratedTravelPlan.GeneratedPlace> places,
            Integer version) {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("✅ %d일차 일정을 재생성했습니다. (버전: %d)\n\n", dayIndex, version));
        sb.append(String.format("=== Day %d ===\n", dayIndex));

        for (var p : places) {
            sb.append("  • ").append(p.title()).append("\n");
        }

        return sb.toString();
    }

    /**
     * DB에서 조회한 Plan을 GeneratedTravelPlan으로 변환
     */
    private GeneratedTravelPlan buildGeneratedPlanFromDb(Plan plan, List<PlanDay> days) {
        // 간단한 변환 (Agent가 필요한 최소 정보만)
        return new GeneratedTravelPlan(
                days.size(),
                "보통", // pace는 중요하지 않음 (파라미터로 받음)
                List.of(), // days는 빈 리스트 (어차피 안 씀)
                plan.getStartDate(),
                plan.getEndDate());
    }

    /**
     * GeneratedTravelPlan 렌더링
     */
    private String renderPlan(GeneratedTravelPlan plan, Integer vesionNo) {
        StringBuilder sb = new StringBuilder();
        sb.append("version: " + vesionNo);
        sb.append(" \n");
        sb.append("=== Seoul Travel Itinerary ===\n");
        sb.append("📅 ")
                .append(plan.startDate())
                .append(" ~ ")
                .append(plan.endDate())
                .append("\n");
        sb.append("⏱️ Pace: ").append(plan.pace()).append("\n\n");

        for (var day : plan.days()) {
            sb.append("Day ").append(day.dayIndex()).append("\n");

            for (var p : day.places()) {
                sb.append("  ")
                        // .append(p.startAt().toLocalTime())
                        // .append("-")
                        // .append(p.endAt().toLocalTime())
                        // .append(" ")
                        .append(p.title())
                        .append("\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }
}