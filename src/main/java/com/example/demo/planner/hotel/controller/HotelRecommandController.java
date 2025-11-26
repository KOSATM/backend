package com.example.demo.planner.hotel.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.planner.hotel.agent.HotelBookingAgent;
import com.example.demo.planner.hotel.dto.request.HotelBookingRequest;
import com.example.demo.planner.hotel.dto.request.TripPlanRequest;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/hotel")
public class HotelRecommandController {

    private final HotelBookingAgent hotelBookingAgent;

    @PostMapping("/booking/auto")
    public HotelBookingRequest autoBooking(@RequestBody TripPlanRequest tripPlan) {
        // 일단 하드코딩 예시
        int adults = 2;
        int children = 0;
        String guestName = "Test Guest";
        String guestEmail = "guest@example.com";
        String guestPhone = "+82-10-0000-0000";

        return hotelBookingAgent.createBookingFromItinerary(
                tripPlan,
                adults,
                children,
                guestName,
                guestEmail,
                guestPhone
        );
    }
}
