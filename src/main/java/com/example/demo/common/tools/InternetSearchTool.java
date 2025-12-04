package com.example.demo.common.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class InternetSearchTool {
    // @Tool(description = "테스트용 도구")
    // public String foo(@ToolParam String input) {
    // return "도구 실행됨" + input;
    // }
    private String searchEndpoint;
    private String apiKey;
    private String engineId;
    private WebClient webClient;
    private ObjectMapper objectMapper = new ObjectMapper();

    public InternetSearchTool(
            @Value("${google.search.endpoint}") String endpoint,
            @Value("${google.search.apiKey}") String apiKey,
            @Value("${google.search.engineId}") String engineId,
            WebClient.Builder webClientBuilder) {
        this.searchEndpoint = endpoint;
        this.apiKey = apiKey;
        this.engineId = engineId;
        this.webClient = webClientBuilder
                .baseUrl(searchEndpoint)
                .defaultHeader("Accept", "application/json")
                .build();
    }

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

    @Tool(description = "이미지 자료를 찾기 위해 인터넷 검색을 합니다.")
    public String getImgUrl(String name) {
        log.info("이미지 URL 검색 시작 : {}", name);
        try {
            String responseBody = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .queryParam("key", apiKey)
                    .queryParam("cx", engineId)
                    .queryParam("q", name)
                    //이미지 검색을 하도록 명시적인 파라미터 추가
                    .queryParam("searchType", "image")
                    //가장 정확한 결과 1개만 받도록 설정
                    .queryParam("num", "1")
                    .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode items = root.path("items");

            //검색 결과가 없거나 배열이 아닌 경우
            if (!items.isArray() || items.isEmpty()) {
                    log.warn("이미지 검색 결과 없음: {}", name);
                    return null; // 결과가 없으면 null 반환
            }

            String imageUrl = items.get(0).path("link").asText();
            log.info("검색된 이미지 URL : {}", imageUrl);
            
            return imageUrl;
        } catch (Exception e) {
            log.error("이미지 URL 검색 중 오류 발생: {}", e.getMessage());
            return null;
        }
    }
}
