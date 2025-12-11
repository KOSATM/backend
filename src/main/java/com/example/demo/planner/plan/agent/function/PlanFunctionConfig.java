package com.example.demo.planner.plan.agent.function;

import java.util.function.Function;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import com.example.demo.planner.plan.service.action.PlanActionExecutor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 🛠️ PlanFunctionConfig - LLM Function Calling 설정
 *
 * Spring AI의 Function Calling을 사용하여 LLM이 필요시 Java 메서드를 직접 호출할 수 있도록 함
 *
 * 아키텍처:
 * SmartPlanAgent → LLM (Function Calling) → PlanFunctionConfig (이 클래스)
 *   → PlanActionExecutor → PlanService → DAO
 *
 * 등록된 함수:
 * - deletePlaceByName: 장소명으로 일정에서 장소 삭제
 * - swapPlacesInDay: 같은 날짜 내에서 두 장소의 순서 교환
 * - swapPlacesBetweenDays: 서로 다른 날짜 간 장소 교환
 *
 * LLM이 사용자 요청을 분석하여 적절한 함수를 자동으로 선택하고 호출함
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class PlanFunctionConfig {

    private final PlanActionExecutor planActionExecutor;

    /**
     * 장소 삭제 함수
     * LLM이 "창경궁 삭제해줘" 같은 요청을 받으면 자동으로 이 함수를 호출
     */
    @Bean
    @Description("일정에서 특정 장소를 삭제합니다. 장소명과 planId가 필요합니다.")
    public Function<DeletePlaceRequest, DeletePlaceResponse> deletePlaceByName() {
        return request -> {
            log.info("📞 [Function] deletePlaceByName 호출: planId={}, placeName={}",
                    request.planId(), request.placeName());

            // PlanActionExecutor로 위임
            String result = planActionExecutor.deletePlace(request.planId(), request.placeName());
            boolean success = result.startsWith("✅");

            return new DeletePlaceResponse(success, result);
        };
    }

    /**
     * 같은 날짜 내 장소 순서 교환 함수
     * LLM이 "1일차의 첫 번째와 두 번째 장소 순서를 바꿔줘" 같은 요청을 받으면 호출
     */
    @Bean
    @Description("같은 날짜 내에서 두 장소의 순서를 교환합니다.")
    public Function<SwapPlacesInDayRequest, SwapPlacesResponse> swapPlacesInDay() {
        return request -> {
            log.info("📞 [Function] swapPlacesInDay 호출: planId={}, day={}, place1={}, place2={}",
                    request.planId(), request.dayIndex(), request.placeIndex1(), request.placeIndex2());

            // PlanActionExecutor로 위임
            String result = planActionExecutor.swapPlaces(
                request.planId(),
                request.dayIndex(),
                request.placeIndex1(),
                request.placeIndex2()
            );
            boolean success = result.startsWith("✅");

            return new SwapPlacesResponse(success, result);
        };
    }

    /**
     * 서로 다른 날짜 간 장소 교환 함수
     * LLM이 "1일차의 첫 번째 장소와 2일차의 두 번째 장소를 바꿔줘" 같은 요청을 받으면 호출
     */
    @Bean
    @Description("서로 다른 날짜 간에 장소를 교환합니다.")
    public Function<SwapPlacesBetweenDaysRequest, SwapPlacesResponse> swapPlacesBetweenDays() {
        return request -> {
            log.info("📞 [Function] swapPlacesBetweenDays 호출: planId={}, day1={}[{}], day2={}[{}]",
                    request.planId(), request.dayIndex1(), request.placeIndex1(),
                    request.dayIndex2(), request.placeIndex2());

            // PlanActionExecutor로 위임
            String result = planActionExecutor.swapPlacesBetweenDays(
                request.planId(),
                request.dayIndex1(),
                request.placeIndex1(),
                request.dayIndex2(),
                request.placeIndex2()
            );
            boolean success = result.startsWith("✅");

            return new SwapPlacesResponse(success, result);
        };
    }

    // DTO 정의

    /**
     * 장소 삭제 요청
     */
    public record DeletePlaceRequest(
        Long planId,
        String placeName
    ) {}

    /**
     * 장소 삭제 응답
     */
    public record DeletePlaceResponse(
        boolean success,
        String message
    ) {}

    /**
     * 같은 날짜 내 장소 순서 교환 요청
     */
    public record SwapPlacesInDayRequest(
        Long planId,
        int dayIndex,
        int placeIndex1,
        int placeIndex2
    ) {}

    /**
     * 날짜 간 장소 교환 요청
     */
    public record SwapPlacesBetweenDaysRequest(
        Long planId,
        int dayIndex1,
        int placeIndex1,
        int dayIndex2,
        int placeIndex2
    ) {}

    /**
     * 장소 교환 응답 (공통)
     */
    public record SwapPlacesResponse(
        boolean success,
        String message
    ) {}

    /**
     * 장소 교체 함수 (네이버 검색 사용)
     * LLM이 "덕수궁을 창경궁으로 바꿔줘" 같은 요청을 받으면 호출
     */
    @Bean
    @Description("기존 장소를 새로운 장소로 교체합니다. 네이버 검색으로 새 장소 정보를 가져와 업데이트합니다.")
    public Function<ReplacePlaceRequest, ReplacePlaceResponse> replacePlace() {
        return request -> {
            log.info("📞 [Function] replacePlace 호출: planId={}, {} → {}",
                    request.planId(), request.oldPlaceName(), request.newPlaceName());

            // PlanActionExecutor로 위임
            String result = planActionExecutor.replacePlace(
                request.planId(),
                request.oldPlaceName(),
                request.newPlaceName()
            );
            boolean success = result.startsWith("✅");

            return new ReplacePlaceResponse(success, result);
        };
    }

    /**
     * 장소 교체 요청
     */
    public record ReplacePlaceRequest(
        Long planId,
        String oldPlaceName,
        String newPlaceName
    ) {}

    /**
     * 장소 교체 응답
     */
    public record ReplacePlaceResponse(
        boolean success,
        String message
    ) {}
}
