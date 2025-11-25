package com.example.demo.planner.hotel.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.planner.hotel.dao.HotelBookingDao;
import com.example.demo.planner.hotel.dto.entity.HotelBooking;

@Service
public class HotelBookingService {
    
    @Autowired
    private HotelBookingDao hotelBookingDao;
    
    public Long saveHotelBooking(HotelBooking hotelBooking) {
        hotelBookingDao.insertHotelBooking(hotelBooking);
        return hotelBooking.getId();
    }
    
    public HotelBooking getHotelBooking(Long id) {
        return hotelBookingDao.selectHotelBookingById(id);
    }
    
    public void updateHotelBooking(HotelBooking hotelBooking) {
        hotelBookingDao.updateHotelBooking(hotelBooking);
    }
    
    public void deleteHotelBooking(Long id) {
        hotelBookingDao.deleteHotelBookingById(id);
    }
}
