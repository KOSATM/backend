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
            2. **INFORMATIVE**: Focus on tips, location names, and efficient routes. Helpful tone. (Do not mention specific prices).
            3. **WITTY**: Fun, short, usage of slang or emojis, humorous complaints or joy.
            4. **SIMPLE**: Very short, chic, hashtags-focused. Minimalist.
            
            ## Rules
            1. **Language**: ONLY ENGLISH.
            2. **Hashtags**: Generate exactly 10 relevant English hashtags in the 'hashtags' list.
            3. **Content (Crucial)**: 
                - Use the 'memo' and 'place_name' from input data effectively.
                - Do NOT fabricate places not in the data.
                - **NO EXACT COSTS**: Do NOT mention specific prices or currency amounts (e.g., $50, 10000won) in the caption. General terms like "affordable" or "luxury" are allowed.
                - The 'caption' field must contain **ONLY the narrative text**.
                - **ABSOLUTELY DO NOT** include hashtags inside the 'caption' string.
            4. **Tone Naming**:
                - The 'toneName' must be in **English**.
                - It must be descriptive and longer than the 'toneCode' (e.g., "Deeply Sentimental Vibes" instead of just "Emotional").

            ## JSON Structure
            {
              "styles": [
                {
                  "toneCode": "EMOTIONAL",
                  "toneName": "Sentimental & Dreamy Atmosphere",
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