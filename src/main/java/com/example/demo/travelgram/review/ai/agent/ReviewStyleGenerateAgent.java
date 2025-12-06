package com.example.demo.travelgram.review.ai.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import com.example.demo.travelgram.review.ai.dto.response.GeneratedStyleResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class ReviewStyleGenerateAgent {

    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper;

    public GeneratedStyleResponse generateStyles(String tripJson, String mood, String travelType) {
        ChatClient chatClient = chatClientBuilder.build();

        String systemPrompt = """
            You are a professional Instagram Travel Content Creator.
            Your task is to generate 4 distinct styles of captions and hashtags based on the provided trip data and overall mood.

            ## Input Data
            1. Trip Data (JSON): Contains places, memo, costs, dates.
            2. Overall Mood: The atmosphere of the trip (e.g., Relaxing, Exciting).
            3. Travel Type: SOLO, GROUP, UNCLEAR

            ## Output Requirements (Strict JSON)
            You must return a JSON object containing a list of 4 styles.
            
            Key Styles:
            1. **EMOTIONAL**: Focus on feelings, scenery, and "vibes". Soft tone.
            2. **INFORMATIVE**: Focus on tips, costs, location names, and efficient routes. Helpful tone.
            3. **WITTY**: Fun, short, usage of slang or emojis, humorous complaints or joy.
            4. **SIMPLE**: Very short, chic, hashtags-focused. Minimalist.

            ## Rules
            - Language: ONLY ENGLISH.
            - Hashtags: Generate exactly **10 relevant hashtags** for each style.
            - Content:
                - Use the 'memo' and 'place_name' from input data effectively.
                - The 'caption' field must contain ONLY the main text.
                - DO NOT include hashtags in the 'caption' field. (They must go into the 'hashtags' array).
            - Do NOT fabricate places not in the data.

            ## JSON Structure
            {
              "styles": [
                {
                  "toneCode": "EMOTIONAL",
                  "toneName": "감성 가득",
                  "caption": "...",
                  "hashtags": ["#...", "#..."]
                },
                ... (repeat for all 4 styles)
              ]
            }
            """;

        String userPrompt = String.format("""
            ## Trip Data:
            %s

            ## Context:
            - Mood: %s
            - Type: %s
            """, tripJson, mood, travelType);

        try {
            String response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();

            // 마크다운 제거 (```json ...)
            String cleanJson = response.replaceAll("```json", "").replaceAll("```", "").trim();
            
            return objectMapper.readValue(cleanJson, GeneratedStyleResponse.class);

        } catch (Exception e) {
            log.error("Style Generation Failed", e);
            throw new RuntimeException("AI Style Generation Error");
        }
    }
}