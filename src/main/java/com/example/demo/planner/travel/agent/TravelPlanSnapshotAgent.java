package com.example.demo.planner.travel.agent;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.example.demo.planner.travel.dao.TravelPlanSnapshotDao;
import com.example.demo.planner.travel.dto.entity.TravelPlanSnapshot;
import com.example.demo.planner.travel.dto.response.TravelPlanSnapshotContent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 여행 계획 버전 관리 에이전트
 * 
 * 책임:
 * - 사용자에게 계획 수정 여부 확인
 * - 수정 승인 시 스냅샷 생성
 * - 이전 버전 복원 (rollback)
 * - 버전 간 비교 및 관리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TravelPlanSnapshotAgent {

  private final TravelPlanSnapshotDao travelPlanSnapshotDao;
  private final ChatClient.Builder chatClientBuilder;
  private final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * 사용자에게 계획 수정 여부를 묻고 처리하는 메인 메서드
   */
  public String manageVersionWithUserInteraction(Long userId, String question) {
    log.info("여행 계획 버전 관리 시작 - 사용자: {}, 계획ID: {}", userId);

    String systemPrompt = """
        당신은 여행 계획 버전 관리 어시스턴트입니다.

        사용자에게 다음을 제시하고 대답을 기다리세요:
        1. 현재 여행 계획이 맞는지 확인
        2. 수정이 필요하면 구체적인 수정사항 물어보기
        3. 수정 승인 시 스냅샷 생성
        4. 필요시 이전 버전으로 복원할 수 있음을 알려주기

        사용자의 요청에 따라 적절한 도구를 호출하세요.
        - 버전 정보가 주어진 경우 해당 버전 상세 조회 -> `getVersionDetails` 사용
        - 사용자가 수정을 지시할 경우 최신 버전 조회, 지시에 따른 수정 후 표시 -> `generateSnapshot` 사용
        """;

    ChatClient chatClient = chatClientBuilder.build();
    VersionTools tools = new VersionTools(userId);

    String response = chatClient.prompt()
        .system(systemPrompt)
        // .user("사용자 ID: " + userId + ", 계획 ID: " + planId + ", 현재 계획: " +
        // currentPlanJson)
        .user("사용자 ID: " + userId + "지시: " + question)
        .tools(tools)
        .call()
        .content();

    log.info("버전 관리 상호작용 완료");
    return response;
  }

  /**
   * 버전 관리 도구 모음
   */
  class VersionTools {
    private final Long userId;
    // private final Long planId;
    // private final String currentPlanJson;

    public VersionTools(Long userId) {
      this.userId = userId;
      // this.planId = planId;
      // this.currentPlanJson = currentPlanJson;
    }

    /**
     * 현재 여행 계획을 스냅샷으로 저장합니다.
     * 사용자가 계획에 만족하거나 수정을 완료했을 때 호출됩니다.
     * @throws JsonProcessingException 
     * @throws JsonMappingException 
     */
    @Tool(description = "최신 여행 계획 스냅샷을 기반으로 사용자가 요청한 부분을 수정하여 새로운 스냅샷을 생성합니다.", returnDirect = true)
    public Object createModifiedSnapshot(
        @ToolParam(description = "수정할 일정의 날짜 (예: 2025-12-21)") String date,
        @ToolParam(description = "수정할 일정 제목 (예: '강남 카페 투어')") String scheduleTitle,
        @ToolParam(description = "사용자가 원하는 새로운 내용 (예: '성수동 카페 투어')") String newTitle) throws JsonMappingException, JsonProcessingException {
      // 내부에서 userId 기반 최신 스냅샷 조회
      TravelPlanSnapshot latest = travelPlanSnapshotDao.selectLatestTravelPlanSnapshotByUserId(userId);

      // snapshotJson 파싱
      TravelPlanSnapshotContent content = objectMapper.readValue(latest.getSnapshotJson(), TravelPlanSnapshotContent.class);

      // 해당 날짜/일정 찾아서 title 변경
      content.getDays().stream()
          .filter(d -> d.getDate().equals(date))
          .flatMap(d -> d.getSchedules().stream())
          .filter(s -> s.getTitle().equals(scheduleTitle))
          .forEach(s -> s.setTitle(newTitle));

      // 새로운 스냅샷 생성
      TravelPlanSnapshot newSnapshot = new TravelPlanSnapshot();
      newSnapshot.setUserId(userId);
      newSnapshot.setVersionNo(latest.getVersionNo() + 1);
      newSnapshot.setSnapshotJson(objectMapper.writeValueAsString(content));
      newSnapshot.setCreatedAt(OffsetDateTime.now());

      // DB에 저장 후 반환
      // travelPlanSnapshotDao.insertTravelPlanSnapshot(newSnapshot);

      return content;
    }

    /**
     * 현재 여행 계획을 스냅샷으로 저장합니다.
     * 사용자가 계획에 만족하거나 수정을 완료했을 때 호출됩니다.
     */
    @Tool(description = "현재 여행 계획을 스냅샷으로 저장합니다. 사용자가 계획을 승인했을 때 호출하세요.", returnDirect = true)
    public String createSnapshot(
        @ToolParam(description = "저장할 여행 계획 JSON") String planJson,
        @ToolParam(description = "이 버전에 대한 설명 (예: 초기 계획, 수정됨 등)") String description) {

      ObjectMapper objectMapper = new ObjectMapper();

      log.info("스냅샷 생성 시작 - 사용자: {}, 계획: {}", userId);

      try {
        // 최신 버전 번호 조회
        TravelPlanSnapshot latest = travelPlanSnapshotDao.selectLatestTravelPlanSnapshotByUserId(userId);
        int nextVersionNo = (latest != null) ? latest.getVersionNo() + 1 : 1;

        // 새 스냅샷 생성
        TravelPlanSnapshot snapshot = new TravelPlanSnapshot();
        snapshot.setUserId(userId);
        snapshot.setVersionNo(nextVersionNo);
        snapshot.setSnapshotJson(planJson);
        snapshot.setCreatedAt(OffsetDateTime.now());

        return objectMapper.writeValueAsString(snapshot);

        // 저장
        // TravelPlanSnapshot saved =
        // travelPlanSnapshotService.saveTravelPlanSnapshot(snapshot);

        // log.info("스냅샷 생성 완료 - 스냅샷ID: {}, 버전: {}", saved.getId(), nextVersionNo);
        // return String.format("버전 %d으로 여행 계획이 저장되었습니다. (스냅샷 ID: %d)%n설명: %s",
        // nextVersionNo, saved.getId(), description);

      } catch (Exception e) {
        log.error("스냅샷 생성 실패", e);
        return "스냅샷 생성 중 오류 발생: " + e.getMessage();
      }
    }

    /**
     * 이전 버전의 목록을 조회합니다.
     */
    @Tool(description = "사용자의 모든 여행 계획 버전을 조회합니다. 복원하고 싶은 버전을 선택할 때 사용하세요.")
    public String getVersionHistory() {
      log.info("버전 이력 조회 - 사용자: {}", userId);

      try {
        List<TravelPlanSnapshot> snapshots = travelPlanSnapshotDao.selectTravelPlanSnapshotsByUserId(userId);

        if (snapshots.isEmpty()) {
          return "저장된 버전이 없습니다.";
        }

        StringBuilder sb = new StringBuilder("저장된 여행 계획 버전:\n");
        snapshots.forEach(snapshot -> {
          sb.append(String.format("- 버전 %d (ID: %d, 생성일: %s)%n",
              snapshot.getVersionNo(),
              snapshot.getId(),
              snapshot.getCreatedAt()))
              .append("  미리보기: ").append(truncateJson(snapshot.getSnapshotJson(), 100)).append("\n");
        });

        return sb.toString();

      } catch (Exception e) {
        log.error("버전 이력 조회 실패", e);
        return "버전 이력 조회 중 오류 발생: " + e.getMessage();
      }
    }

    /**
     * 이전 버전을 복원합니다.
     * 복원 방식: 이전 버전을 조회 → JSON 복사 → 버전+1 → 새로운 행으로 저장
     */
    @Tool(description = "이전 버전의 여행 계획을 복원합니다. 이전 계획으로 되돌리고 싶을 때 호출하세요.")
    public String restorePreviousVersion(
        @ToolParam(description = "복원할 버전 번호") Integer targetVersionNo) {

      ObjectMapper objectMapper = new ObjectMapper();

      log.info("버전 복원 시작 - 사용자: {}, 대상버전: {}", userId, targetVersionNo);

      try {
        // 복원할 버전 조회
        List<TravelPlanSnapshot> allSnapshots = travelPlanSnapshotDao.selectTravelPlanSnapshotsByUserId(userId);
        TravelPlanSnapshot targetSnapshot = allSnapshots.stream()
            .filter(s -> s.getVersionNo().equals(targetVersionNo))
            .findFirst()
            .orElse(null);

        if (targetSnapshot == null) {
          log.warn("복원할 버전을 찾을 수 없음 - 버전: {}", targetVersionNo);
          return String.format("버전 %d을(를) 찾을 수 없습니다.", targetVersionNo);
        }

        // 최신 버전 번호 조회
        Integer maxVersionNo = allSnapshots.stream()
            .map(TravelPlanSnapshot::getVersionNo)
            .max(Integer::compareTo)
            .orElse(0);

        // 새 스냅샷 생성 (버전+1)
        TravelPlanSnapshot restoredSnapshot = new TravelPlanSnapshot();
        restoredSnapshot.setUserId(userId);
        restoredSnapshot.setVersionNo(maxVersionNo + 1);
        restoredSnapshot.setSnapshotJson(targetSnapshot.getSnapshotJson());
        restoredSnapshot.setCreatedAt(OffsetDateTime.now());

        return objectMapper.writeValueAsString(restoredSnapshot);

        // 저장
        // int saved = travelPlanSnapshotDao.insertTravelPlanSnapshot(restoredSnapshot);

        // log.info("버전 복원 완료 - 원본버전: {}, 새버전: {}", targetVersionNo, maxVersionNo + 1);
        // return String.format(
        // "버전 %d의 여행 계획이 복원되었습니다.%n"
        // + "새로운 버전: %d (스냅샷 ID: %d)%n"
        // + "이전 버전은 보존됩니다.",
        // targetVersionNo, maxVersionNo + 1, saved.getId());

      } catch (Exception e) {
        log.error("버전 복원 실패", e);
        return "버전 복원 중 오류 발생: " + e.getMessage();
      }
    }

    /**
     * 두 버전을 비교하여 차이점을 표시합니다.
     */
    @Tool(description = "두 개의 여행 계획 버전을 비교하여 차이점을 보여줍니다.")
    public String compareVersions(
        @ToolParam(description = "첫 번째 버전 번호") Integer version1No,
        @ToolParam(description = "두 번째 버전 번호") Integer version2No) {

      log.info("버전 비교 시작 - 버전1: {}, 버전2: {}", version1No, version2No);

      try {
        List<TravelPlanSnapshot> allSnapshots = travelPlanSnapshotDao.selectTravelPlanSnapshotsByUserId(userId);

        TravelPlanSnapshot snapshot1 = allSnapshots.stream()
            .filter(s -> s.getVersionNo().equals(version1No))
            .findFirst()
            .orElse(null);

        TravelPlanSnapshot snapshot2 = allSnapshots.stream()
            .filter(s -> s.getVersionNo().equals(version2No))
            .findFirst()
            .orElse(null);

        if (snapshot1 == null || snapshot2 == null) {
          return "비교할 버전을 찾을 수 없습니다.";
        }

        // JSON 비교
        JsonNode json1 = objectMapper.readTree(snapshot1.getSnapshotJson());
        JsonNode json2 = objectMapper.readTree(snapshot2.getSnapshotJson());

        StringBuilder comparison = new StringBuilder();
        comparison.append(String.format("=== 버전 %d vs 버전 %d 비교 ===%n", version1No, version2No));

        // 예산 비교
        if (!json1.path("budget").equals(json2.path("budget"))) {
          comparison.append(String.format("예산: %s → %s%n",
              json1.path("budget").asText(),
              json2.path("budget").asText()));
        }

        // 기간 비교
        if (!json1.path("startDate").equals(json2.path("startDate"))) {
          comparison.append(String.format("시작일: %s → %s%n",
              json1.path("startDate").asText(),
              json2.path("startDate").asText()));
        }

        if (!json1.path("endDate").equals(json2.path("endDate"))) {
          comparison.append(String.format("종료일: %s → %s%n",
              json1.path("endDate").asText(),
              json2.path("endDate").asText()));
        }

        // 일정 수 비교
        int days1 = json1.path("days").size();
        int days2 = json2.path("days").size();
        if (days1 != days2) {
          comparison.append(String.format("여행 일정 수: %d일 → %d일%n", days1, days2));
        }

        if (comparison.length() == String.format("=== 버전 %d vs 버전 %d 비교 ===%n", version1No, version2No).length()) {
          comparison.append("두 버전이 동일합니다.\n");
        }

        return comparison.toString();

      } catch (Exception e) {
        log.error("버전 비교 실패", e);
        return "버전 비교 중 오류 발생: " + e.getMessage();
      }
    }

    /**
     * 특정 버전의 상세 정보를 조회합니다.
     */
    @Tool(description = "특정 버전의 여행 계획 상세 정보를 조회합니다. 데이터베이스에서 SELECT 쿼리를 실행하고 JSON 데이터를 반환합니다. INSERT/UPDATE/DELETE는 실행할 수 없습니다.", returnDirect = true)
    public Object getVersionDetails(
        @ToolParam(description = "조회할 버전 번호") Integer versionNo) {

      log.info("버전 상세 조회 - 버전: {}", versionNo);

      try {
        List<TravelPlanSnapshot> allSnapshots = travelPlanSnapshotDao.selectTravelPlanSnapshotsByUserId(userId);
        TravelPlanSnapshot snapshot = allSnapshots.stream()
            .filter(s -> s.getVersionNo() == versionNo)
            .findFirst()
            .orElse(null);

        if (snapshot == null) {
          log.info("버전 %d을(를) 찾을 수 없습니다.", versionNo);
          return String.format("버전 %d을(를) 찾을 수 없습니다.", versionNo);
        }

        log.info("snapshot: {}", snapshot.toString());
        // snapshot.getSnapshotJson();
        TravelPlanSnapshotContent content = objectMapper.readValue(snapshot.getSnapshotJson(),
            TravelPlanSnapshotContent.class);

        return content;

        // JsonNode planJson = objectMapper.readTree(snapshot.getSnapshotJson());

        // StringBuilder details = new StringBuilder();
        // details.append(String.format("=== 버전 %d 상세 정보 ===%n", versionNo));
        // details.append(String.format("생성일: %s%n", snapshot.getCreatedAt()));
        // details.append(String.format("예산: %s원%n", planJson.path("budget").asText()));
        // details.append(String.format("기간: %s ~ %s%n",
        // planJson.path("startDate").asText(),
        // planJson.path("endDate").asText()));
        // details.append(String.format("일정 수: %d일%n", planJson.path("days").size()));

        // 각 일정 요약
        // details.append("\n주요 일정:\n");
        // planJson.path("days").forEach(day -> {
        // details.append(String.format("- %s: %s%n",
        // day.path("date").asText(),
        // day.path("title").asText()));
        // });

        // return details.toString();

      } catch (Exception e) {
        log.error("버전 상세 조회 실패", e);
        return "버전 상세 조회 중 오류 발생: " + e.getMessage();
      }
    }

    /**
     * JSON을 지정된 길이로 자릅니다.
     */
    private String truncateJson(String json, int maxLength) {
      if (json.length() <= maxLength) {
        return json;
      }
      return json.substring(0, maxLength) + "...";
    }
  }

  /**
   * 특정 버전을 직접 복원하는 메서드 (비도구 방식)
   */
  // public TravelPlanSnapshot restoreVersionDirect(Long userId, Integer
  // targetVersionNo) {
  // log.info("버전 직접 복원 - 사용자: {}, 버전: {}", userId, targetVersionNo);

  // try {
  // List<TravelPlanSnapshot> allSnapshots =
  // travelPlanSnapshotDao.selectTravelPlanSnapshotsByUserId(userId);

  // TravelPlanSnapshot targetSnapshot = allSnapshots.stream()
  // .filter(s -> s.getVersionNo().equals(targetVersionNo))
  // .findFirst()
  // .orElseThrow(() -> new IllegalArgumentException("버전을 찾을 수 없습니다"));

  // Integer maxVersionNo = allSnapshots.stream()
  // .map(TravelPlanSnapshot::getVersionNo)
  // .max(Integer::compareTo)
  // .orElse(0);

  // TravelPlanSnapshot restoredSnapshot = new TravelPlanSnapshot();
  // restoredSnapshot.setUserId(userId);
  // restoredSnapshot.setVersionNo(maxVersionNo + 1);
  // restoredSnapshot.setSnapshotJson(targetSnapshot.getSnapshotJson());
  // restoredSnapshot.setCreatedAt(OffsetDateTime.now());

  // TravelPlanSnapshot saved =
  // travelPlanSnapshotDao.saveTravelPlanSnapshot(restoredSnapshot);
  // log.info("버전 직접 복원 완료 - 새버전: {}", maxVersionNo + 1);

  // return saved;

  // } catch (Exception e) {
  // log.error("버전 직접 복원 실패", e);
  // throw new RuntimeException("버전 복원 실패: " + e.getMessage(), e);
  // }
  // }
}
