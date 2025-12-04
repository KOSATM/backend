package com.example.demo.planner.travel.dto.response;

import java.util.ArrayList;
import java.util.List;

import com.example.demo.planner.travel.dto.TravelPlaceCandidate;
import com.example.demo.planner.travel.utils.CategoryNames;

import lombok.Getter;

@Getter
public class DayPlanResult {

    private int dayNumber;
    private final List<TravelPlaceCandidate> places = new ArrayList<>();

    public DayPlanResult(int dayNumber) {
        this.dayNumber = dayNumber;
    }

    /** 장소 추가 */
    public void addPlace(TravelPlaceCandidate place) {
        if (!places.contains(place)) {
            places.add(place);
        }
    }

    /** 카테고리 개수 세기 */
    public long countByCategory(String category) {
        return places.stream()
                .filter(p -> p.getNormalizedCategory().equals(category))
                .count();
    }

    /** 선택 카테고리 개수 */
    public long countOptional() {
        return places.stream()
                .filter(p -> CategoryNames.OPTIONAL.contains(p.getNormalizedCategory()))
                .count();
    }

    /** 포함 여부 */
    public boolean contains(TravelPlaceCandidate place) {
        return places.contains(place);
    }
}