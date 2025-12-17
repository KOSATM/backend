// package com.example.demo.planner.plan.agent;

// import java.time.LocalDate;
// import java.time.LocalDateTime;
// import java.time.OffsetDateTime;
// import java.time.ZoneOffset;
// import java.time.format.DateTimeFormatter;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;

// import org.springframework.ai.chat.model.ToolContext;
// import org.springframework.ai.tool.annotation.Tool;
// import org.springframework.ai.tool.annotation.ToolParam;
// import org.springframework.stereotype.Component;
// import org.springframework.transaction.annotation.Transactional;

// import com.example.demo.common.naver.dto.LocalItem;
// import com.example.demo.planner.plan.dao.PlanDao;
// import com.example.demo.planner.plan.dao.PlanDayDao;
// import com.example.demo.planner.plan.dao.PlanPlaceDao;
// import com.example.demo.planner.plan.dao.PlanSnapshotDao;
// import com.example.demo.planner.plan.dto.entity.GeneratedTravelPlan;
// import com.example.demo.planner.plan.dto.entity.Plan;
// import com.example.demo.planner.plan.dto.entity.PlanDay;
// import com.example.demo.planner.plan.dto.entity.PlanPlace;
// import com.example.demo.planner.plan.dto.entity.PlanSnapshot;
// import com.example.demo.planner.plan.dto.response.PlanSnapshotContent;
// import com.example.demo.planner.plan.service.PlanSnapshotService;
// import com.example.demo.planner.plan.service.PlanSnapshotUtility;
// import com.example.demo.planner.plan.service.TravelPlanSaveService;
// import com.example.demo.planner.plan.service.action.PlanAddAction;
// import com.example.demo.planner.plan.service.action.PlanDeleteAction;
// import com.example.demo.planner.plan.service.action.PlanModifyAction;
// import com.example.demo.planner.plan.service.action.PlanSwapAction;
// import com.example.demo.planner.plan.utils.CategoryNames;
// import com.fasterxml.jackson.databind.ObjectMapper;

// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;

// /**
//  * SmartPlanAgent Tool Functions
//  * - Spring AI Function Calling 진입점
//  * - Action 기반 서비스로 위임
//  * - ThreadLocal로 planId 관리 (LLM이 planId를 알 필요 없음)
//  */
// @Component("planTools")
// @RequiredArgsConstructor
// @Slf4j
// public class PlanTools {

//     private final PlanAddAction addAction;
//     private final PlanModifyAction modifyAction;
//     private final PlanSwapAction swapAction;
//     private final PlanDeleteAction deleteAction;

//     private final PlanDao planDao;
//     private final PlanDayDao planDayDao;
//     private final PlanPlaceDao planPlaceDao;
//     private final PlanSnapshotService planSnapshotService;
//     private final PlanSnapshotDao planSnapshotDao;
//     private final PlanSnapshotUtility planSnapshotUtility;

//     private final TravelPlanAgent travelPlanAgent;
//     private final TravelPlanSaveService travelPlanSaveService;
//     private final ThreadLocal<Long> currentPlanId = new ThreadLocal<>();

//     DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//     DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

//     public void setPlanId(Long planId) {
//         currentPlanId.set(planId);
//     }

//     public void clearPlanId() {
//         currentPlanId.remove();
//     }

//     private Long getPlanId() {
//         return currentPlanId.get();
//     }

//     @Tool(description = "현재 여행 일정 전체를 조회합니다")
//     public String viewPlan() {
//         Long planId = getPlanId();
//         log.info("🔧 [Tool] viewPlan: planId={}", planId);

//         try {
//             // 1) plan 조회
//             Plan plan = planDao.selectPlanById(planId);

//             if (plan == null) {
//                 return "일정을 찾을 수 없습니다.";
//             }

//             // 2) days 조회
//             List<PlanDay> planDays = planDayDao.selectPlanDaysByPlanId(planId);

//             // 3) places 조회
//             Map<Long, List<PlanPlace>> placesByDayId = new HashMap<>();
//             for (PlanDay day : planDays) {
//                 List<PlanPlace> places = planPlaceDao.selectPlanPlacesByPlanDayId(day.getId());
//                 placesByDayId.put(day.getId(), places);
//             }

//             // 4) 렌더링
//             return renderPlan(plan, planDays, placesByDayId);

//         } catch (Exception e) {
//             log.error("일정 조회 실패", e);
//             return "일정 조회 중 오류 발생: " + e.getMessage();
//         }
//     }

//     @Tool(description = "특정 일차의 일정만 조회합니다. dayIndex는 1부터 시작 (1일차=1), 해당 툴의 응답은 최대한 가공하지 않고 제공해주세요.")
//     public String viewDay(
//             @ToolParam(description = "조회할 일차 번호 (1부터 시작)") int dayIndex) {
//         Long planId = getPlanId();
//         log.info("🔧 [Tool] viewDay: planId={}, dayIndex={}", planId, dayIndex);

//         try {
//             // Day 조회
//             PlanDay day = planDayDao.selectPlanDayByPlanIdAndDayIndex(planId, dayIndex);

//             if (day == null) {
//                 return dayIndex + "일차가 없습니다.";
//             }

//             // Places 조회
//             List<PlanPlace> places = planPlaceDao.selectPlanPlacesByPlanDayId(day.getId());
//             return renderDay(day, places);

//         } catch (Exception e) {
//             log.error("일차 조회 실패", e);
//             return "일차 조회 중 오류 발생: " + e.getMessage();
//         }
//     }

//     @Tool(description = "특정 장소를 일정에서 삭제합니다")
//     public String deletePlace(String placeName) {
//         Long planId = getPlanId();
//         log.info("🔧 [Tool] deletePlace: planId={}, placeName={}", planId, placeName);
//         try {
//             deleteAction.deletePlaceByName(planId, placeName);

//             // 스냅샷 저장
//             Plan plan = planDao.selectPlanById(planId);
//             List<PlanDay> planDays = planDayDao.selectPlanDaysByPlanId(planId);
//             List<PlanPlace> planPlaces = planPlaceDao.selectPlanPlacesByPlanId(planId);
//             PlanSnapshot newSnapshot = planSnapshotService.savePlanSnapshot(plan, planDays, planPlaces);
//             Integer versionNo = newSnapshot.getVersionNo();

//             return String.format("✅ '%s' 장소를 일정에서 삭제했습니다. 버전: %d", placeName, versionNo);
//         } catch (IllegalArgumentException e) {
//             return String.format("❌ '%s' 장소를 찾을 수 없습니다.", placeName);
//         } catch (Exception e) {
//             log.error("장소 삭제 실패", e);
//             return String.format("❌ 장소 삭제 중 오류 발생: %s", e.getMessage());
//         }
//     }

//     @Tool(description = "같은 날짜 내에서 두 장소의 순서를 교환합니다 (dayIndex는 1부터 시작)")
//     public String swapPlaces(int dayIndex, int index1, int index2) {
//         Long planId = getPlanId();
//         log.info("🔧 [Tool] swapPlaces: planId={}, dayIndex={}, index1={}, index2={}", planId, dayIndex, index1,
//                 index2);
//         try {
//             swapAction.swapPlacesInSameDay(planId, dayIndex, index1, index2);

//             // 스냅샷 저장
//             Plan plan = planDao.selectPlanById(planId);
//             List<PlanDay> planDays = planDayDao.selectPlanDaysByPlanId(planId);
//             List<PlanPlace> planPlaces = planPlaceDao.selectPlanPlacesByPlanId(planId);
//             PlanSnapshot newSnapshot = planSnapshotService.savePlanSnapshot(plan, planDays, planPlaces);
//             Integer versionNo = newSnapshot.getVersionNo();

//             return String.format("✅ %d일차의 %d번째와 %d번째 장소 순서를 교환했습니다. 버전: %d", dayIndex, index1, index2, versionNo);
//         } catch (Exception e) {
//             log.error("장소 순서 교환 실패", e);
//             return String.format("❌ 장소 순서 교환 중 오류 발생: %s", e.getMessage());
//         }
//     }

//     @Tool(description = "서로 다른 날짜 간 장소를 교환합니다 (dayIndex는 1부터 시작)")
//     public String swapPlacesBetweenDays(int day1, int index1, int day2, int index2) {
//         Long planId = getPlanId();
//         log.info("🔧 [Tool] swapPlacesBetweenDays: planId={}, day1={}, index1={}, day2={}, index2={}", planId, day1,
//                 index1, day2, index2);
//         try {
//             swapAction.swapPlacesBetweenDays(planId, day1, index1, day2, index2);

//             // 스냅샷 저장
//             Plan plan = planDao.selectPlanById(planId);
//             List<PlanDay> planDays = planDayDao.selectPlanDaysByPlanId(planId);
//             List<PlanPlace> planPlaces = planPlaceDao.selectPlanPlacesByPlanId(planId);
//             PlanSnapshot newSnapshot = planSnapshotService.savePlanSnapshot(plan, planDays, planPlaces);
//             Integer versionNo = newSnapshot.getVersionNo();

//             return String.format("✅ %d일차의 %d번째 장소와 %d일차의 %d번째 장소를 교환했습니다. 버전: %d", day1, index1, day2, index2,
//                     versionNo);
//         } catch (Exception e) {
//             log.error("날짜 간 장소 교환 실패", e);
//             return String.format("❌ 장소 교환 중 오류 발생: %s", e.getMessage());
//         }
//     }

//     @Tool(description = "기존 장소를 다른 장소로 교체합니다 (첫 번째 검색 결과 자동 선택)")
//     public String replacePlace(String oldPlaceName, String newPlaceName) {
//         Long planId = getPlanId();
//         log.info("🔧 [Tool] replacePlace: planId={}, old={}, new={}", planId, oldPlaceName, newPlaceName);
//         try {
//             String newName = modifyAction.replacePlaceWithSearch(planId, oldPlaceName, newPlaceName);

//             // 스냅샷 저장
//             Plan plan = planDao.selectPlanById(planId);
//             List<PlanDay> planDays = planDayDao.selectPlanDaysByPlanId(planId);
//             List<PlanPlace> planPlaces = planPlaceDao.selectPlanPlacesByPlanId(planId);
//             PlanSnapshot newSnapshot = planSnapshotService.savePlanSnapshot(plan, planDays, planPlaces);
//             Integer versionNo = newSnapshot.getVersionNo();

//             return String.format("✅ '%s'를 '%s'(으)로 변경했습니다. 버전: %d", oldPlaceName, newName, versionNo);
//         } catch (Exception e) {
//             log.error("장소 교체 실패", e);
//             return String.format("❌ 장소 교체 중 오류 발생: %s", e.getMessage());
//         }
//     }

//     @Tool(description = "네이버에서 장소를 검색하여 여러 후보를 보여줍니다")
//     public String searchPlace(String searchQuery) {
//         log.info("🔧 [Tool] searchPlace: query={}", searchQuery);
//         try {
//             var searchResults = addAction.searchNaverLocal(searchQuery);
//             if (searchResults.isEmpty()) {
//                 return String.format("❌ '%s' 검색 결과가 없습니다.", searchQuery);
//             }

//             int count = Math.min(searchResults.size(), 5);
//             StringBuilder result = new StringBuilder();
//             result.append(String.format("🔍 '%s' 검색 결과 %d개:\n\n", searchQuery, count));

//             for (int i = 0; i < count; i++) {
//                 LocalItem item = searchResults.get(i);
//                 result.append(String.format("%d. **%s**\n", i + 1, cleanHtmlTags(item.getTitle())));
//                 result.append(String.format("   - 카테고리: %s\n", item.getCategory()));
//                 result.append(String.format("   - 주소: %s\n", item.getRoadAddress()));
//                 if (i < count - 1)
//                     result.append("\n");
//             }

//             result.append("\n어떤 장소로 하시겠어요? (번호로 선택해주세요)");
//             return result.toString();
//         } catch (Exception e) {
//             log.error("장소 검색 실패", e);
//             return String.format("❌ 장소 검색 중 오류: %s", e.getMessage());
//         }
//     }

//     @Tool(description = "검색 결과에서 사용자가 선택한 장소로 교체합니다")
//     public String replacePlaceWithSelection(String oldPlaceName, String newPlaceName, int selectedIndex) {
//         Long planId = getPlanId();
//         log.info("🔧 [Tool] replacePlaceWithSelection: planId={}, old={}, new={}, index={}", planId, oldPlaceName,
//                 newPlaceName, selectedIndex);
//         try {
//             String newName = modifyAction.replacePlaceWithSelection(planId, oldPlaceName, newPlaceName, selectedIndex);

//             // 스냅샷 저장
//             Plan plan = planDao.selectPlanById(planId);
//             List<PlanDay> planDays = planDayDao.selectPlanDaysByPlanId(planId);
//             List<PlanPlace> planPlaces = planPlaceDao.selectPlanPlacesByPlanId(planId);
//             PlanSnapshot newSnapshot = planSnapshotService.savePlanSnapshot(plan, planDays, planPlaces);
//             Integer versionNo = newSnapshot.getVersionNo();

//             return String.format("✅ '%s'를 '%s'(으)로 변경했습니다. 버전: %d", oldPlaceName, newName, versionNo);
//         } catch (Exception e) {
//             log.error("장소 교체 실패", e);
//             return String.format("❌ 장소 교체 중 오류 발생: %s", e.getMessage());
//         }
//     }

//     @Tool(description = "특정 날짜에 새로운 장소를 추가합니다. dayIndex는 1부터 시작 (1일차=1, 2일차=2). 장소명으로 자동 검색하여 추가합니다.")
//     public String addPlace(int dayIndex, String placeName, String startTime) {
//         Long planId = getPlanId();
//         log.info("🔧 [Tool] addPlace: planId={}, dayIndex={}, placeName={}, startTime={}", planId, dayIndex, placeName,
//                 startTime);
//         try {
//             String result = addAction.addPlace(planId, dayIndex, placeName, startTime);

//             // 스냅샷 저장
//             Plan plan = planDao.selectPlanById(planId);
//             List<PlanDay> planDays = planDayDao.selectPlanDaysByPlanId(planId);
//             List<PlanPlace> planPlaces = planPlaceDao.selectPlanPlacesByPlanId(planId);
//             PlanSnapshot newSnapshot = planSnapshotService.savePlanSnapshot(plan, planDays, planPlaces);
//             Integer versionNo = newSnapshot.getVersionNo();

//             return String.format("✅ %d일차에 '%s'을(를) 추가했습니다. 버전: %d", dayIndex, result, versionNo);
//         } catch (Exception e) {
//             log.error("장소 추가 실패", e);
//             return String.format("❌ 장소 추가 중 오류 발생: %s", e.getMessage());
//         }
//     }

//     @Tool(description = "특정 위치에 장소를 삽입하고 이후 일정을 자동으로 조정합니다. dayIndex는 1부터 시작.")
//     public String addPlaceAtPosition(int dayIndex, int position, String placeName, Integer duration) {
//         Long planId = getPlanId();
//         log.info("🔧 [Tool] addPlaceAtPosition: planId={}, dayIndex={}, position={}, placeName={}, duration={}",
//                 planId, dayIndex, position, placeName, duration);
//         try {
//             String result = addAction.addPlaceAtPosition(planId, dayIndex, position, placeName, duration);

//             // 스냅샷 저장
//             Plan plan = planDao.selectPlanById(planId);
//             List<PlanDay> planDays = planDayDao.selectPlanDaysByPlanId(planId);
//             List<PlanPlace> planPlaces = planPlaceDao.selectPlanPlacesByPlanId(planId);
//             PlanSnapshot newSnapshot = planSnapshotService.savePlanSnapshot(plan, planDays, planPlaces);
//             Integer versionNo = newSnapshot.getVersionNo();

//             return String.format("✅ %d일차 %d번째에 '%s'을(를) 추가했습니다. 버전: %d", dayIndex, position, result, versionNo);
//         } catch (Exception e) {
//             log.error("장소 삽입 실패", e);
//             return String.format("❌ 장소 삽입 중 오류 발생: %s", e.getMessage());
//         }
//     }

//     @Tool(description = "특정 장소의 시간을 변경합니다")
//     public String updatePlaceTime(String placeName, String newTime) {
//         Long planId = getPlanId();
//         log.info("🔧 [Tool] updatePlaceTime: planId={}, placeName={}, newTime={}", planId, placeName, newTime);
//         try {
//             modifyAction.updatePlaceTime(planId, placeName, newTime);

//             // 스냅샷 저장
//             Plan plan = planDao.selectPlanById(planId);
//             List<PlanDay> planDays = planDayDao.selectPlanDaysByPlanId(planId);
//             List<PlanPlace> planPlaces = planPlaceDao.selectPlanPlacesByPlanId(planId);
//             PlanSnapshot newSnapshot = planSnapshotService.savePlanSnapshot(plan, planDays, planPlaces);
//             Integer versionNo = newSnapshot.getVersionNo();

//             return String.format("✅ '%s'의 시간을 %s(으)로 변경했습니다. %d 버전: ", placeName, newTime, versionNo);
//         } catch (Exception e) {
//             log.error("시간 변경 실패", e);
//             return String.format("❌ 시간 변경 중 오류 발생: %s", e.getMessage());
//         }
//     }

//     @Tool(description = "특정 날짜 전체를 삭제합니다 (dayIndex는 1부터 시작)")
//     public String deleteDay(int dayIndex) {
//         Long planId = getPlanId();
//         log.info("🔧 [Tool] deleteDay: planId={}, dayIndex={}", planId, dayIndex);
//         try {
//             deleteAction.deleteDay(planId, dayIndex);

//             // 스냅샷 저장
//             Plan plan = planDao.selectPlanById(planId);
//             List<PlanDay> planDays = planDayDao.selectPlanDaysByPlanId(planId);
//             List<PlanPlace> planPlaces = planPlaceDao.selectPlanPlacesByPlanId(planId);
//             PlanSnapshot newSnapshot = planSnapshotService.savePlanSnapshot(plan, planDays, planPlaces);
//             Integer versionNo = newSnapshot.getVersionNo();

//             return String.format("✅ %d일차 일정을 삭제했습니다. 버전: %d", dayIndex, versionNo);
//         } catch (Exception e) {
//             log.error("날짜 삭제 실패", e);
//             return String.format("❌ 날짜 삭제 중 오류 발생: %s", e.getMessage());
//         }
//     }

//     @Tool(description = "두 날짜의 일정 전체를 교환합니다 (dayIndex는 1부터 시작)")
//     public String swapDays(int day1, int day2) {
//         Long planId = getPlanId();
//         log.info("🔧 [Tool] swapDays: planId={}, day1={}, day2={}", planId, day1, day2);
//         try {
//             swapAction.swapDays(planId, day1, day2);

//             // 스냅샷 저장
//             Plan plan = planDao.selectPlanById(planId);
//             List<PlanDay> planDays = planDayDao.selectPlanDaysByPlanId(planId);
//             List<PlanPlace> planPlaces = planPlaceDao.selectPlanPlacesByPlanId(planId);
//             PlanSnapshot newSnapshot = planSnapshotService.savePlanSnapshot(plan, planDays, planPlaces);
//             Integer versionNo = newSnapshot.getVersionNo();

//             return String.format("✅ %d일차와 %d일차 일정을 교환했습니다. 버전: %d", day1, day2, versionNo);
//         } catch (Exception e) {
//             log.error("날짜 교환 실패", e);
//             return String.format("❌ 날짜 교환 중 오류 발생: %s", e.getMessage());
//         }
//     }

//     @Tool(description = "여행 기간을 늘립니다 (날짜 추가)")
//     public String extendPlan(int extraDays) {
//         Long planId = getPlanId();
//         log.info("🔧 [Tool] extendPlan: planId={}, extraDays={}", planId, extraDays);
//         try {
//             addAction.extendPlan(planId, extraDays);

//             // 스냅샷 저장
//             Plan plan = planDao.selectPlanById(planId);
//             List<PlanDay> planDays = planDayDao.selectPlanDaysByPlanId(planId);
//             List<PlanPlace> planPlaces = planPlaceDao.selectPlanPlacesByPlanId(planId);
//             PlanSnapshot newSnapshot = planSnapshotService.savePlanSnapshot(plan, planDays, planPlaces);
//             Integer versionNo = newSnapshot.getVersionNo();

//             return String.format("✅ 여행을 %d일 연장했습니다. 버전: %d", extraDays, versionNo);
//         } catch (Exception e) {
//             log.error("일정 확장 실패", e);
//             return String.format("❌ 일정 확장 중 오류 발생: %s", e.getMessage());
//         }
//     }

//     @Tool(description = "전체 일정을 완전히 삭제합니다 (Plan + 모든 날짜와 장소 삭제). 중요: 사용자가 명확히 확인한 경우에만 호출하세요!")
//     public String deletePlan(ToolContext toolContext) {
//         Long planId = getPlanId();
//         log.info("🔧 [Tool] deletePlan: planId={}", planId);
//         try {
//             deleteAction.deleteAllDaysAndPlaces(planId);
//             // 사용자의 스냅샷 모두 삭제
//             planSnapshotDao.deletePlanSnapshotsByUserId((Long) toolContext.getContext().get("userId"));
//             return "✅ 전체 일정이 완전히 삭제되었습니다. 새로운 여행 계획을 만들고 싶으시면 말씀해주세요!";
//         } catch (Exception e) {
//             log.error("전체 일정 삭제 실패", e);
//             return String.format("❌ 전체 일정 삭제 중 오류 발생: %s", e.getMessage());
//         }
//     }

//     @Transactional
//     @Tool(description = "사용자가 가지고 있는 계획 스냅샷의 바로 이전 버전으로 돌아갑니다.")
//     public String rollBack(@ToolParam(description = "사용자 아이디") ToolContext toolContext) {
//         try {
//             PlanSnapshot planSnapshot = planSnapshotService
//                     .getPlanSnapshotsByUserId((Long) toolContext.getContext().get("userId")).get(1);
//             PlanSnapshotContent snapshotContent = planSnapshotUtility.parseSnapshot(planSnapshot.getSnapshotJson());

//             Long planId = getPlanId();
//             Plan plan = planDao.selectPlanById(getPlanId());

//             List<PlanPlace> existingPlaces = planPlaceDao.selectPlanPlacesByPlanId(planId);
//             for (PlanPlace place : existingPlaces) {
//                 planPlaceDao.deletePlanPlaceById(place.getId());
//             }
//             log.info("plan_places 삭제 완료");

//             List<PlanDay> existingDays = planDayDao.selectPlanDaysByPlanId(planId);
//             for (PlanDay day : existingDays) {
//                 planDayDao.deletePlanDay(day.getId());
//             }
//             log.info("plan_days 삭제 완료");

//             Plan rollbackPlan = Plan.builder()
//                     .userId((Long) toolContext.getContext().get("userId"))
//                     .budget(snapshotContent.getBudget())
//                     .startDate(LocalDate.parse(snapshotContent.getStartDate(), formatter1))
//                     .endDate(LocalDate.parse(snapshotContent.getEndDate(), formatter1))
//                     .createdAt(plan.getCreatedAt())
//                     .updatedAt(OffsetDateTime.now())
//                     .build();
//             planDao.updatePlan(rollbackPlan);
//             log.info("Plan 업데이트 완료");

//             Map<String, Long> dateToDayId = new HashMap<>();
//             for (int i = 0; i < snapshotContent.getDays().size(); i++) {
//                 PlanSnapshotContent.PlanDay pscDay = snapshotContent.getDays().get(i);

//                 PlanDay newDay = PlanDay.builder()
//                         .planId(planId)
//                         .dayIndex(i + 1)
//                         .title(pscDay.getTitle())
//                         .planDate(LocalDate.parse(pscDay.getDate(), formatter1))
//                         .build();

//                 planDayDao.insertPlanDay(newDay);
//                 dateToDayId.put(pscDay.getDate(), newDay.getId());
//             }
//             log.info("PlanDays 재생성 완료");

//             for (PlanSnapshotContent.PlanDay pscDay : snapshotContent.getDays()) {
//                 Long dayId = dateToDayId.get(pscDay.getDate());

//                 for (PlanSnapshotContent.PlanDayItem pscItem : pscDay.getSchedules()) {
//                     PlanPlace newPlace = PlanPlace.builder()
//                             .dayId(dayId)
//                             .title(pscItem.getTitle())
//                             .startAt(LocalDateTime.parse(pscItem.getStartAt(), formatter2)
//                                     .atOffset(ZoneOffset.of("+00:00")))
//                             .endAt(LocalDateTime.parse(pscItem.getEndAt(), formatter2)
//                                     .atOffset(ZoneOffset.of("+00:00")))
//                             .placeName(pscItem.getPlaceName())
//                             .address(pscItem.getAddress())
//                             .lat(pscItem.getLat())
//                             .lng(pscItem.getLng())
//                             .expectedCost(pscItem.getExpectedCost())
//                             .normalizedCategory(pscItem.getNormalizedCategory())
//                             .firstImage(pscItem.getFirstImage())
//                             .firstImage2(pscItem.getFirstImage2())
//                             .isEnded(pscItem.getIsEnded() == null ? false : pscItem.getIsEnded())
//                             .build();

//                     planPlaceDao.insertPlanPlace(newPlace);
//                 }
//             }
//             log.info("PlanPlaces 재생성 완료");

//             List<PlanDay> newDays = planDayDao.selectPlanDaysByPlanId(planId);
//             List<PlanPlace> newPlaces = planPlaceDao.selectPlanPlacesByPlanId(planId);
//             PlanSnapshot newSnapshot = planSnapshotService.savePlanSnapshot(rollbackPlan, newDays, newPlaces);
//             Integer newVersionNo = newSnapshot.getVersionNo();
//             log.info("버전 환원 완료");
//             return String.format("이전 버전으로 돌아갔습니다. 버전 번호는 증가합니다. 버전: %d", newVersionNo);

//         } catch (Exception e) {
//             log.error(e.getMessage());
//             return String.format("❌ 버전 환원 중 오류 발생: %s", e.getMessage());
//         }
//     }

//     @Transactional
//     @Tool(description = "사용자가 지정한 계획의 버전으로 으로 돌아갑니다. 버전 정보가 언급된 경우에만 사용합니다.")
//     public String rollBackToSpecific(@ToolParam(description = "돌아가고자 하는 버전 번호") Integer versionNo,
//             ToolContext toolContext) {
//         try {
//             log.info("돌아갈 버전: {}", versionNo);
//             PlanSnapshot toRevert = PlanSnapshot.builder()
//                     .userId((Long) toolContext.getContext().get("userId"))
//                     .versionNo(versionNo)
//                     .build();
//             // PlanSnapshot planSnapshot =
//             // planSnapshotService.getPlanSnapshotsByUserId((Long)
//             // toolContext.getContext().get("userId")).get(1);
//             log.info("toRevert: {}", toRevert.toString());
//             PlanSnapshot planSnapshot = planSnapshotDao.selectPlanSnapshotByUserIdAndVersionNo(toRevert);
//             PlanSnapshotContent snapshotContent = planSnapshotUtility.parseSnapshot(planSnapshot.getSnapshotJson());

//             Long planId = getPlanId();
//             Plan plan = planDao.selectPlanById(getPlanId());

//             List<PlanPlace> existingPlaces = planPlaceDao.selectPlanPlacesByPlanId(planId);
//             for (PlanPlace place : existingPlaces) {
//                 planPlaceDao.deletePlanPlaceById(place.getId());
//             }
//             log.info("plan_places 삭제 완료");

//             List<PlanDay> existingDays = planDayDao.selectPlanDaysByPlanId(planId);
//             for (PlanDay day : existingDays) {
//                 planDayDao.deletePlanDay(day.getId());
//             }
//             log.info("plan_days 삭제 완료");

//             Plan rollbackPlan = Plan.builder()
//                     .userId((Long) toolContext.getContext().get("userId"))
//                     .budget(snapshotContent.getBudget())
//                     .startDate(LocalDate.parse(snapshotContent.getStartDate(), formatter1))
//                     .endDate(LocalDate.parse(snapshotContent.getEndDate(), formatter1))
//                     .createdAt(plan.getCreatedAt())
//                     .updatedAt(OffsetDateTime.now())
//                     .build();
//             planDao.updatePlan(rollbackPlan);
//             log.info("Plan 업데이트 완료");

//             Map<String, Long> dateToDayId = new HashMap<>();
//             for (int i = 0; i < snapshotContent.getDays().size(); i++) {
//                 PlanSnapshotContent.PlanDay pscDay = snapshotContent.getDays().get(i);

//                 PlanDay newDay = PlanDay.builder()
//                         .planId(planId)
//                         .dayIndex(i + 1)
//                         .title(pscDay.getTitle())
//                         .planDate(LocalDate.parse(pscDay.getDate(), formatter1))
//                         .build();

//                 planDayDao.insertPlanDay(newDay);
//                 dateToDayId.put(pscDay.getDate(), newDay.getId());
//             }
//             log.info("PlanDays 재생성 완료");

//             for (PlanSnapshotContent.PlanDay pscDay : snapshotContent.getDays()) {
//                 Long dayId = dateToDayId.get(pscDay.getDate());

//                 for (PlanSnapshotContent.PlanDayItem pscItem : pscDay.getSchedules()) {
//                     PlanPlace newPlace = PlanPlace.builder()
//                             .dayId(dayId)
//                             .title(pscItem.getTitle())
//                             .startAt(LocalDateTime.parse(pscItem.getStartAt(), formatter2)
//                                     .atOffset(ZoneOffset.of("+00:00")))
//                             .endAt(LocalDateTime.parse(pscItem.getEndAt(), formatter2)
//                                     .atOffset(ZoneOffset.of("+00:00")))
//                             .placeName(pscItem.getPlaceName())
//                             .address(pscItem.getAddress())
//                             .lat(pscItem.getLat())
//                             .lng(pscItem.getLng())
//                             .expectedCost(pscItem.getExpectedCost())
//                             .normalizedCategory(pscItem.getNormalizedCategory())
//                             .firstImage(pscItem.getFirstImage())
//                             .firstImage2(pscItem.getFirstImage2())
//                             .isEnded(pscItem.getIsEnded() == null ? false : pscItem.getIsEnded())
//                             .build();

//                     planPlaceDao.insertPlanPlace(newPlace);
//                 }
//             }
//             log.info("PlanPlaces 재생성 완료");

//             List<PlanDay> newDays = planDayDao.selectPlanDaysByPlanId(planId);
//             List<PlanPlace> newPlaces = planPlaceDao.selectPlanPlacesByPlanId(planId);
//             // planSnapshotService.savePlanSnapshot(rollbackPlan, newDays, newPlaces);
//             PlanSnapshot newSnapshot = planSnapshotService.savePlanSnapshot(rollbackPlan, newDays, newPlaces);
//             Integer newVersionNo = newSnapshot.getVersionNo();
//             log.info("버전 환원 완료");
//             return String.format("이전 버전으로 돌아갔습니다. 버전 번호는 증가합니다. 버전: %d", newVersionNo);

//         } catch (Exception e) {
//             log.error(e.getMessage());
//             return String.format("❌ 버전 환원 중 오류 발생: %s", e.getMessage());
//         }
//     }

//     @Tool(name = "createSeoulTravelPlan", description = """
//             서울 여행 일정을 자동으로 생성합니다.
//             사용자가 "N박N일 계획 짜줘", "여행 일정 만들어줘"라고 요청할 때 사용하세요.
//             사용자의 요청에서 다음 정보를 추출하여 전달하세요:
//             - userMessage: 유저 메시지 (필수)
//             - duration: 여행 기간 (필수)
//             - style: 여행 스타일 (선택)
//             - location: 선호 지역 (선택)
//             - pace: 일정 강도 (선택)
//             - startDateText: 여행 시작 시점 (선택)

//             예시:
//             "3일 kpop 강남 여행"
//             → duration=3, style="kpop", location="강남", pace=null

//             "서울 당일치기"
//             → duration=1, style=null, location=null, pace=null

//             "5일 힐링 여행 빡빡하게"
//             → duration=5, style="힐링", location=null, pace="빡빡"

//             주의:
//             - 새로운 여행 계획을 생성할 때만 사용하세요.
//             - 여행 기간이 없다면 사용자에게 다시 물어보세요.
//             """)
//     public String createTravelPlan(

//             @ToolParam(description = "여행 기간(일). 반드시 사용자 발화에 명시되어야 함, 추론할 수 없다면 null", required = true) Integer duration,

//             @ToolParam(description = "여행 스타일. 예: 'kpop', '힐링'. 없으면 null", required = false) String style,

//             @ToolParam(description = "선호 지역. 예: '강남', '강남, 홍대'. 없으면 null", required = false) String location,

//             @ToolParam(description = "일정 강도. '빡빡', '널널'. 없으면 null", required = false) String pace,

//             @ToolParam(description = "사용자가 말한 여행 시작 시점의 원문 표현 그대로. 예: '3일뒤', '다음주 월요일', '이번주말'. 날짜 계산하지 말 것, yyyy-MM-dd로 변환하지 말것", required = false) String startDateText,
//             ToolContext toolContext) {

//         // String sessionId = currentSessionId.get();
//         // 검증 (null이면 에러)
//         // if (sessionId == null) {
//         // log.error("❌ SessionId가 ThreadLocal에 없습니다. 비정상적인 호출입니다.");
//         // return "세션 오류가 발생했습니다. 다시 시도해주세요.";
//         // }

//         log.info("🗼 Tool 호출: createTravelPlan");
//         log.info("   파라미터: duration={}, style={}, location={}, pace={}",
//                 duration, style, location, pace);
//         // log.info(" 세션 ID: {}", sessionId);

//         boolean success = false;

//         Long userId = (Long) toolContext.getContext().get("userId");

//         try {
//             // sendAgentEvent("Seoul Planner", "running", "서울 여행 일정 생성 중...");

//             // validation
//             if (duration == null || duration <= 0) {
//                 return "여행 기간을 지정해주세요.";
//             }

//             GeneratedTravelPlan plan = travelPlanAgent.createSeoulTravelPlanStructured(
//                     duration, style, location, pace, startDateText);
//             if (plan.days().isEmpty()) {
//                 return "여행 기간을 지정해주세요. 며칠 동안 여행하실 예정인가요?";
//             }

//             log.info("   일정 생성 완료 - 길이: {} 자", plan.toString().length());

//             // String planSummary = PlanState.buildSummary(plan, location, style);

//             Long planId = travelPlanSaveService.save(userId, plan, new ObjectMapper());

//             // savePlanToState(sessionId, planId, planSummary);

//             success = true;
//             // sendAgentEvent("Seoul Planner", "complete", "서울 여행 일정 생성 완료");
//             return render(plan);

//         } catch (Exception e) {
//             log.error("❌ 서울 여행 일정 생성 실패", e);
//             return null;

//         } finally {
//             if (!success) {
//                 // sendAgentEvent("Seoul Planner", "error", "서울 여행 일정 생성 실패");
//             }
//         }
//     }

//     public static String render(GeneratedTravelPlan plan) {
//         StringBuilder sb = new StringBuilder();

//         sb.append("=== Seoul Travel Itinerary ===\n");
//         sb.append("📅 ")
//                 .append(plan.startDate())
//                 .append(" ~ ")
//                 .append(plan.endDate())
//                 .append("\n");
//         sb.append("⏱️ Pace: ").append(plan.pace()).append("\n\n");

//         for (var day : plan.days()) {
//             sb.append("Day ").append(day.dayIndex()).append("\n");

//             for (var p : day.places()) {
//                 sb.append("  ")
//                         .append(p.startAt())
//                         .append("-")
//                         .append(p.endAt())
//                         .append(" ")
//                         .append(p.title())
//                         .append("\n");
//             }
//             sb.append("\n");
//         }
//         return sb.toString();
//     }

//     // Helper method
//     private String cleanHtmlTags(String text) {
//         if (text == null)
//             return null;
//         return text.replaceAll("<[^>]*>", "");
//     }

//     private String renderPlan(
//             Plan plan,
//             List<PlanDay> planDays,
//             Map<Long, List<PlanPlace>> placesByDayId) {

//         StringBuilder sb = new StringBuilder();

//         sb.append("📅 ").append(plan.getTitle() != null ? plan.getTitle() : "여행 일정").append("\n");
//         sb.append("기간: ").append(plan.getStartDate())
//                 .append(" ~ ").append(plan.getEndDate()).append("\n");

//         if (plan.getBudget() != null) {
//             sb.append("예산: ").append(plan.getBudget()).append("원\n");
//         }
//         sb.append("\n");

//         for (PlanDay day : planDays) {
//             sb.append("=== Day ").append(day.getDayIndex()).append(" ===\n");

//             if (day.getTitle() != null) {
//                 sb.append(day.getTitle()).append("\n");
//             }
//             sb.append(day.getPlanDate()).append("\n\n");

//             List<PlanPlace> places = placesByDayId.get(day.getId());

//             if (places == null || places.isEmpty()) {
//                 sb.append("등록된 장소가 없습니다.\n\n");
//                 continue;
//             }

//             for (PlanPlace place : places) {
//                 sb.append("• ");

//                 // if (place.getStartAt() != null) {
//                 // sb.append(place.getStartAt().toLocalTime()).append(" ");
//                 // }

//                 sb.append(place.getTitle()).append("\n");

//                 if (place.getAddress() != null) {
//                     sb.append("  📍 ").append(place.getAddress()).append("\n");
//                 }

//                 sb.append("\n");
//             }
//         }

//         return sb.toString();
//     }

//     private String renderDay(PlanDay day, List<PlanPlace> places) {
//         StringBuilder sb = new StringBuilder();

//         sb.append("📅 Day ").append(day.getDayIndex()).append("\n");
//         sb.append("🗓 날짜: ").append(day.getPlanDate()).append("\n");
//         sb.append("────────────────────\n");

//         if (places == null || places.isEmpty()) {
//             sb.append("⚠️ 일정에 등록된 장소가 없습니다.\n");
//             return sb.toString();
//         }

//         for (PlanPlace place : places) {
//             // 시간
//             sb.append("• ");
//             if (place.getStartAt() != null) {
//                 sb.append("⏰ ")
//                         .append(place.getStartAt().toLocalTime())
//                         .append(" ");
//             }

//             // 장소명
//             sb.append(place.getTitle());

//             // 카테고리
//             if (place.getNormalizedCategory() != null) {
//                 sb.append("  [")
//                         .append(CategoryNames.categoryLabel(place.getNormalizedCategory()))
//                         .append("]");
//             }
//             sb.append("\n");

//             // 주소
//             if (place.getAddress() != null) {
//                 sb.append("   📍 ")
//                         .append(place.getAddress())
//                         .append("\n");
//             }

//             sb.append("\n");
//         }
//         log.info(sb.toString()+">>>>>>>>>>>>>>>>>>>>>>>>>>>");
//         return sb.toString();
//     }
// }
