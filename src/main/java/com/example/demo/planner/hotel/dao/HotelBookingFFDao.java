package com.example.demo.planner.hotel.dao;

import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.demo.planner.hotel.dto.entity.HotelBookingFF.HotelBookingFFResponse;;

@Mapper
public interface HotelBookingFFDao {

    void insert(HotelBookingFFResponse booking);

    Optional<HotelBookingFFResponse> findByUserId(@Param("userId") Long userId);

    int update(HotelBookingFFResponse booking);

    int deleteById(@Param("id") Long id);
}