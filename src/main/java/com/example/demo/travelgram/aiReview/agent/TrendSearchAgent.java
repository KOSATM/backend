package com.example.demo.travelgram.aiReview.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.demo.common.tools.InternetSearchTool;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class TrendSearchAgent {
  private ChatClient chatClient;
  private String searchEndpoint;
  private String apiKey;
  private String engineId;
  private WebClient webClient;
  private ObjectMapper objectMapper = new ObjectMapper();
  private InternetSearchTool internetSearchTool;

  public TrendSearchAgent(
      ChatClient.Builder chatClientBuilder,
      @Value("${google.search.endpoint}") String endpoint,
      @Value("${google.search.apiKey}") String apiKey,
      @Value("${google.search.engineId}") String engineId,
      WebClient.Builder webClientBuilder,
    InternetSearchTool internetSearchTool) {
    chatClient = chatClientBuilder.build();
    this.searchEndpoint = endpoint;
    this.apiKey = apiKey;
    this.engineId = engineId;
    this.internetSearchTool = internetSearchTool;
    this.webClient = webClientBuilder
        .baseUrl(searchEndpoint)
        .defaultHeader("Accept", "application/json")
        .build();
  }

  public String generateTrend(String question) {
    String response = chatClient.prompt()
        .system(
    """
    You are the TrendSearch Agent for a SEOUL-ONLY travel service.

    ## Mission
    - Analyze ONLY SEOUL-RELATED Instagram trends.
    - The travel service is limited to Seoul. 
    - NEVER introduce or mention other regions such as Tokyo, Osaka, Kyoto, LA, Paris, New York, or any other city.
    - All captions, hashtags, insights, and trend patterns must be strictly based on SEOUL.

    ## How to search
    When calling Google Search Tool, ALWAYS use only the following patterns:

    - "site:instagram.com 서울 {keyword} 2025"
    - "서울 {keyword} 인스타"
    - "서울여행 {keyword} 핫플"
    - "서울 {keyword} 감성"
    - "SEOUL {keyword} instagram"
    
    ## Output
    Produce a JSON-safe trend insight object:

    {
      "keywords": [...],
      "captionPatterns": [...],
      "popularHashtags": [...],
      "vibe": "...",
      "observations": "..."
    }

    ## Rules
    - ABSOLUTELY NO other cities (Tokyo, Osaka, Singapore, Paris, etc.).
    - Do NOT fabricate Instagram posts.
    - Base everything ONLY on Google Search Tool results.
    - Use concise and high-signal patterns suitable for a Seoul travel diary generation agent.
    - BEFORE generating the final JSON, strictly review the 'observations' and 'captionPatterns' to ensure NO mention of non-Seoul cities exists. If found, rephrase and correct.
    
    ## final rules
    Final check MUST be done to remove any foreign city reference.
    """)
        .tools(internetSearchTool)
        .call()
        .content();
    return response;
  }

  
}
