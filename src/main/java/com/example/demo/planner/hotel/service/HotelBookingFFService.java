package com.example.demo.planner.hotel.service;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.planner.hotel.dao.HotelBookingFFDao;
import com.example.demo.planner.hotel.dto.entity.HotelBookingFF.HotelBookingFFResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HotelBookingFFService {

    private final HotelBookingFFDao hotelBookingFFDao;

    /**
     * 특정 유저의 호텔 예약 1건 조회 (userId 당 1건이라는 가정)
     */
    @Transactional(readOnly = true)
    public HotelBookingFFResponse getBookingByUserId(Long userId) {
        return hotelBookingFFDao.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "해당 유저의 호텔 예약이 없습니다. userId=" + userId));
    }

    /**
     * 유저의 호텔 예약 생성 또는 수정 (Upsert)
     * - userId 당 항상 1건만 유지
     */
    @Transactional
    public HotelBookingFFResponse saveOrUpdateBooking(HotelBookingFFResponse booking) {
        if (booking.getUserId() == null) {
            throw new IllegalArgumentException("userId는 필수 값입니다.");
        }

        // 간단한 날짜 검증
        if (booking.getCheckinDate() == null || booking.getCheckoutDate() == null) {
            throw new IllegalArgumentException("체크인/체크아웃 날짜는 필수입니다.");
        }
        if (booking.getCheckoutDate().isBefore(booking.getCheckinDate())) {
            throw new IllegalArgumentException("체크아웃 날짜는 체크인 이후여야 합니다.");
        }

        // 이미 예약이 있는지 확인
        var existingOpt = hotelBookingFFDao.findByUserId(booking.getUserId());

        if (existingOpt.isEmpty()) {
            // 새 예약 생성
            if (booking.getCreatedAt() == null) {
                booking.setCreatedAt(OffsetDateTime.now());
            }
            hotelBookingFFDao.insert(booking);
            return booking;
        } else {
            // 기존 예약 덮어쓰기 (id 유지)
            HotelBookingFFResponse existing = existingOpt.get();
            booking.setId(existing.getId());

            // createdAt 은 기존 값 유지 (없으면 기존 값 사용)
            if (booking.getCreatedAt() == null) {
                booking.setCreatedAt(existing.getCreatedAt());
            }

            int updated = hotelBookingFFDao.update(booking);
            if (updated == 0) {
                throw new IllegalStateException(
                        "예약 수정에 실패했습니다. userId=" + booking.getUserId());
            }

            return booking;
        }
    }

    /**
     * 예약 삭제 (PK 기준)
     */
    @Transactional
    public void deleteBookingById(Long id) {
        int deleted = hotelBookingFFDao.deleteById(id);
        if (deleted == 0) {
            throw new IllegalArgumentException("삭제할 예약이 존재하지 않습니다. id=" + id);
        }
    }
}
