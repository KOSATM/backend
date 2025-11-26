package com.example.demo.planner.hotel.service;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.planner.hotel.dto.entity.HotelRatePlanCandidate;

@Service
public class HotelCandidateServiceImpl implements HotelCandidateService {

    @Override
    public List<HotelRatePlanCandidate> findCandidates(
            OffsetDateTime checkinDate,
            OffsetDateTime checkoutDate,
            int adults,
            int children
    ) {
        // TODO: 나중에 여기서 진짜 DB에서 호텔 운영 정보 가져와서 채우면 됨
        return Collections.emptyList();
    }
}
