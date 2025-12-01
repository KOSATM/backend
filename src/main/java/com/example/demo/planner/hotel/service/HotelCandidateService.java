package com.example.demo.planner.hotel.service;

import java.time.OffsetDateTime;
import java.util.List;

import com.example.demo.planner.hotel.dto.entity.HotelRatePlanCandidate;

public interface HotelCandidateService {

    /**
     * 주어진 숙박 기간과 인원에 맞는 호텔/요금제 후보 조회
     */
    List<HotelRatePlanCandidate> findCandidates(
            OffsetDateTime checkinDate,
            OffsetDateTime checkoutDate,
            int adults,
            int children
    );
}
