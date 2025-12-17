package com.example.demo.planner.plan.agent.tools;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import com.example.demo.planner.plan.agent.TravelPlanGenerationService;
import com.example.demo.planner.plan.dto.entity.GeneratedTravelPlan;
import com.example.demo.planner.plan.dto.entity.Plan;
import com.example.demo.planner.plan.service.PlanCrudService;
import com.example.demo.planner.plan.utils.date.DateParser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 여행 일정 생성 도구 모음
 * - TravelPlanAgent가 사용
 * - 실제 일정 생성 로직을 분리하여 Tool로 제공
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TravelPlanTools {

    private final TravelPlanGenerationService generationService;
    private final PlanCrudService planCrudService;

    /**
     * 서울 여행 일정 생성 및 DB 저장
     */
    @Tool(description = "서울 여행 일정을 생성하고 DB에 저장합니다")
    public String generateAndSavePlan(
            Long userId,
            Integer duration,
            String style,
            String location,
            String pace,
            String startDateText) {

        log.info("🔧 [Tool] generateAndSavePlan: userId={}, duration={}, style={}, location={}, pace={}",
                userId, duration, style, location, pace);

        try {
            // startDateText를 LocalDate로 변환
            LocalDate startDate = DateParser.parse(startDateText);
            
            // 1. 일정 생성 (기존 로직)
            GeneratedTravelPlan generatedPlan = generationService.generatePlan(
                    duration, style, location, pace, startDate);

            if (generatedPlan.days().isEmpty()) {
                log.warn("⚠️ 장소 검색 결과 없음 - 샘플 데이터로 생성");
                Plan plan = planCrudService.createPlanWithSampleData(
                        userId, duration, null, generatedPlan.startDate());
                return String.format(
                    "{\"success\": true, \"planId\": %d, \"duration\": %d, \"placeCount\": 0, \"message\": \"샘플 데이터로 생성됨\"}",
                    plan.getId(), duration
                );
            }

            // 2. DB 저장
            Plan savedPlan = savePlanToDb(userId, generatedPlan);
            int totalPlaces = generatedPlan.days().stream().mapToInt(d -> d.places().size()).sum();

            log.info("✅ 일정 생성 및 DB 저장 완료: planId={}", savedPlan.getId());
            return String.format(
                "{\"success\": true, \"planId\": %d, \"duration\": %d, \"placeCount\": %d, \"startDate\": \"%s\", \"endDate\": \"%s\"}",
                savedPlan.getId(),
                duration,
                totalPlaces,
                generatedPlan.startDate(),
                generatedPlan.endDate()
            );

        } catch (Exception e) {
            log.error("❌ 일정 생성 실패", e);
            return "❌ 일정 생성에 실패했습니다: " + e.getMessage();
        }
    }

    /**
     * GeneratedTravelPlan을 DB에 저장
     */
    private Plan savePlanToDb(Long userId, GeneratedTravelPlan generatedPlan) {
        log.info("💾 DB 저장 시작: userId={}, duration={}", userId, generatedPlan.duration());

        // 1. Plan 생성 (budget 기본값 0 설정)
        Plan plan = planCrudService.createPlan(Plan.builder()
                .userId(userId)
                .budget(BigDecimal.ZERO)  // ✅ NOT NULL constraint 대응
                .title("서울 여행 계획")
                .startDate(generatedPlan.startDate())
                .endDate(generatedPlan.endDate())
                .isEnded(false)
                .build());

        log.info("✅ Plan 저장 완료: planId={}", plan.getId());

        // 2. 각 일차 및 장소 저장
        for (GeneratedTravelPlan.GeneratedDay genDay : generatedPlan.days()) {
            var day = planCrudService.createPlanDay(plan.getId(), genDay.dayIndex(), genDay.dayDate());
            log.info("✅ Day 저장 완료: dayId={}, dayIndex={}", day.getId(), genDay.dayIndex());

            for (GeneratedTravelPlan.GeneratedPlace genPlace : genDay.places()) {
                planCrudService.createPlanPlace(
                        day.getId(),
                        genPlace.title(),
                        genPlace.placeName(),
                        genPlace.address(),
                        genPlace.lat(),
                        genPlace.lng(),
                        genPlace.startAt(),
                        genPlace.endAt(),
                        genPlace.category(),
                        genPlace.firstImage(),
                        genPlace.firstImage2()
                );
            }
            log.info("✅ Day {} 장소 저장 완료: {}개", genDay.dayIndex(), genDay.places().size());
        }

        log.info("✅ DB 저장 완료: planId={}", plan.getId());
        return plan;
    }
}
