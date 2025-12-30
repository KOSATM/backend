package com.example.demo.planner.plan.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.planner.plan.dao.PlanDao;
import com.example.demo.planner.plan.dao.PlanDayDao;
import com.example.demo.planner.plan.dao.PlanPlaceDao;
import com.example.demo.planner.plan.dao.PlanSnapshotDao;
import com.example.demo.planner.plan.dto.entity.Plan;
import com.example.demo.planner.plan.dto.entity.PlanDay;
import com.example.demo.planner.plan.dto.entity.PlanPlace;
import com.example.demo.planner.plan.dto.entity.PlanSnapshot;
import com.example.demo.planner.plan.dto.response.PlanSnapshotContent;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * planSnapshotService는 여행 계획 스냅샷 관련 비즈니스 로직을 처리합니다.
 * 
 * 스냅샷은 여행 계획의 버전 관리와 히스토리 추적을 위해 사용됩니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanSnapshotService {
    private final PlanSnapshotDao planSnapshotDao;
    private final PlanPlaceDao planPlaceDao;
    private final PlanDao planDao;
    private final PlanDayDao planDayDao;
    DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ### 단순 CRUD ###
    // ID로 개별 스냅샷 조회
    public PlanSnapshot getPlanSnapshotById(Long id) {
        log.info("Getting travel plan snapshot by id: {}", id);
        return planSnapshotDao.selectPlanSnapshotById(id);
    }

    public PlanSnapshot getPlanSnapshotByVersionNo(int versionNo, Long userId) {
        log.info("Getting travel plan snapshot by vesion: {}, id: {}", versionNo, userId);
        return planSnapshotDao.selectPlanSnapshotByVersionNo(versionNo, userId);
    }

    // 사용자의 모든 스냅샷 조회
    public List<PlanSnapshot> getPlanSnapshotsByUserId(Long userId) {
        log.info("Getting all travel plan snapshots for user: {}", userId);
        return planSnapshotDao.selectPlanSnapshotsByUserId(userId);
    }

    // 사용자의 최신 스냅샷 조회
    public PlanSnapshot getLatestPlanSnapshot(Long userId) {
        log.info("Getting latest travel plan snapshot for user: {}", userId);
        return planSnapshotDao.selectLatestPlanSnapshotByUserId(userId);
    }

    // 사용자의 최신 스냅샷 버전No조회
    public int getLatestVersionNo(Long userId) {
        log.info("Getting latest travel plan snapshot ID for user: {}", userId);
        return planSnapshotDao.selectLatestVersionNoByUserId(userId);
    }

    // 스냅샷 저장
    public PlanSnapshot savePlanSnapshot(PlanSnapshot planSnapshot) {
        log.info("Saving travel plan snapshot for user: {}", planSnapshot.getUserId());
        planSnapshotDao.insertPlanSnapshot(planSnapshot);
        return planSnapshot;
    }

    // 스냅샷 저장(기본 테이블 사용)
    @Transactional
    public PlanSnapshot savePlanSnapshot(Plan plan, List<PlanDay> planDays, List<PlanPlace> planPlaces)
            throws Exception {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        ObjectMapper objectMapper = new ObjectMapper();

        log.info("Saving travel plan snapshot for user: {}", plan.getUserId());
        // 최신 스냅샷이 있는지 확인 -> 없으면 버전 정보는 1
        PlanSnapshot latestPlanSnapshot = getLatestPlanSnapshot(plan.getUserId());
        Integer versionNo = latestPlanSnapshot == null ? 1 : latestPlanSnapshot.getVersionNo() + 1;

        PlanSnapshotContent planSnapshotContent = new PlanSnapshotContent();
        planSnapshotContent.setUserId(plan.getUserId());
        planSnapshotContent.setBudget(plan.getBudget());
        planSnapshotContent.setStartDate(plan.getStartDate().toString());
        planSnapshotContent.setEndDate(plan.getEndDate().toString());

        List<PlanSnapshotContent.PlanDay> pscDays = new ArrayList<>();
        for (PlanDay planDay : planDays) {
            PlanSnapshotContent.PlanDay pscDay = new PlanSnapshotContent.PlanDay();
            pscDay.setDate(planDay.getPlanDate().toString());
            pscDay.setTitle(planDay.getTitle());

            List<PlanSnapshotContent.PlanDayItem> pscItems = new ArrayList<>();
            log.info("planDay: {}", planDay.toString());
            log.info("planPlaceDao.selectPlanPlacesByPlanDayId(planDay.getId()): {}",
                    planPlaceDao.selectPlanPlacesByPlanDayId(planDay.getId()).toString());
            for (PlanPlace planPlace : planPlaceDao.selectPlanPlacesByPlanDayId(planDay.getId())) {
                PlanSnapshotContent.PlanDayItem pscItem = new PlanSnapshotContent.PlanDayItem();
                pscItem.setTitle(planPlace.getTitle());
                pscItem.setStartAt(planPlace.getStartAt().format(formatter));
                pscItem.setEndAt(planPlace.getEndAt().format(formatter));
                pscItem.setPlaceName(planPlace.getPlaceName());
                pscItem.setAddress(planPlace.getAddress());
                pscItem.setLat(planPlace.getLat());
                pscItem.setLng(planPlace.getLng());
                pscItem.setExpectedCost(planPlace.getExpectedCost());
                pscItem.setNormalizedCategory(planPlace.getNormalizedCategory());
                pscItem.setFirstImage(planPlace.getFirstImage());
                pscItem.setFirstImage2(planPlace.getFirstImage2());
                pscItem.setIsEnded(planPlace.getIsEnded() == null ? false : planPlace.getIsEnded());
                pscItems.add(pscItem);
            }
            pscDay.setSchedules(pscItems);
            pscDays.add(pscDay);
        }
        planSnapshotContent.setDays(pscDays);

        // log.info(planSnapshotContent.toString());
        String snapshotJson = objectMapper.writeValueAsString(planSnapshotContent);

        PlanSnapshot planSnapshot = PlanSnapshot.builder()
                .userId(plan.getUserId())
                .versionNo(versionNo)
                .snapshotJson(snapshotJson)
                .build();

        planSnapshotDao.insertPlanSnapshot(planSnapshot);

        planSnapshot = planSnapshotDao.selectLatestPlanSnapshotByUserId(plan.getUserId());
        return planSnapshot;
    }

    /**
     * 스냅샷으로부터 Plan 복원
     * 
     * @param planId          복원할 Plan ID
     * @param snapshotContent 스냅샷 내용
     * @param userId          사용자 ID
     * @throws Exception 복원 실패 시
     */
    @Transactional
    public void restorePlanFromSnapshot(Long planId, PlanSnapshotContent snapshotContent, Long userId)
            throws Exception {

        log.info("🔄 스냅샷으로부터 Plan 복원 시작: planId={}", planId);

        // 1. 기존 Plan 조회
        Plan existingPlan = planDao.selectPlanById(planId);
        if (existingPlan == null) {
            throw new IllegalArgumentException("Plan not found: " + planId);
        }

        // 2. 기존 Places 삭제
        List<PlanPlace> existingPlaces = planPlaceDao.selectPlanPlacesByPlanId(planId);
        for (PlanPlace place : existingPlaces) {
            planPlaceDao.deletePlanPlaceById(place.getId());
        }
        log.info("기존 Places 삭제 완료: {}개", existingPlaces.size());

        // 3. 기존 Days 삭제
        List<PlanDay> existingDays = planDayDao.selectPlanDaysByPlanId(planId);
        for (PlanDay day : existingDays) {
            planDayDao.deletePlanDay(day.getId());
        }
        log.info("기존 Days 삭제 완료: {}개", existingDays.size());

        // 4. Plan 업데이트
        Plan rollbackPlan = Plan.builder()
                .id(planId) // ← 중요!
                .userId(userId)
                .budget(snapshotContent.getBudget())
                .startDate(LocalDate.parse(snapshotContent.getStartDate(), formatter1))
                .endDate(LocalDate.parse(snapshotContent.getEndDate(), formatter1))
                .createdAt(existingPlan.getCreatedAt())
                .updatedAt(OffsetDateTime.now())
                .build();
        planDao.updatePlan(rollbackPlan);
        log.info("Plan 업데이트 완료");

        // 5. Days 재생성
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
        log.info("Days 재생성 완료: {}개", snapshotContent.getDays().size());

        // 6. Places 재생성
        int totalPlaces = 0;
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
                totalPlaces++;
            }
        }
        log.info("Places 재생성 완료: {}개", totalPlaces);
        log.info("스냅샷 복원 완료");
    }

    /**
     * 스냅샷 버전의 일정을 간략하게 렌더링
     * 
     * @param versionNo 조회할 버전 번호
     * @param userId    사용자 ID
     * @return 간략한 일정 텍스트
     */
    public String renderSnapshotSummary(int versionNo, Long userId) throws Exception {
        log.info("스냅샷 버전 {} 요약 조회", versionNo);

        // 1. 스냅샷 조회
        PlanSnapshot snapshot = planSnapshotDao.selectPlanSnapshotByVersionNo(versionNo, userId);
        if (snapshot == null) {
            return String.format("버전 %d를 찾을 수 없습니다.", versionNo);
        }

        // 2. JSON 파싱
        PlanSnapshotContent content = new PlanSnapshotUtility().parseSnapshot(snapshot.getSnapshotJson());

        // 3. 렌더링
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("📸 버전 %d 일정 (%s ~ %s)\n",
                versionNo,
                content.getStartDate(),
                content.getEndDate()));
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━\n");

        for (PlanSnapshotContent.PlanDay day : content.getDays()) {
            int placeCount = day.getSchedules().size();
            sb.append(String.format("Day %d (%d개 장소)\n",
                    content.getDays().indexOf(day) + 1,
                    placeCount));

            // 처음 3개만 표시
            int displayCount = Math.min(3, placeCount);
            for (int i = 0; i < displayCount; i++) {
                PlanSnapshotContent.PlanDayItem item = day.getSchedules().get(i);
                sb.append(String.format("  • %s\n", item.getTitle()));
            }

            // 나머지가 있으면 표시
            if (placeCount > 3) {
                sb.append(String.format("  ... 외 %d개\n", placeCount - 3));
            }

            sb.append("\n");
        }

        sb.append("━━━━━━━━━━━━━━━━━━━━━━━");

        return sb.toString();
    }

    /**
     * 사용자의 모든 버전 목록 간략 표시
     * 
     * @param userId 사용자 ID
     * @return 버전 목록 텍스트
     */
    public String listAllVersionsSummary(Long userId) throws Exception {
        log.info("사용자 {} 전체 버전 목록 조회", userId);

        List<PlanSnapshot> snapshots = planSnapshotDao.selectPlanSnapshotsByUserId(userId);

        if (snapshots.isEmpty()) {
            return "저장된 버전이 없습니다.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("📋 저장된 버전 목록\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━\n\n");

        for (PlanSnapshot snapshot : snapshots) {
            PlanSnapshotContent content = new PlanSnapshotUtility().parseSnapshot(snapshot.getSnapshotJson());

            sb.append(String.format("버전 %d - %s ~ %s (%d일)\n",
                    snapshot.getVersionNo(),
                    content.getStartDate(),
                    content.getEndDate(),
                    content.getDays().size()));

            // 각 날짜의 장소 개수
            for (int i = 0; i < content.getDays().size(); i++) {
                PlanSnapshotContent.PlanDay day = content.getDays().get(i);
                sb.append(String.format("  Day %d: %d개 장소\n",
                        i + 1,
                        day.getSchedules().size()));
            }

            sb.append("\n");
        }

        sb.append("━━━━━━━━━━━━━━━━━━━━━━━");

        return sb.toString();
    }

    // 특정 스냅샷 삭제
    public void deletePlanSnapshot(Long id) {
        log.info("Deleting travel plan snapshot: {}", id);
        planSnapshotDao.deletePlanSnapshot(id);
    }

    // 사용자의 모든 스냅샷 삭제
    public void deletePlanSnapshotsByUserId(Long userId) {
        log.info("Deleting all travel plan snapshots for user: {}", userId);
        planSnapshotDao.deletePlanSnapshotsByUserId(userId);
    }

}
