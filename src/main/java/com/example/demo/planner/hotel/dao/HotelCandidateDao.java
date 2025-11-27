package com.example.demo.planner.hotel.dao;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.demo.planner.hotel.dto.entity.HotelRatePlanCandidate;

@Mapper
public interface HotelCandidateDao {
    List<HotelRatePlanCandidate> findCandidates(
            @Param("checkinDate") LocalDate checkinDate,
            @Param("checkoutDate") LocalDate checkoutDate,
            @Param("adults") int adults,
            @Param("children") int children
    );
}
