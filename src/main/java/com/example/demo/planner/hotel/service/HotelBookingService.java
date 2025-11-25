package com.example.demo.planner.hotel.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.demo.planner.hotel.dao.HotelBookingDao;
import com.example.demo.planner.hotel.dto.entity.HotelBooking;
import com.example.demo.planner.hotel.dto.request.HotelBookingRequest;
import com.example.demo.planner.hotel.dto.response.HotelBookingResponse;

@Service
public class HotelBookingService {
    
    @Autowired
    private HotelBookingDao hotelBookingDao;
    
    // Create
    public Long saveHotelBooking(HotelBookingRequest request) {
        HotelBookingRequest hotelBooking = HotelBookingRequest.builder()
            .userId(request.getUserId())
            .externalBookingId(request.getExternalBookingId())
            .hotelId(request.getHotelId())
            .roomTypeId(request.getRoomTypeId())
            .ratePlanId(request.getRatePlanId())
            .checkinDate(request.getCheckinDate())
            .checkoutDate(request.getCheckoutDate())
            .nights(request.getNights())
            .adultsCount(request.getAdultsCount())
            .childrenCount(request.getChildrenCount())
            .currency(request.getCurrency())
            .totalPrice(request.getTotalPrice())
            .taxAmount(request.getTaxAmount())
            .feeAmount(request.getFeeAmount())
            .status(request.getStatus())
            .paymentStatus(request.getPaymentStatus())
            .guestName(request.getGuestName())
            .guestEmail(request.getGuestEmail())
            .guestPhone(request.getGuestPhone())
            .providerBookingMeta(request.getProviderBookingMeta())
            .bookedAt(request.getBookedAt())
            .cancelledAt(request.getCancelledAt())
            .build();
        hotelBookingDao.insertHotelBooking(hotelBooking);
        return null;
    }
    
    // Read
    public HotelBookingResponse getHotelBooking(Long id) {
        HotelBooking hotelBooking = hotelBookingDao.selectHotelBookingById(id);
        if (hotelBooking == null) {
            return null;
        }
        return new HotelBookingResponse(
            hotelBooking.getId(), hotelBooking.getUserId(), hotelBooking.getExternalBookingId(),
            hotelBooking.getHotelId(), hotelBooking.getRoomTypeId(), hotelBooking.getRatePlanId(),
            hotelBooking.getCheckinDate(), hotelBooking.getCheckoutDate(), hotelBooking.getNights(),
            hotelBooking.getAdultsCount(), hotelBooking.getChildrenCount(), hotelBooking.getCurrency(),
            hotelBooking.getTotalPrice(), hotelBooking.getTaxAmount(), hotelBooking.getFeeAmount(),
            hotelBooking.getStatus(), hotelBooking.getPaymentStatus(), hotelBooking.getGuestName(),
            hotelBooking.getGuestEmail(), hotelBooking.getGuestPhone(), hotelBooking.getProviderBookingMeta(),
            hotelBooking.getBookedAt(), hotelBooking.getCancelledAt(), hotelBooking.getCreatedAt(),
            hotelBooking.getUpdatedAt()
        );
    }
    
    // Update
    public void updateHotelBooking(Long id, HotelBookingRequest request) {
        HotelBookingRequest hotelBooking = HotelBookingRequest.builder()
            .userId(request.getUserId())
            .externalBookingId(request.getExternalBookingId())
            .hotelId(request.getHotelId())
            .roomTypeId(request.getRoomTypeId())
            .ratePlanId(request.getRatePlanId())
            .checkinDate(request.getCheckinDate())
            .checkoutDate(request.getCheckoutDate())
            .nights(request.getNights())
            .adultsCount(request.getAdultsCount())
            .childrenCount(request.getChildrenCount())
            .currency(request.getCurrency())
            .totalPrice(request.getTotalPrice())
            .taxAmount(request.getTaxAmount())
            .feeAmount(request.getFeeAmount())
            .status(request.getStatus())
            .paymentStatus(request.getPaymentStatus())
            .guestName(request.getGuestName())
            .guestEmail(request.getGuestEmail())
            .guestPhone(request.getGuestPhone())
            .providerBookingMeta(request.getProviderBookingMeta())
            .bookedAt(request.getBookedAt())
            .cancelledAt(request.getCancelledAt())
            .build();
        hotelBookingDao.updateHotelBooking(hotelBooking);
    }
    
    // Delete
    public void deleteHotelBooking(Long id) {
        hotelBookingDao.deleteHotelBookingById(id);
    }
}
