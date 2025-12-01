package com.example.demo.planner.hotel.dao;

import org.apache.ibatis.annotations.Mapper;
import com.example.demo.planner.hotel.dto.entity.HotelBooking;
import com.example.demo.planner.hotel.dto.request.HotelBookingRequest;

@Mapper
public interface HotelBookingDao {
    HotelBooking selectHotelBookingById(Long id);
    void insertHotelBooking(HotelBookingRequest request);
    void updateHotelBooking(HotelBookingRequest request);
    void deleteHotelBookingById(Long id);
}
