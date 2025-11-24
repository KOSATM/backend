package com.example.demo.planner.dao.hotel;

import com.example.demo.planner.dto.hotel.HotelBooking;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HotelBookingDao {
    HotelBooking selectHotelBookingById(Long id);
    void insertHotelBooking(HotelBooking hotelBooking);
    void updateHotelBooking(HotelBooking hotelBooking);
    void deleteHotelBookingById(Long id);
}
