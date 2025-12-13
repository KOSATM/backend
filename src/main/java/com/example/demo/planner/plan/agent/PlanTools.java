package com.example.demo.planner.plan.agent;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.common.naver.dto.LocalItem;
import com.example.demo.planner.plan.dao.PlanDao;
import com.example.demo.planner.plan.dao.PlanDayDao;
import com.example.demo.planner.plan.dao.PlanPlaceDao;
import com.example.demo.planner.plan.dto.entity.Plan;
import com.example.demo.planner.plan.dto.entity.PlanDay;
import com.example.demo.planner.plan.dto.entity.PlanPlace;
import com.example.demo.planner.plan.dto.entity.PlanSnapshot;
import com.example.demo.planner.plan.dto.response.PlanSnapshotContent;
import com.example.demo.planner.plan.service.PlanSnapshotService;
import com.example.demo.planner.plan.service.action.PlanAddAction;
import com.example.demo.planner.plan.service.action.PlanDeleteAction;
import com.example.demo.planner.plan.service.action.PlanModifyAction;
import com.example.demo.planner.plan.service.action.PlanSwapAction;
import com.example.demo.planner.plan.service.create.PlanService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * SmartPlanAgent Tool Functions
 * - Spring AI Function Calling 진입점
 * - Action 기반 서비스로 위임
 * - ThreadLocal로 planId 관리 (LLM이 planId를 알 필요 없음)
 */
@Component("planTools")
@RequiredArgsConstructor
@Slf4j
public class PlanTools {

    private final PlanService planService;

    private final PlanAddAction addAction;
    private final PlanModifyAction modifyAction;
    private final PlanSwapAction swapAction;
    private final PlanDeleteAction deleteAction;
    
    private final PlanDao planDao;
    private final PlanDayDao planDayDao;
    private final PlanPlaceDao planPlaceDao;
    private final PlanSnapshotService planSnapshotService;


    private final ThreadLocal<Long> currentPlanId = new ThreadLocal<>();

    DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void setPlanId(Long planId) {
        currentPlanId.set(planId);
    }

    public void clearPlanId() {
        currentPlanId.remove();
    }

    private Long getPlanId() {
        return currentPlanId.get();
    }

    @Tool(description = "특정 장소를 일정에서 삭제합니다")
    public String deletePlace(String placeName) {
        Long planId = getPlanId();
        log.info("🔧 [Tool] deletePlace: planId={}, placeName={}", planId, placeName);
        try {
            deleteAction.deletePlaceByName(planId, placeName);

            // 스냅샷 저장
            Plan plan = planDao.selectPlanById(planId);
            List<PlanDay> planDays = planDayDao.selectPlanDaysByPlanId(planId);
            List<PlanPlace> planPlaces = planPlaceDao.selectPlanPlacesByPlanId(planId);
            planSnapshotService.savePlanSnapshot(plan, planDays, planPlaces);

            return String.format("✅ '%s' 장소를 일정에서 삭제했습니다.", placeName);
        } catch (IllegalArgumentException e) {
            return String.format("❌ '%s' 장소를 찾을 수 없습니다.", placeName);
        } catch (Exception e) {
            log.error("장소 삭제 실패", e);
            return String.format("❌ 장소 삭제 중 오류 발생: %s", e.getMessage());
        }
    }

    @Tool(description = "같은 날짜 내에서 두 장소의 순서를 교환합니다 (dayIndex는 1부터 시작)")
    public String swapPlaces(int dayIndex, int index1, int index2) {
        Long planId = getPlanId();
        log.info("🔧 [Tool] swapPlaces: planId={}, dayIndex={}, index1={}, index2={}", planId, dayIndex, index1, index2);
        try {
            swapAction.swapPlacesInSameDay(planId, dayIndex, index1, index2);

            // 스냅샷 저장
            Plan plan = planDao.selectPlanById(planId);
            List<PlanDay> planDays = planDayDao.selectPlanDaysByPlanId(planId);
            List<PlanPlace> planPlaces = planPlaceDao.selectPlanPlacesByPlanId(planId);
            planSnapshotService.savePlanSnapshot(plan, planDays, planPlaces);

            return String.format("✅ %d일차의 %d번째와 %d번째 장소 순서를 교환했습니다.", dayIndex, index1, index2);
        } catch (Exception e) {
            log.error("장소 순서 교환 실패", e);
            return String.format("❌ 장소 순서 교환 중 오류 발생: %s", e.getMessage());
        }
    }

    @Tool(description = "서로 다른 날짜 간 장소를 교환합니다 (dayIndex는 1부터 시작)")
    public String swapPlacesBetweenDays(int day1, int index1, int day2, int index2) {
        Long planId = getPlanId();
        log.info("🔧 [Tool] swapPlacesBetweenDays: planId={}, day1={}, index1={}, day2={}, index2={}", planId, day1, index1, day2, index2);
        try {
            swapAction.swapPlacesBetweenDays(planId, day1, index1, day2, index2);

            // 스냅샷 저장
            Plan plan = planDao.selectPlanById(planId);
            List<PlanDay> planDays = planDayDao.selectPlanDaysByPlanId(planId);
            List<PlanPlace> planPlaces = planPlaceDao.selectPlanPlacesByPlanId(planId);
            planSnapshotService.savePlanSnapshot(plan, planDays, planPlaces);
            
            return String.format("✅ %d일차의 %d번째 장소와 %d일차의 %d번째 장소를 교환했습니다.", day1, index1, day2, index2);
        } catch (Exception e) {
            log.error("날짜 간 장소 교환 실패", e);
            return String.format("❌ 장소 교환 중 오류 발생: %s", e.getMessage());
        }
    }

    @Tool(description = "기존 장소를 다른 장소로 교체합니다 (첫 번째 검색 결과 자동 선택)")
    public String replacePlace(String oldPlaceName, String newPlaceName) {
        Long planId = getPlanId();
        log.info("🔧 [Tool] replacePlace: planId={}, old={}, new={}", planId, oldPlaceName, newPlaceName);
        try {
            String newName = modifyAction.replacePlaceWithSearch(planId, oldPlaceName, newPlaceName);

            // 스냅샷 저장
            Plan plan = planDao.selectPlanById(planId);
            List<PlanDay> planDays = planDayDao.selectPlanDaysByPlanId(planId);
            List<PlanPlace> planPlaces = planPlaceDao.selectPlanPlacesByPlanId(planId);
            planSnapshotService.savePlanSnapshot(plan, planDays, planPlaces);

            return String.format("✅ '%s'를 '%s'(으)로 변경했습니다.", oldPlaceName, newName);
        } catch (Exception e) {
            log.error("장소 교체 실패", e);
            return String.format("❌ 장소 교체 중 오류 발생: %s", e.getMessage());
        }
    }

    @Tool(description = "네이버에서 장소를 검색하여 여러 후보를 보여줍니다")
    public String searchPlace(String searchQuery) {
        log.info("🔧 [Tool] searchPlace: query={}", searchQuery);
        try {
            var searchResults = addAction.searchNaverLocal(searchQuery);
            if (searchResults.isEmpty()) {
                return String.format("❌ '%s' 검색 결과가 없습니다.", searchQuery);
            }

            int count = Math.min(searchResults.size(), 5);
            StringBuilder result = new StringBuilder();
            result.append(String.format("🔍 '%s' 검색 결과 %d개:\n\n", searchQuery, count));

            for (int i = 0; i < count; i++) {
                LocalItem item = searchResults.get(i);
                result.append(String.format("%d. **%s**\n", i + 1, cleanHtmlTags(item.getTitle())));
                result.append(String.format("   - 카테고리: %s\n", item.getCategory()));
                result.append(String.format("   - 주소: %s\n", item.getRoadAddress()));
                if (i < count - 1) result.append("\n");
            }

            result.append("\n어떤 장소로 하시겠어요? (번호로 선택해주세요)");
            return result.toString();
        } catch (Exception e) {
            log.error("장소 검색 실패", e);
            return String.format("❌ 장소 검색 중 오류: %s", e.getMessage());
        }
    }

    @Tool(description = "검색 결과에서 사용자가 선택한 장소로 교체합니다")
    public String replacePlaceWithSelection(String oldPlaceName, String newPlaceName, int selectedIndex) {
        Long planId = getPlanId();
        log.info("🔧 [Tool] replacePlaceWithSelection: planId={}, old={}, new={}, index={}", planId, oldPlaceName, newPlaceName, selectedIndex);
        try {
            String newName = modifyAction.replacePlaceWithSelection(planId, oldPlaceName, newPlaceName, selectedIndex);

            // 스냅샷 저장
            Plan plan = planDao.selectPlanById(planId);
            List<PlanDay> planDays = planDayDao.selectPlanDaysByPlanId(planId);
            List<PlanPlace> planPlaces = planPlaceDao.selectPlanPlacesByPlanId(planId);
            planSnapshotService.savePlanSnapshot(plan, planDays, planPlaces);

            return String.format("✅ '%s'를 '%s'(으)로 변경했습니다.", oldPlaceName, newName);
        } catch (Exception e) {
            log.error("장소 교체 실패", e);
            return String.format("❌ 장소 교체 중 오류 발생: %s", e.getMessage());
        }
    }

    @Tool(description = "특정 날짜에 새로운 장소를 추가합니다. dayIndex는 1부터 시작 (1일차=1, 2일차=2). 장소명으로 자동 검색하여 추가합니다.")
    public String addPlace(int dayIndex, String placeName, String startTime) {
        Long planId = getPlanId();
        log.info("🔧 [Tool] addPlace: planId={}, dayIndex={}, placeName={}, startTime={}", planId, dayIndex, placeName, startTime);
        try {
            String result = addAction.addPlace(planId, dayIndex, placeName, startTime);

            // 스냅샷 저장
            Plan plan = planDao.selectPlanById(planId);
            List<PlanDay> planDays = planDayDao.selectPlanDaysByPlanId(planId);
            List<PlanPlace> planPlaces = planPlaceDao.selectPlanPlacesByPlanId(planId);
            planSnapshotService.savePlanSnapshot(plan, planDays, planPlaces);

            return String.format("✅ %d일차에 '%s'을(를) 추가했습니다.", dayIndex, result);
        } catch (Exception e) {
            log.error("장소 추가 실패", e);
            return String.format("❌ 장소 추가 중 오류 발생: %s", e.getMessage());
        }
    }

    @Tool(description = "특정 위치에 장소를 삽입하고 이후 일정을 자동으로 조정합니다. dayIndex는 1부터 시작.")
    public String addPlaceAtPosition(int dayIndex, int position, String placeName, Integer duration) {
        Long planId = getPlanId();
        log.info("🔧 [Tool] addPlaceAtPosition: planId={}, dayIndex={}, position={}, placeName={}, duration={}",
                planId, dayIndex, position, placeName, duration);
        try {
            String result = addAction.addPlaceAtPosition(planId, dayIndex, position, placeName, duration);

            // 스냅샷 저장
            Plan plan = planDao.selectPlanById(planId);
            List<PlanDay> planDays = planDayDao.selectPlanDaysByPlanId(planId);
            List<PlanPlace> planPlaces = planPlaceDao.selectPlanPlacesByPlanId(planId);
            planSnapshotService.savePlanSnapshot(plan, planDays, planPlaces);

            return String.format("✅ %d일차 %d번째에 '%s'을(를) 추가했습니다.", dayIndex, position, result);
        } catch (Exception e) {
            log.error("장소 삽입 실패", e);
            return String.format("❌ 장소 삽입 중 오류 발생: %s", e.getMessage());
        }
    }

    @Tool(description = "특정 장소의 시간을 변경합니다")
    public String updatePlaceTime(String placeName, String newTime) {
        Long planId = getPlanId();
        log.info("🔧 [Tool] updatePlaceTime: planId={}, placeName={}, newTime={}", planId, placeName, newTime);
        try {
            modifyAction.updatePlaceTime(planId, placeName, newTime);

            // 스냅샷 저장
            Plan plan = planDao.selectPlanById(planId);
            List<PlanDay> planDays = planDayDao.selectPlanDaysByPlanId(planId);
            List<PlanPlace> planPlaces = planPlaceDao.selectPlanPlacesByPlanId(planId);
            planSnapshotService.savePlanSnapshot(plan, planDays, planPlaces);

            return String.format("✅ '%s'의 시간을 %s(으)로 변경했습니다.", placeName, newTime);
        } catch (Exception e) {
            log.error("시간 변경 실패", e);
            return String.format("❌ 시간 변경 중 오류 발생: %s", e.getMessage());
        }
    }

    @Tool(description = "특정 날짜 전체를 삭제합니다 (dayIndex는 1부터 시작)")
    public String deleteDay(int dayIndex) {
        Long planId = getPlanId();
        log.info("🔧 [Tool] deleteDay: planId={}, dayIndex={}", planId, dayIndex);
        try {
            deleteAction.deleteDay(planId, dayIndex);

            // 스냅샷 저장
            Plan plan = planDao.selectPlanById(planId);
            List<PlanDay> planDays = planDayDao.selectPlanDaysByPlanId(planId);
            List<PlanPlace> planPlaces = planPlaceDao.selectPlanPlacesByPlanId(planId);
            planSnapshotService.savePlanSnapshot(plan, planDays, planPlaces);

            return String.format("✅ %d일차 일정을 삭제했습니다.", dayIndex);
        } catch (Exception e) {
            log.error("날짜 삭제 실패", e);
            return String.format("❌ 날짜 삭제 중 오류 발생: %s", e.getMessage());
        }
    }

    @Tool(description = "두 날짜의 일정 전체를 교환합니다 (dayIndex는 1부터 시작)")
    public String swapDays(int day1, int day2) {
        Long planId = getPlanId();
        log.info("🔧 [Tool] swapDays: planId={}, day1={}, day2={}", planId, day1, day2);
        try {
            swapAction.swapDays(planId, day1, day2);

            // 스냅샷 저장
            Plan plan = planDao.selectPlanById(planId);
            List<PlanDay> planDays = planDayDao.selectPlanDaysByPlanId(planId);
            List<PlanPlace> planPlaces = planPlaceDao.selectPlanPlacesByPlanId(planId);
            planSnapshotService.savePlanSnapshot(plan, planDays, planPlaces);

            return String.format("✅ %d일차와 %d일차 일정을 교환했습니다.", day1, day2);
        } catch (Exception e) {
            log.error("날짜 교환 실패", e);
            return String.format("❌ 날짜 교환 중 오류 발생: %s", e.getMessage());
        }
    }

    @Tool(description = "여행 기간을 늘립니다 (날짜 추가)")
    public String extendPlan(int extraDays) {
        Long planId = getPlanId();
        log.info("🔧 [Tool] extendPlan: planId={}, extraDays={}", planId, extraDays);
        try {
            addAction.extendPlan(planId, extraDays);

            // 스냅샷 저장
            Plan plan = planDao.selectPlanById(planId);
            List<PlanDay> planDays = planDayDao.selectPlanDaysByPlanId(planId);
            List<PlanPlace> planPlaces = planPlaceDao.selectPlanPlacesByPlanId(planId);
            planSnapshotService.savePlanSnapshot(plan, planDays, planPlaces);
            
            return String.format("✅ 여행을 %d일 연장했습니다.", extraDays);
        } catch (Exception e) {
            log.error("일정 확장 실패", e);
            return String.format("❌ 일정 확장 중 오류 발생: %s", e.getMessage());
        }
    }

    @Tool(description = "전체 일정을 완전히 삭제합니다 (Plan + 모든 날짜와 장소 삭제). 중요: 사용자가 명확히 확인한 경우에만 호출하세요!")
    public String deletePlan() {
        Long planId = getPlanId();
        log.info("🔧 [Tool] deletePlan: planId={}", planId);
        try {
            deleteAction.deleteAllDaysAndPlaces(planId);
            return "✅ 전체 일정이 완전히 삭제되었습니다. 새로운 여행 계획을 만들고 싶으시면 말씀해주세요!";
        } catch (Exception e) {
            log.error("전체 일정 삭제 실패", e);
            return String.format("❌ 전체 일정 삭제 중 오류 발생: %s", e.getMessage());
        }
    }

    @Transactional
    @Tool(description = "사용자가 가지고 있는 계획 스냅샷의 바로 이전 버전으로 돌아갑니다.")
    public String rollBack(@ToolParam(description = "사용자 아이디") ToolContext toolContext) {
        try {
            PlanSnapshot planSnapshot = planSnapshotService.getPlanSnapshotsByUserId((Long) toolContext.getContext().get("userId")).get(1);
            PlanSnapshotContent snapshotContent = planService.parseSnapshot(planSnapshot.getSnapshotJson());

            Long planId = getPlanId();
            Plan plan = planDao.selectPlanById(getPlanId());

            List<PlanPlace> existingPlaces = planPlaceDao.selectPlanPlacesByPlanId(planId);
            for (PlanPlace place : existingPlaces) {
                planPlaceDao.deletePlanPlace(place.getId());
            }
            log.info("plan_places 삭제 완료");

            List<PlanDay> existingDays = planDayDao.selectPlanDaysByPlanId(planId);
            for (PlanDay day : existingDays) {
                planDayDao.deletePlanDay(day.getId());
            }
            log.info("plan_days 삭제 완료");

            Plan rollbackPlan = Plan.builder()
                .userId((Long) toolContext.getContext().get("userId"))
                .budget(snapshotContent.getBudget())
                .startDate(LocalDate.parse(snapshotContent.getStartDate(), formatter1))
                .endDate(LocalDate.parse(snapshotContent.getEndDate(), formatter1))
                .createdAt(plan.getCreatedAt())
                .updatedAt(OffsetDateTime.now())
                .build();
            planDao.updatePlan(rollbackPlan);
            log.info("Plan 업데이트 완료");

            Map<String, Long> dateToDayId = new HashMap<>();
            for (int i=0; i<snapshotContent.getDays().size(); i++) {
                PlanSnapshotContent.PlanDay pscDay = snapshotContent.getDays().get(i);

                PlanDay newDay = PlanDay.builder()
                    .planId(planId)
                    .dayIndex(i+1)
                    .title(pscDay.getTitle())
                    .planDate(LocalDate.parse(pscDay.getDate(), formatter1))
                    .build();

                planDayDao.insertPlanDay(newDay);
                dateToDayId.put(pscDay.getDate(), newDay.getId());
            }
            log.info("PlanDays 재생성 완료");

            for (PlanSnapshotContent.PlanDay pscDay : snapshotContent.getDays()) {
                Long dayId = dateToDayId.get(pscDay.getDate());

                for (PlanSnapshotContent.PlanDayItem pscItem : pscDay.getSchedules()) {
                    PlanPlace newPlace = PlanPlace.builder()
                        .dayId(dayId)
                        .title(pscItem.getTitle())
                        .startAt(LocalDateTime.parse(pscItem.getStartAt(), formatter2).atOffset(ZoneOffset.of("+09:00")))
                        .endAt(LocalDateTime.parse(pscItem.getEndAt(), formatter2).atOffset(ZoneOffset.of("+09:00")))
                        .placeName(pscItem.getPlaceName())
                        .address(pscItem.getAddress())
                        .lat(pscItem.getLat())
                        .lng(pscItem.getLng())
                        .expectedCost(pscItem.getExpectedCost())
                        .normalizedCategory(pscItem.getNormalizedCategory())
                        .firstImage(pscItem.getFirstImage())
                        .firstImage2(pscItem.getFirstImage2())
                        .isEnded(pscItem.getIsEnded() == null ? false : pscItem.getIsEnded())
                        .build();
                    
                    planPlaceDao.insertPlanPlace(newPlace);
                }
            }
            log.info("PlanPlaces 재생성 완료");

            List<PlanDay> newDays = planDayDao.selectPlanDaysByPlanId(planId);
            List<PlanPlace> newPlaces = planPlaceDao.selectPlanPlacesByPlanId(planId);
            planSnapshotService.savePlanSnapshot(rollbackPlan, newDays, newPlaces);
            log.info("버전 환원 완료");
            return "이전 버전으로 돌아갔습니다";

        } catch (Exception e) {
            log.error(e.getMessage());
            return String.format("❌ 버전 환원 중 오류 발생: %s", e.getMessage());
        }
    }

    // Helper method
    private String cleanHtmlTags(String text) {
        if (text == null) return null;
        return text.replaceAll("<[^>]*>", "");
    }
}
