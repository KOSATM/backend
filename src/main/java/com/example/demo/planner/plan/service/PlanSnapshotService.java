package com.example.demo.planner.plan.service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    // ### 단순 CRUD ###
    // ID로 개별 스냅샷 조회
    public PlanSnapshot getPlanSnapshotById(Long id) {
        log.info("Getting travel plan snapshot by id: {}", id);
        return planSnapshotDao.selectPlanSnapshotById(id);
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

    // 스냅샷 저장
    public PlanSnapshot savePlanSnapshot(PlanSnapshot planSnapshot) {
        log.info("Saving travel plan snapshot for user: {}", planSnapshot.getUserId());
        planSnapshotDao.insertPlanSnapshot(planSnapshot);
        return planSnapshot;
    }

    // 스냅샷 저장(기본 테이블 사용)
    @Transactional
    public PlanSnapshot savePlanSnapshot(Plan plan, List<PlanDay> planDays, List<PlanPlace> planPlaces) throws Exception {
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
            log.info("planPlaceDao.selectPlanPlacesByPlanDayId(planDay.getId()): {}", planPlaceDao.selectPlanPlacesByPlanDayId(planDay.getId()).toString());
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
