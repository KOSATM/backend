package com.example.demo.planner.dao;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.planner.dto.HotelBooking;

@Mapper
public interface HotelBookingDao {
    HotelBooking selectHotelBookingById(Long id);
    void insertHotelBooking(HotelBooking hotelBooking);
    void updateHotelBooking(HotelBooking hotelBooking);
    void deleteHotelBookingById(Long id);
}
