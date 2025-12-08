package com.example.demo.planner.travel.service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.demo.planner.travel.dao.TravelDayDao;
import com.example.demo.planner.travel.dao.TravelPlaceDao;
import com.example.demo.planner.travel.dao.TravelPlanDao;
import com.example.demo.planner.travel.dto.entity.TravelDay;
import com.example.demo.planner.travel.dto.entity.TravelPlace;
import com.example.demo.planner.travel.dto.entity.TravelPlan;
import com.example.demo.planner.travel.dto.response.TravelDayWithPlaces;
import com.example.demo.planner.travel.dto.response.TravelPlanDetail;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TravelService {

    private final TravelPlanDao travelPlanDao;
    private final TravelDayDao travelDayDao;
    private final TravelPlaceDao travelPlaceDao;

    // 여행 계획 생성
    public void createTravelPlan(TravelPlan travelPlan) {
        travelPlanDao.insertTravelPlan(travelPlan);
    }

    // ID로 여행 계획 조회
    public TravelPlan getTravelPlanById(Long id) {
        return travelPlanDao.selectTravelPlanById(id);
    }

    // 사용자 ID로 모든 여행 계획 조회
    public List<TravelPlan> getTravelPlansByUserId(Long userId) {
        return travelPlanDao.selectTravelPlansByUserId(userId);
    }

    // 사용자 ID로 진행중인 여행 계획 조회 (is_ended = false 또는 NULL)
    public TravelPlan getActiveTravelPlanByUserId(Long userId) {
        TravelPlan result = travelPlanDao.selectActiveTravelPlanByUserId(userId);
        System.out.println("=== 디버그: 진행중인 여행 계획 조회 ===");
        System.out.println("userId: " + userId);
        System.out.println("result: " + (result != null ? "플랜 ID: " + result.getId() : "없음"));
        return result;
    }

    // 사용자 ID로 완료된 여행 계획 조회 (is_ended = true)
    public List<TravelPlan> getCompletedTravelPlansByUserId(Long userId) {
        return travelPlanDao.selectCompletedTravelPlansByUserId(userId);
    }

    // 모든 여행 계획 조회
    public List<TravelPlan> getAllTravelPlans() {
        return travelPlanDao.selectAllTravelPlans();
    }

    // 여행 계획 수정
    public void updateTravelPlan(TravelPlan travelPlan) {
        travelPlanDao.updateTravelPlan(travelPlan);
    }

    // 여행 계획 수정 (ID로 조회 후 업데이트)
    public boolean updateTravelPlanById(Long planId, TravelPlan travelPlan) {
        // 존재 여부 확인
        TravelPlan existingPlan = travelPlanDao.selectTravelPlanById(planId);
        if (existingPlan == null) {
            return false;
        }

        // ID 설정 후 업데이트
        travelPlan.setId(planId);
        travelPlanDao.updateTravelPlan(travelPlan);
        return true;
    }

    // 여행 계획 삭제 (단순 삭제 - 사용 안함, WithValidation 사용 권장)
    // public void deleteTravelPlanById(Long id) {
    //     travelPlanDao.deleteTravelPlan(id);
    // }

    // 여행 계획 삭제 (존재 여부 확인 포함)
    public boolean deleteTravelPlanByIdWithValidation(Long planId) {
        // 존재 여부 확인
        TravelPlan plan = travelPlanDao.selectTravelPlanById(planId);
        if (plan == null) {
            return false;
        }

        // 관련 데이터 포함 완전 삭제
        deleteAllDataByPlanId(planId);
        return true;
    }

    // 여행 플랜과 관련된 모든 데이터 삭제 (외래키 순서 고려)
    public void deleteAllDataByPlanId(Long planId) {
        // 1. 해당 플랜의 모든 날짜 가져오기
        List<TravelDay> days = travelDayDao.selectTravelDaysByPlanId(planId);

        // 2. 각 날짜의 모든 장소 삭제
        for (TravelDay day : days) {
            List<TravelPlace> places = travelPlaceDao.selectTravelPlacesByDayId(day.getId());
            for (TravelPlace place : places) {
                travelPlaceDao.deleteTravelPlace(place.getId());
            }
        }

        // 3. 모든 날짜 삭제
        for (TravelDay day : days) {
            travelDayDao.deleteTravelDay(day.getId());
        }

        // 4. 마지막으로 플랜 삭제
        travelPlanDao.deleteTravelPlan(planId);
    }

    // ==================== TravelDay CRUD ====================

    // 여행 일자 생성
    public void createTravelDay(TravelDay day) {
        travelDayDao.insertTravelDay(day);
    }

    // ID로 여행 일자 조회
    public TravelDay getTravelDayById(Long id) {
        return travelDayDao.selectTravelDayById(id);
    }

    // Plan ID로 모든 여행 일자 조회
    public List<TravelDay> getTravelDaysByPlanId(Long planId) {
        return travelDayDao.selectTravelDaysByPlanId(planId);
    }

    // 여행 일자 수정
    public void updateTravelDay(TravelDay day) {
        travelDayDao.updateTravelDay(day);
    }

    // 여행 일자 수정 (존재 여부 확인 포함)
    public boolean updateTravelDayById(Long dayId, TravelDay day) {
        TravelDay existingDay = travelDayDao.selectTravelDayById(dayId);
        if (existingDay == null) {
            return false;
        }
        day.setId(dayId);
        travelDayDao.updateTravelDay(day);
        return true;
    }

    // 여행 일자 삭제
    public void deleteTravelDayById(Long id) {
        travelDayDao.deleteTravelDay(id);
    }

    // 여행 일자 삭제 (관련 Place도 함께 삭제)
    public boolean deleteTravelDayByIdWithValidation(Long dayId) {
        TravelDay day = travelDayDao.selectTravelDayById(dayId);
        if (day == null) {
            return false;
        }

        // 해당 일자의 모든 장소 삭제
        List<TravelPlace> places = travelPlaceDao.selectTravelPlacesByDayId(dayId);
        for (TravelPlace place : places) {
            travelPlaceDao.deleteTravelPlace(place.getId());
        }

        // 일자 삭제
        travelDayDao.deleteTravelDay(dayId);
        return true;
    }

    // ==================== TravelPlace CRUD ====================

    // 여행 장소 생성
    public void createTravelPlace(TravelPlace place) {
        travelPlaceDao.insertTravelPlace(place);
    }

    // ID로 여행 장소 조회
    public TravelPlace getTravelPlaceById(Long id) {
        return travelPlaceDao.selectTravelPlaceById(id);
    }

    // Day ID로 모든 여행 장소 조회
    public List<TravelPlace> getTravelPlacesByDayId(Long dayId) {
        return travelPlaceDao.selectTravelPlacesByDayId(dayId);
    }

    // 여행 장소 수정
    public void updateTravelPlace(TravelPlace place) {
        travelPlaceDao.updateTravelPlace(place);
    }

    // 여행 장소 수정 (존재 여부 확인 포함)
    public boolean updateTravelPlaceById(Long placeId, TravelPlace place) {
        TravelPlace existingPlace = travelPlaceDao.selectTravelPlaceById(placeId);
        if (existingPlace == null) {
            return false;
        }
        place.setId(placeId);
        travelPlaceDao.updateTravelPlace(place);
        return true;
    }

    // 여행 장소 삭제
    public void deleteTravelPlaceById(Long id) {
        travelPlaceDao.deleteTravelPlace(id);
    }

    // 여행 장소 삭제 (존재 여부 확인 포함)
    public boolean deleteTravelPlaceByIdWithValidation(Long placeId) {
        TravelPlace place = travelPlaceDao.selectTravelPlaceById(placeId);
        if (place == null) {
            return false;
        }
        travelPlaceDao.deleteTravelPlace(placeId);
        return true;
    }

    // ==================== 복합 조회 ====================

    // 여행 계획 상세 조회 (일자별 장소 포함)
    public TravelPlanDetail getTravelPlanDetail(Long planId) {
        TravelPlan plan = travelPlanDao.selectTravelPlanById(planId);
        if (plan == null) {
            return null;
        }

        List<TravelDay> days = travelDayDao.selectTravelDaysByPlanId(planId);

        List<TravelDayWithPlaces> daysWithPlaces = days.stream()
            .map(day -> {
                List<TravelPlace> places = travelPlaceDao.selectTravelPlacesByDayId(day.getId());
                return new TravelDayWithPlaces(day, places);
            })
            .collect(Collectors.toList());

        TravelPlanDetail detail = new TravelPlanDetail();
        detail.setPlan(plan);
        detail.setDays(daysWithPlaces);

        return detail;
    }

    // === LLM Agent 친화적 메서드들 ===

    /**
     * 자연어 인덱스로 여행 일자 수정 (1, 2, 3...)
     * LLM이 "3일차 수정해줘"라고 하면 해당 일차를 찾아서 수정
     */
    public boolean updateDayByIndex(Long planId, Integer dayIndex, String title, LocalDate planDate) {
        List<TravelDay> days = travelDayDao.selectTravelDaysByPlanId(planId);
        TravelDay targetDay = days.stream()
            .filter(day -> day.getDayIndex().equals(dayIndex))
            .findFirst()
            .orElse(null);

        if (targetDay == null) {
            return false;
        }

        if (title != null) targetDay.setTitle(title);
        if (planDate != null) targetDay.setPlanDate(planDate);

        travelDayDao.updateTravelDay(targetDay);
        return true;
    }

    /**
     * 자연어 인덱스로 여행 일자 삭제
     * LLM이 "2일차 삭제해줘"라고 하면 해당 일차를 삭제
     */
    public boolean deleteDayByIndex(Long planId, Integer dayIndex) {
        List<TravelDay> days = travelDayDao.selectTravelDaysByPlanId(planId);
        TravelDay targetDay = days.stream()
            .filter(day -> day.getDayIndex().equals(dayIndex))
            .findFirst()
            .orElse(null);

        if (targetDay == null) {
            return false;
        }

        travelDayDao.deleteTravelDay(targetDay.getId());
        return true;
    }

    /**
     * 일차와 장소 순서로 특정 장소 수정
     * LLM이 "3일차 2번째 장소 수정해줘"라고 하면 해당 장소를 수정
     */
    public boolean updatePlaceByIndex(Long planId, Integer dayIndex, Integer placeIndex, TravelPlace newData) {
        // 먼저 해당 일차 찾기
        List<TravelDay> days = travelDayDao.selectTravelDaysByPlanId(planId);
        TravelDay targetDay = days.stream()
            .filter(day -> day.getDayIndex().equals(dayIndex))
            .findFirst()
            .orElse(null);

        if (targetDay == null) {
            return false;
        }

        // 해당 일차의 모든 장소 가져오기 (시작 시간 순)
        List<TravelPlace> places = travelPlaceDao.selectTravelPlacesByDayId(targetDay.getId());
        if (placeIndex < 1 || placeIndex > places.size()) {
            return false;
        }

        TravelPlace targetPlace = places.get(placeIndex - 1); // 0-based 인덱스로 변환

        // null이 아닌 필드만 업데이트
        if (newData.getTitle() != null) targetPlace.setTitle(newData.getTitle());
        if (newData.getStartAt() != null) targetPlace.setStartAt(newData.getStartAt());
        if (newData.getEndAt() != null) targetPlace.setEndAt(newData.getEndAt());
        if (newData.getPlaceName() != null) targetPlace.setPlaceName(newData.getPlaceName());
        if (newData.getAddress() != null) targetPlace.setAddress(newData.getAddress());
        if (newData.getLat() != 0) targetPlace.setLat(newData.getLat());
        if (newData.getLng() != 0) targetPlace.setLng(newData.getLng());
        if (newData.getExpectedCost() != null) targetPlace.setExpectedCost(newData.getExpectedCost());

        travelPlaceDao.updateTravelPlace(targetPlace);
        return true;
    }

    /**
     * 일차와 장소 순서로 특정 장소 삭제
     * LLM이 "2일차 1번째 장소 삭제해줘"라고 하면 해당 장소를 삭제
     */
    public boolean deletePlaceByIndex(Long planId, Integer dayIndex, Integer placeIndex) {
        // 먼저 해당 일차 찾기
        List<TravelDay> days = travelDayDao.selectTravelDaysByPlanId(planId);
        TravelDay targetDay = days.stream()
            .filter(day -> day.getDayIndex().equals(dayIndex))
            .findFirst()
            .orElse(null);

        if (targetDay == null) {
            return false;
        }

        // 해당 일차의 모든 장소 가져오기
        List<TravelPlace> places = travelPlaceDao.selectTravelPlacesByDayId(targetDay.getId());
        if (placeIndex < 1 || placeIndex > places.size()) {
            return false;
        }

        TravelPlace targetPlace = places.get(placeIndex - 1);
        travelPlaceDao.deleteTravelPlace(targetPlace.getId());
        return true;
    }

}
