package com.example.demo.supporter.checklist.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.demo.supporter.checklist.dto.response.TravelDayResponse;

@Mapper
public interface ChecklistTravelDayDao {
    TravelDayResponse getTravelDay(
        @Param("planId") Long planId,
        @Param("dayIndex") Integer dayIndex
    );
}
