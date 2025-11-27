package com.example.demo.planner.hotel.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.planner.hotel.dao.HotelCandidateDao;
import com.example.demo.planner.hotel.dto.entity.HotelRatePlanCandidate;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class HotelCandidateServiceImpl implements HotelCandidateService {

    @Autowired
    private HotelCandidateDao hotelCandidateDao;

    @Override
    public List<HotelRatePlanCandidate> findCandidates(
            OffsetDateTime checkinDate,
            OffsetDateTime checkoutDate,
            int adults,
            int children
    ) {
        LocalDate checkin = checkinDate.toLocalDate();
        LocalDate checkout = checkoutDate.toLocalDate();
        
        log.info("📊 Querying DB for candidates: {} ~ {}, adults={}, children={}", 
            checkin, checkout, adults, children);
        
        List<HotelRatePlanCandidate> result = hotelCandidateDao.findCandidates(checkin, checkout, adults, children);
        
        log.info("✅ Found {} hotel candidates", result.size());
        if (!result.isEmpty()) {
            result.forEach(h -> log.info("   - {}: {} {}", h.getHotelName(), h.getTotalPrice(), h.getCurrency()));
        }
        
        return result;
    }
}
