package com.example.demo.common.test.service;

import org.springframework.stereotype.Service;

import com.example.demo.common.test.agent.TestAgent;
import com.example.demo.common.test.dto.ChatRequest;
import com.example.demo.common.test.dto.ChatResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Test Service - Business logic for Seoul travel itinerary
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TestService {

    private final TestAgent testAgent;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Process chat message and generate response
     */
    public ChatResponse processChat(ChatRequest request, Long userId) {
        log.info("Processing chat request - UserId: {}, Message: {}", userId, request.getMessage());
        
        try {
            String agentResponse = testAgent.chat(request.getMessage(), userId);
            Object parsedResponse = parseJsonResponse(agentResponse);
            
            return ChatResponse.builder()
                    .data(parsedResponse)
                    .userId(userId)
                    .timestamp(System.currentTimeMillis())
                    .build();
        } catch (Exception e) {
            log.error("Error processing chat", e);
            return ChatResponse.builder()
                    .data("Error: " + e.getMessage())
                    .userId(userId)
                    .timestamp(System.currentTimeMillis())
                    .build();
        }
    }

    /**
     * Create Seoul travel itinerary
     */
    public ChatResponse createSeoulItinerary(String message, Long userId) {
        log.info("Creating Seoul itinerary - UserId: {}, Message: {}", userId, message);
        
        // If message doesn't contain days, add default
        String enhancedMessage = message;
        if (!message.toLowerCase().contains("day") && !message.toLowerCase().contains("days")) {
            enhancedMessage = message + " (5 days)";
        }
        
        try {
            String agentResponse = testAgent.chat(enhancedMessage, userId);
            Object parsedResponse = parseJsonResponse(agentResponse);
            
            return ChatResponse.builder()
                    .data(parsedResponse)
                    .userId(userId)
                    .timestamp(System.currentTimeMillis())
                    .build();
        } catch (Exception e) {
            log.error("Error creating Seoul itinerary", e);
            return ChatResponse.builder()
                    .data("Error: " + e.getMessage())
                    .userId(userId)
                    .timestamp(System.currentTimeMillis())
                    .build();
        }
    }

    /**
     * Parse JSON string to Object
     */
    private Object parseJsonResponse(String response) {
        try {
            // Remove markdown code block if present
            String cleanedResponse = response.trim();
            if (cleanedResponse.startsWith("```json")) {
                cleanedResponse = cleanedResponse.substring(7);
            } else if (cleanedResponse.startsWith("```")) {
                cleanedResponse = cleanedResponse.substring(3);
            }
            if (cleanedResponse.endsWith("```")) {
                cleanedResponse = cleanedResponse.substring(0, cleanedResponse.length() - 3);
            }
            cleanedResponse = cleanedResponse.trim();
            
            JsonNode jsonNode = objectMapper.readTree(cleanedResponse);
            return objectMapper.convertValue(jsonNode, Object.class);
        } catch (Exception e) {
            log.warn("Failed to parse JSON response, returning as string: {}", e.getMessage());
            return response;
        }
    }
}
