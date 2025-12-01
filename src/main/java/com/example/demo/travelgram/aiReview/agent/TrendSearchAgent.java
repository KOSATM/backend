package com.example.demo.travelgram.aiReview.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
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

  public TrendSearchAgent(
      ChatClient.Builder chatClientBuilder,
      @Value("${google.search.endpoint}") String endpoint,
      @Value("${google.search.apiKey}") String apiKey,
      @Value("${google.search.engineId}") String engineId,
      WebClient.Builder webClientBuilder) {
    chatClient = chatClientBuilder.build();
    this.searchEndpoint = endpoint;
    this.apiKey = apiKey;
    this.engineId = engineId;
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

    - "site:instagram.com 서울 {keyword}"
    - "서울 {keyword} 인스타"
    - "서울여행 {keyword}"
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
    """)
        .tools(new InternetSearch())
        .call()
        .content();
    return response;
  }

  // ##### 도구 #####
  class InternetSearch {
    // @Tool(description = "테스트용 도구")
    // public String foo(@ToolParam String input) {
    // return "도구 실행됨" + input;
    // }

    @Tool(description = "자료를 찾기 위해 인터넷 검색을 합니다.")
    public String googleSearch(@ToolParam String query) {
      log.info("인터넷 검색 도구 호출됨");
      try {
        String responseBody = webClient.get()
            .uri(uriBuilder -> uriBuilder
                .queryParam("key", apiKey)
                .queryParam("cx", engineId)
                .queryParam("q", query)
                .build())
            .retrieve()
            .bodyToMono(String.class)
            .block();
        // log.info("응답본문: {}", responseBody);

        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode items = root.path("items");

        if (!items.isArray() || items.isEmpty()) {
          return "검색 결과가 없습니다.";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(3, items.size()); i++) {
          JsonNode item = items.get(i);
          String title = item.path("title").asText();
          String link = item.path("link").asText();
          String snippet = item.path("snippet").asText();
          sb.append(String.format("[%d] %s\n%s\n%s\n\n", i + 1, title, link, snippet));
        }
        return sb.toString().trim();

      } catch (Exception e) {
        return "인터넷 검색 중 오류 발생: " + e.getMessage();
      }
    }
  }

}
