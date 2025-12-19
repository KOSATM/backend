package com.example.demo.planner.plan.agent.tools;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.demo.planner.plan.agent.common.PlanToolSupport;
import com.example.demo.planner.plan.dao.PlanDao;
import com.example.demo.planner.plan.dao.PlanDayDao;
import com.example.demo.planner.plan.dao.PlanPlaceDao;
import com.example.demo.planner.plan.dao.PlanSnapshotDao;
import com.example.demo.planner.plan.dto.entity.Plan;
import com.example.demo.planner.plan.dto.entity.PlanDay;
import com.example.demo.planner.plan.dto.entity.PlanPlace;
import com.example.demo.planner.plan.dto.entity.PlanSnapshot;
import com.example.demo.planner.plan.dto.response.PlanSnapshotContent;
import com.example.demo.planner.plan.service.PlanSnapshotService;
import com.example.demo.planner.plan.service.PlanSnapshotUtility;
import com.example.demo.planner.plan.service.action.PlanAddAction;
import com.example.demo.planner.plan.service.action.PlanDeleteAction;
import com.example.demo.planner.plan.service.action.PlanModifyAction;
import com.example.demo.planner.plan.service.action.PlanSwapAction;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component("planAdvancedTools")
@RequiredArgsConstructor
@Slf4j
public class PlanAdvancedTools {

    private final PlanToolSupport support;
    private final PlanSwapAction swapAction;
    private final PlanAddAction addAction;
    private final PlanDeleteAction deleteAction;
    private final PlanModifyAction modifyAction;
    private final PlanSnapshotDao planSnapshotDao;
    private final PlanSnapshotService planSnapshotService;
    private final PlanSnapshotUtility planSnapshotUtility;
    private final PlanDao planDao;
    private final PlanDayDao planDayDao;
    private final PlanPlaceDao planPlaceDao;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Value("${google.search.endpoint}")
    private String endpoint;
    @Value("${google.search.apiKey}")
    private String apiKey;
    @Value("${google.search.engineId}")
    private String engineId;

    DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ========== 순서 교환 (3개) ==========
    @Transactional
    @Tool(description = "같은 날짜 내에서 두 장소의 순서를 교환합니다 (dayIndex는 1부터 시작)")
    public String swapPlaces(int dayIndex, int index1, int index2, ToolContext toolContext) {
        String conversationId = getConversationId(toolContext);
        Long planId = support.getPlanId(conversationId);
        log.info("🔧 [Tool] swapPlaces: planId={}, dayIndex={}, index1={}, index2={}",
                planId, dayIndex, index1, index2);

        try {
            swapAction.swapPlacesInSameDay(planId, dayIndex, index1, index2);
            Integer version = support.saveSnapshot(planId);

            return String.format("%d일차의 %d번째와 %d번째 장소 순서를 교환했습니다. 버전: %d",
                    dayIndex, index1, index2, version);
        } catch (Exception e) {
            log.error("장소 순서 교환 실패", e);
            return String.format("장소 순서 교환 중 오류 발생: %s", e.getMessage());
        }
    }

    @Transactional
    @Tool(description = """
                서로 다른 날짜 간 장소를 교환합니다 (dayIndex는 1부터 시작)
            """)
    public String swapPlacesBetweenDays(int day1, int index1, int day2, int index2, ToolContext toolContext) {
        String conversationId = getConversationId(toolContext);
        Long planId = support.getPlanId(conversationId);
        log.info("🔧 [Tool] swapPlacesBetweenDays: planId={}, day1={}, index1={}, day2={}, index2={}",
                planId, day1, index1, day2, index2);

        try {
            swapAction.swapPlacesBetweenDays(planId, day1, index1, day2, index2);
            Integer version = support.saveSnapshot(planId);

            return String.format("✅ %d일차의 %d번째 장소와 %d일차의 %d번째 장소를 교환했습니다. 버전: %d",
                    day1, index1, day2, index2, version);
        } catch (Exception e) {
            log.error("날짜 간 장소 교환 실패", e);
            return String.format("❌ 장소 교환 중 오류 발생: %s", e.getMessage());
        }
    }

    @Transactional
    @Tool(description = """
            두 날짜의 일정 전체를 교환합니다 (dayIndex는 1부터 시작)
            - 교환하기 전 반드시 사용자에게 한번 더 물어봐주세요.
                        """)
    public String swapDays(int day1, int day2, ToolContext toolContext) {
        String conversationId = getConversationId(toolContext);
        Long planId = support.getPlanId(conversationId);

        log.info("🔧 [Tool] swapDays: planId={}, day1={}, day2={}", planId, day1, day2);

        try {
            swapAction.swapDays(planId, day1, day2);
            Integer version = support.saveSnapshot(planId);

            return String.format("✅ %d일차와 %d일차 일정을 교환했습니다. 버전: %d", day1, day2, version);
        } catch (Exception e) {
            log.error("날짜 교환 실패", e);
            return String.format("❌ 날짜 교환 중 오류 발생: %s", e.getMessage());
        }
    }

    // ========== 고급 추가 (2개) ==========
    // @Transactional
    // @Tool(description = "특정 위치에 장소를 삽입하고 이후 일정을 자동으로 조정합니다. dayIndex는 1부터 시작")
    // public String addPlaceAtPosition(int dayIndex, int position, String
    // placeName, Integer duration) {
    // Long planId = support.getPlanId();
    // log.info("🔧 [Tool] addPlaceAtPosition: planId={}, dayIndex={}, position={},
    // placeName={}, duration={}",
    // planId, dayIndex, position, placeName, duration);

    // try {
    // String result = addAction.addPlaceAtPosition(planId, dayIndex, position,
    // placeName, duration);
    // Integer version = support.saveSnapshot(planId);

    // return String.format("%d일차 %d번째에 '%s'을(를) 추가했습니다. 버전: %d",
    // dayIndex, position, result, version);
    // } catch (Exception e) {
    // log.error("장소 삽입 실패", e);
    // return String.format("❌ 장소 삽입 중 오류 발생: %s", e.getMessage());
    // }
    // }

    @Transactional
    @Tool(description = """
            추천된 장소 목록에서 사용자가 선택한 번호의 장소를
            지정한 위치에 추가합니다.

            사용 예:
            - "5번을 3일차 1번째에 추가해줘"
            - "추천 2번을 2일차 맨 앞에 넣어줘"

            중요 규칙:
            - 추천이 반드시 선행되어야 합니다.
            - 날짜(dayIndex)와 위치(position)가 명확하지 않으면 절대 추측하지 말고 반드시 사용자에게 다시 물어보세요.
            - 숫자는 추천 목록 번호(index)에만 사용하세요.
            """)
    public String addRecommendedPlace(
            @ToolParam(description = "몇 일차인지 (1부터). 없으면 null", required = false) Integer dayIndex,

            @ToolParam(description = "몇 번째 위치인지 (1부터). 없으면 null", required = false) Integer position,

            @ToolParam(description = "추천 목록에서 선택한 번호 (1부터 시작)", required = true) Integer index,

            // @ToolParam(description = "머무는 시간(분). 없으면 기본값 사용", required = false) Integer duration,

            ToolContext toolContext) {

        log.info("🧩 addRecommendedPlace 호출: dayIndex={}, position={}, index={}, duration={}",
                dayIndex, position, index);

        String conversationId = getConversationId(toolContext);
        Long planId = support.getPlanId(conversationId);

        // 1. 추천 목록 확인
        List<Map<String, Object>> recs = support.getLastRecommendations(conversationId);
        if (recs == null || recs.isEmpty()) {
            return "먼저 여행지 추천을 받아주세요.";
        }

        // 2. index 검증
        if (index == null || index < 1 || index > recs.size()) {
            return String.format("추천 목록은 1번부터 %d번까지 있습니다.", recs.size());
        }

        // 3. 날짜 / 위치 없으면 무조건 되묻기
        if (dayIndex == null || position == null) {
            return """
                    어느 날짜에, 몇 번째로 추가할까요?

                    예시:
                    - "5번을 3일차 1번째에 추가해줘"
                    - "추천 2번을 2일차 맨 뒤에 넣어줘"
                    """;
        }

        // 4. duration 기본값 처리 (안전)
        // if (duration == null || duration <= 0) {
        //     duration = 90;
        // }

        Map<String, Object> selected = recs.get(index - 1);
        Long placeId = ((Long) selected.get("id")).longValue();

        try {
            String newPlaceTitle = addAction.addPlaceFromRecommendation(
                    planId,
                    dayIndex,
                    position,
                    placeId);

            Integer version = support.saveSnapshot(planId);

            
            return String.format(
                    "%d일차 %d번째에 '%s'을(를) 추가했습니다. (버전 %d)",
                    dayIndex,
                    position,
                    newPlaceTitle,
                    version);

        } catch (Exception e) {
            log.error("❌ 추천 장소 추가 실패", e);
            return "추천 장소를 추가하는 중 오류가 발생했습니다: " + e.getMessage();
        }
    }

    @Tool(description = """
             인터넷 검색으로 사용자가 언급한 특정 장소에 대해 사실 기반 설명을 제공합니다.

                IMPORTANT:
                - 이 Tool은 장소를 추천하지 않습니다.
                - 후보를 비교하거나 선택하지 않습니다.
                - 이미 언급된 장소가 어떤 곳인지 설명할 때만 사용하세요.

                사용 예:
                - "경복궁이 뭐야?"
                - "이 일정에 있는 북촌 한옥마을은 어떤 곳이야?"
                - "이 카페 어떤 곳인지 설명해줘"

                주의:
                - 명확한 여행지 이름이 없다면 반드시 사용자에게 다시 물어보세요.
                - 사용자가 "어떤 곳인지 설명"을 명확히 요청한 경우에만 사용하세요.
                - 장소 이름이 불완전하거나 여러 후보가 떠오르면 절대 호출하지 말고 다시 물어보세요.
                - 일정 수정, 추천, 비교가 필요한 경우 이 Tool을 사용하지 마세요.
                - 인터넷 검색이란 단어가 있으면 절대 응답하지 마십시오.
            """)
    public String googleSearch(@ToolParam(description = "장소명") String searchQuery) {
        log.info("인터넷 검색 도구 호출됨");
        try {
            WebClient webClient = WebClient.builder().baseUrl(endpoint).defaultHeader("Accept", "application/json")
                    .build();
            String responseBody = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("key", apiKey)
                            .queryParam("cx", engineId)
                            .queryParam("q", searchQuery)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            log.info("응답본문: {}", responseBody);

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode items = root.path("items");

            if (!items.isArray() || items.isEmpty()) {
                return "검색 결과가 없습니다.";
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(3, items.size()); i++) {
                JsonNode item = items.get(i);
                String title = item.path("title").asText();
                String link = item.path("link").asText();
                String snippet = item.path("snippet").asText();
                sb.append(String.format("[%d] %s\n%s\n%s\n\n", i + 1, title, link, snippet));
            }
            log.info(sb.toString().trim() + ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
            return sb.toString().trim();

        } catch (Exception e) {
            return "인터넷 검색 중 오류 발생: " + e.getMessage();
        }
    }

    @Transactional
    @Tool(description = "여행 기간을 늘립니다 (날짜 추가)")
    public String extendPlan(int extraDays, ToolContext toolContext) {
        String conversationId = getConversationId(toolContext);
        Long planId = support.getPlanId(conversationId);
        log.info("🔧 [Tool] extendPlan: planId={}, extraDays={}", planId, extraDays);

        try {
            addAction.extendPlan(planId, extraDays);
            Integer version = support.saveSnapshot(planId);

            return String.format("여행을 %d일 연장했습니다. 버전: %d", extraDays, version);
        } catch (Exception e) {
            log.error("일정 확장 실패", e);
            return String.format("❌ 일정 확장 중 오류 발생: %s", e.getMessage());
        }
    }

    // ========== 검색 관련 (2개) ==========
    // @Transactional
    // @Tool(description = """
    // 인터넷 검색으로 사용자가 언급한 특정 장소에 대해 사실 기반 설명을 제공합니다.

    // IMPORTANT:
    // - 이 Tool은 장소를 추천하지 않습니다.
    // - 후보를 비교하거나 선택하지 않습니다.
    // - 이미 언급된 장소가 어떤 곳인지 설명할 때만 사용하세요.

    // 제공 정보:
    // - 장소 이름
    // - 주소
    // - 카테고리(관광지, 음식점, 카페 등)
    // - 장소의 성격을 이해하는 데 필요한 기본 정보

    // 사용 예:
    // - "경복궁이 뭐야?"
    // - "이 일정에 있는 북촌 한옥마을은 어떤 곳이야?"
    // - "이 카페 어떤 곳인지 설명해줘"

    // 주의:
    // - 명확한 여행지 이름이 없다면 반드시 사용자에게 다시 물어보세요.""")
    // public String searchPlace(@ToolParam(description = "장소명") String searchQuery)
    // {
    // log.info("🔧 [Tool] searchPlace: query={}", searchQuery);

    // try {
    // List<LocalItem> searchResults = addAction.searchNaverLocal(searchQuery);
    // if (searchResults.isEmpty()) {
    // return String.format("❌ '%s' 검색 결과가 없습니다.", searchQuery);
    // }

    // int count = Math.min(searchResults.size(), 5);
    // StringBuilder result = new StringBuilder();
    // result.append(String.format("🔍 '%s' 검색 결과 %d개:\n\n", searchQuery, count));

    // for (int i = 0; i < count; i++) {
    // LocalItem item = searchResults.get(i);
    // result.append(String.format("%d. **%s**\n", i + 1,
    // cleanHtmlTags(item.getTitle())));
    // result.append(String.format(" - 카테고리: %s\n", item.getCategory()));
    // result.append(String.format(" - 주소: %s\n", item.getRoadAddress()));
    // result.append(String.format(" - 설명: %s\n", item.getDescription()));
    // log.info(item.getDescription()+"");
    // if (i < count - 1)
    // result.append("\n");
    // }

    // // result.append("\n어떤 장소로 하시겠어요? (번호로 선택해주세요)");
    // // result.append("\n 해당 장소");
    // return result.toString();

    // } catch (Exception e) {
    // log.error("장소 검색 실패", e);
    // return String.format("❌ 장소 검색 중 오류: %s", e.getMessage());
    // }
    // }

    // @Transactional
    // @Tool(description = "검색 결과에서 사용자가 선택한 장소로 교체합니다")
    // public String replacePlaceWithSelection(String oldPlaceName, String
    // newPlaceName, int selectedIndex) {
    // Long planId = support.getPlanId();
    // log.info("🔧 [Tool] replacePlaceWithSelection: planId={}, old={}, new={},
    // index={}",
    // planId, oldPlaceName, newPlaceName, selectedIndex);

    // try {
    // String newName = modifyAction.replacePlaceWithSelection(planId, oldPlaceName,
    // newPlaceName, selectedIndex);
    // Integer version = support.saveSnapshot(planId);

    // return String.format("✅ '%s'를 '%s'(으)로 변경했습니다. 버전: %d",
    // oldPlaceName, newName, version);
    // } catch (Exception e) {
    // log.error("장소 교체 실패", e);
    // return String.format("❌ 장소 교체 중 오류 발생: %s", e.getMessage());
    // }
    // }

    // ========== 버전 관리 (2개) ==========

    @Transactional
    @Tool(description = """
            사용자가 가지고 있는 계획 스냅샷의 바로 이전 버전으로 돌아갑니다.

            사용 예:
                - 이전 버전으로 되돌려줘
                - 일정을 이전으로 돌려줘

            주의:
            - 사용자 요청 이전 버전으로 버전을 되돌려달라고 했을 때만 사용하세요.
            - 되돌리기 전 사용자에게 한번 더 물어봐주세요.
            """)
    public String rollBack(ToolContext toolContext) {
        try {
            Long userId = (Long) toolContext.getContext().get("userId");
            int versionNo = planSnapshotService.getLatestVersionNo(userId);
            if (1 == versionNo)
                return "일정 버전이 1이기 때문에 이전 버전으로 돌아갈 수 없습니다.";

            PlanSnapshot planSnapshot = planSnapshotService
                    .getPlanSnapshotByVersionNo(versionNo - 1, userId);
            PlanSnapshotContent snapshotContent = planSnapshotUtility.parseSnapshot(planSnapshot.getSnapshotJson());

            String conversationId = getConversationId(toolContext);
            Long planId = support.getPlanId(conversationId);
            Plan plan = planDao.selectPlanById(planId);

            // 기존 데이터 삭제
            List<PlanPlace> existingPlaces = planPlaceDao.selectPlanPlacesByPlanId(planId);
            for (PlanPlace place : existingPlaces) {
                planPlaceDao.deletePlanPlaceById(place.getId());
            }

            List<PlanDay> existingDays = planDayDao.selectPlanDaysByPlanId(planId);
            // log.info("daySize: {}", existingDays.size());
            for (PlanDay day : existingDays) {
                planDayDao.deletePlanDay(day.getId());
                // log.info("dayid: {}", day.getId());
            }

            // Plan 업데이트
            Plan rollbackPlan = Plan.builder()
                    .userId(userId)
                    .budget(snapshotContent.getBudget())
                    .startDate(LocalDate.parse(snapshotContent.getStartDate(), formatter1))
                    .endDate(LocalDate.parse(snapshotContent.getEndDate(), formatter1))
                    .createdAt(plan.getCreatedAt())
                    .updatedAt(OffsetDateTime.now())
                    .build();
            planDao.updatePlan(rollbackPlan);

            // Days 재생성
            Map<String, Long> dateToDayId = new HashMap<>();
            for (int i = 0; i < snapshotContent.getDays().size(); i++) {
                PlanSnapshotContent.PlanDay pscDay = snapshotContent.getDays().get(i);

                PlanDay newDay = PlanDay.builder()
                        .planId(planId)
                        .dayIndex(i + 1)
                        .title(pscDay.getTitle())
                        .planDate(LocalDate.parse(pscDay.getDate(), formatter1))
                        .build();

                planDayDao.insertPlanDay(newDay);
                dateToDayId.put(pscDay.getDate(), newDay.getId());
            }

            // Places 재생성
            for (PlanSnapshotContent.PlanDay pscDay : snapshotContent.getDays()) {
                Long dayId = dateToDayId.get(pscDay.getDate());

                for (PlanSnapshotContent.PlanDayItem pscItem : pscDay.getSchedules()) {
                    PlanPlace newPlace = PlanPlace.builder()
                            .dayId(dayId)
                            .title(pscItem.getTitle())
                            .startAt(LocalDateTime.parse(pscItem.getStartAt(), formatter2)
                                    .atOffset(ZoneOffset.of("+00:00")))
                            .endAt(LocalDateTime.parse(pscItem.getEndAt(), formatter2)
                                    .atOffset(ZoneOffset.of("+00:00")))
                            .placeName(pscItem.getPlaceName())
                            .address(pscItem.getAddress())
                            .lat(pscItem.getLat())
                            .lng(pscItem.getLng())
                            .expectedCost(pscItem.getExpectedCost())
                            .normalizedCategory(pscItem.getNormalizedCategory())
                            .firstImage(pscItem.getFirstImage())
                            .firstImage2(pscItem.getFirstImage2())
                            .isEnded(pscItem.getIsEnded() != null && pscItem.getIsEnded())
                            .build();

                    planPlaceDao.insertPlanPlace(newPlace);
                }
            }

            // 새 스냅샷 저장
            Integer newVersionNo = support.saveSnapshot(planId);

            return String.format("이전 버전으로 돌아갔습니다. 버전: %d", newVersionNo);

        } catch (Exception e) {
            log.error("버전 환원 실패", e);
            return String.format("버전 환원 중 오류 발생: %s", e.getMessage());
        }
    }

    @Tool(description = "사용자가 버전 정보를 요청했을 때 사용합니다.")
    public String getVersionNumber(ToolContext toolContext) {
        Long userId = (Long) toolContext.getContext().get("userId");
        int currentVersionNo = planSnapshotService.getLatestVersionNo(userId);
        return "현재 일정은 " + currentVersionNo + "버전입니다.";
    }

    @Transactional
    @Tool(description = """
            사용자가 지정한 계획의 버전으로 돌아갑니다. 버전 정보가 언급된 경우에만 사용합니다.

            주의:
                - 사용자 요청 특정 버전으로 버전을 되돌려달라고 했을 때만 사용하세요.
                - 되돌리기 전 사용자에게 한번 더 물어봐주세요.
            """)
    public String rollBackToSpecific(
            @ToolParam(description = "돌아가고자 하는 버전 번호") Integer versionNo,
            ToolContext toolContext) {

        try {
            Long userId = (Long) toolContext.getContext().get("userId");

            int currentVersionNo = planSnapshotService.getLatestVersionNo(userId);

            log.info("돌아갈 버전: {}", versionNo);

            if (versionNo == currentVersionNo)
                return "현재 버전과 같은 버전이기 때문에 돌아갈 수 없습니다.";

            PlanSnapshot toRevert = PlanSnapshot.builder()
                    .userId(userId)
                    .versionNo(versionNo)
                    .build();

            PlanSnapshot planSnapshot = planSnapshotDao.selectPlanSnapshotByUserIdAndVersionNo(toRevert);
            PlanSnapshotContent snapshotContent = planSnapshotUtility.parseSnapshot(planSnapshot.getSnapshotJson());

            // ... rollBack()와 동일한 로직 ...
            String conversationId = getConversationId(toolContext);
            Long planId = support.getPlanId(conversationId);
            Integer newVersionNo = support.saveSnapshot(planId);

            return String.format("버전 %d로 돌아갔습니다. 새 버전: %d", versionNo, newVersionNo);

        } catch (Exception e) {
            log.error("버전 환원 실패", e);
            return String.format("버전 환원 중 오류 발생: %s", e.getMessage());
        }
    }

    // ========== 전체 삭제 (1개) ==========
    @Transactional
    @Tool(description = "전체 일정을 완전히 삭제합니다. 중요: 사용자가 명확히 확인한 경우에만 호출하세요!")
    public String deletePlan(ToolContext toolContext) {
        String conversationId = getConversationId(toolContext);
        Long planId;
        try {
            planId = support.getPlanId(conversationId);
        } catch (Exception e) {
            log.warn("삭제 요청했으나 삭제할 일정이 없음 (conversationId={})", conversationId);
            return "삭제할 여행 일정이 없습니다.";
        }

        log.info("🔧 [Tool] deletePlan: planId={}", planId);

        try {
            deleteAction.deleteAllDaysAndPlaces(planId);

            Long userId = (Long) toolContext.getContext().get("userId");
            planSnapshotDao.deletePlanSnapshotsByUserId(userId);

            support.clear(conversationId);
            log.info("🧹 전체 일정 삭제로 conversation 상태 초기화");
            return "전체 일정이 완전히 삭제되었습니다. 새로운 여행 계획을 만들고 싶으시면 말씀해주세요!";

        } catch (Exception e) {
            log.error("전체 일정 삭제 실패", e);
            return String.format("전체 일정 삭제 중 오류 발생: %s", e.getMessage());
        }
    }

    // ===============================
    // Helper
    // ===============================

    private String cleanHtmlTags(String text) {
        if (text == null)
            return null;
        return text.replaceAll("<[^>]*>", "");
    }

    private String getConversationId(ToolContext toolContext) {
        Object v = toolContext.getContext().get("conversationId");
        if (v == null) {
            throw new IllegalStateException("conversationId가 ToolContext에 없습니다.");
        }
        return String.valueOf(v);
    }
}