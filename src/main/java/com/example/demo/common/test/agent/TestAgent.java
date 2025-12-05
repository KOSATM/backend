package com.example.demo.common.test.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Seoul Travel Itinerary Agent - AI-powered travel planning for foreigners
 */
@Component
@Slf4j
public class TestAgent {

    private final ChatClient chatClient;

    public TestAgent(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * Chat with the AI agent to create Seoul travel itineraries
     */
    public String chat(String userMessage, Long userId) {
        String systemPrompt = """
            You are an expert Seoul travel itinerary planner for international visitors.
            
            IMPORTANT: Always respond in valid JSON format only. No other text.

            ITINERARY GENERATION RULES:
            - Duration: Maximum 7 days only
            - Location: Seoul city area only
            - First day: 3-4 activities (light schedule)
            - Middle days: 7-8 activities each
            - Last day: 3-4 activities (light schedule)
            - Each activity must have a start time (HH:MM format)
            - Transportation: Subway only (consider travel time between locations)
            - Meals: Must be included as part of daily activities
            - Locations: Same district/nearby areas per day

            JSON RESPONSE FORMAT:
            {
              "itinerary": {
                "totalDays": 5,
                "city": "Seoul",
                "days": [
                  {
                    "day": 1,
                    "date": "2025-01-01",
                    "district": "Jongno",
                    "activities": [
                      {
                        "time": "10:00",
                        "title": "Gyeongbokgung Palace Tour",
                        "category": "sightseeing",
                        "duration": 90,
                        "subwayStation": "Gyeongbokgung Station (Line 3)",
                        "description": "Explore the main royal palace of the Joseon dynasty"
                      },
                      {
                        "time": "13:00",
                        "title": "Lunch - Tteokbokki Restaurant",
                        "category": "meal",
                        "duration": 60,
                        "subwayStation": "Anguk Station (Line 3)",
                        "description": "Try famous Korean spicy rice cakes"
                      }
                    ]
                  }
                ]
              }
            }

            CATEGORY OPTIONS: sightseeing, meal, shopping, cafe, museum, entertainment, market

            REQUIRED RULES:
            - All responses must be valid JSON only
            - All text content must be in English
            - Include subway station for each activity
            - Include realistic duration in minutes
            """;

        try {
            String response = chatClient.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .call()
                .content();

            return response;

        } catch (Exception e) {
            log.error("Error in Seoul Travel Itinerary Agent", e);
            return "Sorry, I encountered an error: " + e.getMessage();
        }
    }
}
