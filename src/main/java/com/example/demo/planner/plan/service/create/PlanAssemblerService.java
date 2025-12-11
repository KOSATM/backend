package com.example.demo.planner.plan.service.create;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.planner.plan.agent.PlanSchedulerAgent;
import com.example.demo.planner.plan.dao.PlanDao;
import com.example.demo.planner.plan.dao.PlanDayDao;
import com.example.demo.planner.plan.dao.PlanPlaceDao;
import com.example.demo.planner.plan.dto.entity.Plan;
import com.example.demo.planner.plan.dto.entity.PlanDay;
import com.example.demo.planner.plan.dto.entity.PlanPlace;
import com.example.demo.planner.plan.dto.entity.PlanScheduleRow;
import com.example.demo.planner.plan.dto.entity.PlanSnapshot;
import com.example.demo.planner.plan.dto.entity.TravelPlaces;
import com.example.demo.planner.plan.dto.response.DayPlanResult;
import com.example.demo.planner.plan.dto.response.PlanDetailResponse;
import com.example.demo.planner.plan.dto.response.PlanScheduleResult;
import com.example.demo.planner.plan.service.PlanSnapshotService;
import com.example.demo.planner.plan.utils.DateTimeUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlanAssemblerService {

    private final PlanSchedulerAgent planSchedulerAgent;
    private final ObjectMapper objectMapper;
    private final PlanSnapshotService planSnapshotService;
    private final PlanDao planDao;
    private final PlanDayDao planDayDao;
    private final PlanPlaceDao planPlaceDao;


    @Transactional
    public PlanDetailResponse createAndSavePlan(
            List<DayPlanResult> dayPlans,
            Map<String, Object> arguments,
            Long userId) {

        LocalDate startDate = LocalDate.parse((String) arguments.get("startDate"));
        int duration = (int) arguments.get("duration");

        // 1) Agent 호출 → JSON 생성
        String scheduleJson = planSchedulerAgent.createTravelPlan(dayPlans, startDate);
        PlanScheduleResult scheduleResult = parseScheduleJson(scheduleJson);

        // 2) placeId → TravelPlaces 매핑 준비
        Map<Long, TravelPlaces> placeInfoMap = extractPlaceInfo(dayPlans);

        // 3) Plan 생성 및 저장
        Plan plan = createPlanEntity(userId, startDate, duration);
        // log.info(plan.toString());
        planDao.insertPlan(plan);

        // 3) PlanDays 생성 및 저장
        List<PlanDay> days = createPlanDayEntity(startDate, plan.getId(), scheduleResult);
        planDayDao.insertPlanDayBatch(days);
        log.info(days.toString() + "<< <<<<");
        List<Long> planDaysIds = planDayDao.selectPlanDayIdsByPlanId(plan.getId());
        

        // 4) 각 날짜 엔티티 생성
        List<PlanPlace> planPlaces = createPlanPlaceEntity(planDaysIds, scheduleResult, startDate, placeInfoMap);
        // for(PlanPlace planPlace : scheduledPlanPlaces){
        //     System.out.println(planPlace.toString());
        //     System.err.println(" >>>");
        // }

        log.info(planPlaces.toString()+"++++++++++++++++++++++++++++++++++");
        planPlaceDao.insertPlanPlaceBatch(planPlaces);

        try {
            PlanSnapshot snapShot = planSnapshotService.savePlanSnapshot(plan, days, planPlaces);
            log.info(snapShot+"..,.,.,.,");
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<PlanScheduleRow> planScheduleRows = planDao.selectPlanWithAllByPlanId(plan.getId());
        PlanDetailResponse reponse = PlanDetailResponse.fromRows(planScheduleRows);
        return reponse;
    }


    // --------------------------------------------------------
    // JSON 파싱
    // --------------------------------------------------------
    private PlanScheduleResult parseScheduleJson(String scheduleJson) {
        try {
            return objectMapper.readValue(scheduleJson, PlanScheduleResult.class);
        } catch (Exception e) {
            throw new RuntimeException("일정 JSON 파싱 실패", e);
        }
    }

    // --------------------------------------------------------
    // Plan 생성
    // --------------------------------------------------------
    private Plan createPlanEntity(Long userId, LocalDate startDate, int duration) {
        return Plan.builder()
                .userId(userId) // 추후 변경
                .budget(BigDecimal.valueOf(0))
                .startDate(startDate)
                .endDate(startDate.plusDays(duration - 1))
                .title(null) // 나중에 제목 Agent 실행 가능
                .isEnded(false)
                .build();
    }

    // --------------------------------------------------------
    // PlanDay 생성
    // --------------------------------------------------------
    private List<PlanDay> createPlanDayEntity(
            LocalDate startDate,
            Long planId,
            PlanScheduleResult scheduleResult) {
        return scheduleResult.getDays().stream()
                .map(d -> PlanDay.builder()
                        .planId(planId)
                        .dayIndex(d.getDayIndex())
                        .planDate(startDate.plusDays(d.getDayIndex() - 1))
                        .title(null) // 제목 따로 생성 가능
                        .build())
                .toList();
    }

    // --------------------------------------------------------
    // 기존 dayPlans 리스트 → Id → TravelPlaces 매핑 테이블 생성
    // --------------------------------------------------------
    private Map<Long, TravelPlaces> extractPlaceInfo(List<DayPlanResult> dayPlans) {
        return dayPlans.stream()
                .flatMap(d -> d.getPlaces().stream())
                .map(c -> c.getOriginal().getTravelPlaces())
                .collect(Collectors.toMap(TravelPlaces::getId, t -> t, (a, b) -> a));
    }

    // --------------------------------------------------------
    // PlanPlace 생성
    // --------------------------------------------------------
    private List<PlanPlace> createPlanPlaceEntity(
            List<Long> planDayIds,
            PlanScheduleResult schedule,
            LocalDate startDate,
            Map<Long, TravelPlaces> placeInfoMap) {
        List<PlanPlace> result = new ArrayList<>();

        for (PlanScheduleResult.Day d : schedule.getDays()) {

            int dayIndex = d.getDayIndex();

            Long planDayId = planDayIds.get(dayIndex - 1);

            for (PlanScheduleResult.Item item : d.getItems()) {

                TravelPlaces p = placeInfoMap.get(item.getId());
                if (p == null) {
                    throw new IllegalStateException(
                            "placeInfo not found for itemId=" + item.getId());
                }
                PlanPlace place = PlanPlace.builder()
                        .dayId(planDayId)
                        .title(p.getTitle())
                        .placeName(p.getTitle())
                        .address(p.getAddress())
                        .lat(p.getLat())
                        .lng(p.getLng())
                        .expectedCost(BigDecimal.ZERO)
                        .startAt(DateTimeUtil.toOffsetDateTime(
                                startDate, dayIndex, item.getStart()))
                        .endAt(DateTimeUtil.toOffsetDateTime(
                                startDate, dayIndex, item.getEnd()))
                        .normalizedCategory(p.getNormalizedCategory())
                        .firstImage(p.getFirstImage())
                        .firstImage2(p.getFirstImage2())
                        .build();

                result.add(place);
                log.info(place.toString()+";;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;");
            }
        }

        return result;
    }
}
