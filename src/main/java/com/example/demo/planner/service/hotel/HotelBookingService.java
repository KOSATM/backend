package com.example.demo.planner.service.hotel;

import com.example.demo.planner.dao.hotel.HotelBookingDao;
import com.example.demo.planner.dto.hotel.HotelBooking;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HotelBookingService {
    
    @Autowired
    private HotelBookingDao hotelBookingDao;
    
    // DB에 호텔 예약 저장
    public Long saveHotelBooking(HotelBooking hotelBooking) {
        hotelBookingDao.insertHotelBooking(hotelBooking);
        return hotelBooking.getId();
    }
    
    // ID로 호텔 예약 조회
    public HotelBooking getHotelBooking(Long id) {
        return hotelBookingDao.selectHotelBookingById(id);
    }
    
    // 호텔 예약 업데이트
    public void updateHotelBooking(HotelBooking hotelBooking) {
        hotelBookingDao.updateHotelBooking(hotelBooking);
    }
    
    // 호텔 예약 삭제
    public void deleteHotelBooking(Long id) {
        hotelBookingDao.deleteHotelBookingById(id);
    }
}
