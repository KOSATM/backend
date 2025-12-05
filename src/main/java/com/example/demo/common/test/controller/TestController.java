package com.example.demo.common.test.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.common.global.response.ResponseWrapper;
import com.example.demo.common.test.dto.ChatRequest;
import com.example.demo.common.test.dto.ChatResponse;
import com.example.demo.common.test.service.TestService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Test Controller - Seoul Travel Itinerary Agent API
 * Handles requests for creating and managing travel itineraries
 */
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Slf4j
public class TestController {

    private final TestService testService;

    /**
     * Chat endpoint for Seoul travel itinerary requests
     * 
     * @param request ChatRequest containing user message
     * @param userId User ID from header or parameter
     * @return ChatResponse with AI-generated itinerary
     */
    @PostMapping("/chat")
    public ResponseEntity<ResponseWrapper<ChatResponse>> chat(
            @RequestBody ChatRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestParam(value = "userId", required = false) Long paramUserId) {
        
        log.info("Chat request received - Message: {}", request.getMessage());
        
        // Determine userId from header or param
        Long userId = headerUserId != null ? headerUserId : paramUserId;
        if (userId == null) {
            userId = 1L; // Default user ID for testing
        }
        
        try {
            ChatResponse response = testService.processChat(request, userId);
            
            return ResponseEntity.ok(
                    ResponseWrapper.success("Chat response generated successfully", response)
            );
        } catch (Exception e) {
            log.error("Error processing chat request", e);
            return ResponseEntity.ok(
                    ResponseWrapper.error("Failed to process chat request: " + e.getMessage())
            );
        }
    }

    /**
     * Create Seoul travel itinerary by days - POST method
     * 
     * @param request ChatRequest with days parameter
     * @param userId User ID
     * @return ResponseEntity with created itinerary
     */
    @PostMapping("/create-itinerary")
    public ResponseEntity<ResponseWrapper<ChatResponse>> createItineraryPost(
            @RequestBody ChatRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        
        if (userId == null) {
            userId = 1L;
        }
        
        Integer days = request.getDays();
        if (days == null || days < 1 || days > 7) {
            return ResponseEntity.ok(
                    ResponseWrapper.error("Invalid days. Please provide 1-7 days for Seoul itinerary.")
            );
        }
        
        log.info("Create itinerary request (POST) - UserId: {}, Days: {}", userId, days);
        
        try {
            // Build a message with the days parameter
            String message = String.format("I want a %d-day Seoul itinerary", days);
            ChatResponse response = testService.createSeoulItinerary(message, userId);
            
            return ResponseEntity.ok(
                    ResponseWrapper.success("Seoul travel itinerary created successfully", response)
            );
        } catch (Exception e) {
            log.error("Error creating itinerary", e);
            return ResponseEntity.ok(
                    ResponseWrapper.error("Failed to create itinerary: " + e.getMessage())
            );
        }
    }

    /**
     * Create Seoul travel itinerary by days - GET method
     * 
     * @param days Number of travel days (query parameter)
     * @param userId User ID
     * @return ResponseEntity with created itinerary
     */
    @GetMapping("/create-itinerary")
    public ResponseEntity<ResponseWrapper<ChatResponse>> createItineraryGet(
            @RequestParam(value = "days") Integer days,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        
        if (userId == null) {
            userId = 1L;
        }
        
        if (days == null || days < 1 || days > 7) {
            return ResponseEntity.ok(
                    ResponseWrapper.error("Invalid days. Please provide 1-7 days for Seoul itinerary.")
            );
        }
        
        log.info("Create itinerary request (GET) - UserId: {}, Days: {}", userId, days);
        
        try {
            // Build a message with the days parameter
            String message = String.format("I want a %d-day Seoul itinerary", days);
            ChatResponse response = testService.createSeoulItinerary(message, userId);
            
            return ResponseEntity.ok(
                    ResponseWrapper.success("Seoul travel itinerary created successfully", response)
            );
        } catch (Exception e) {
            log.error("Error creating itinerary", e);
            return ResponseEntity.ok(
                    ResponseWrapper.error("Failed to create itinerary: " + e.getMessage())
            );
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<ResponseWrapper<String>> health() {
        log.info("Health check requested");
        return ResponseEntity.ok(
                ResponseWrapper.success("Seoul Travel Itinerary Agent is running", "OK")
        );
    }
}
