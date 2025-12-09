package com.example.demo.common.tools;

import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.demo.common.naver.dto.LocalItem;
import com.example.demo.common.naver.dto.NaverImageSearchResponse;
import com.example.demo.common.naver.dto.NaverLocalSearchResponse;
import com.example.demo.planner.blog.BlogItem;
import com.example.demo.planner.blog.BlogSearchResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class NaverInternetSearchTool {
    private final WebClient webClient;
    private final String naverClientId;
    private final String naverClientSecret;

    public NaverInternetSearchTool(
            @Value("${naver.api.client-id}") String naverClientId,
            @Value("${naver.api.client-secret}") String naverClientSecret,
            WebClient.Builder webClientBuilder) {
        this.naverClientId = naverClientId;
        this.naverClientSecret = naverClientSecret;
        this.webClient = webClientBuilder
                .baseUrl("https://openapi.naver.com")
                .defaultHeader("Accept", "application/json")
                .build();
    }

    @Tool(description = "이미지 자료를 찾기 위해 인터넷 검색을 합니다.")
    public String getImgUrl(String query) {
        log.info("네이버 이미지 URL 검색 시작 : {}", query);
        try {
            NaverImageSearchResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/v1/search/image") // 이미지 검색 API 경로
                    .queryParam("query", query)
                    .queryParam("display", 1)
                    .queryParam("sort", "sim")
                    .build())
                .header("X-Naver-Client-Id", naverClientId)
                .header("X-Naver-Client-Secret", naverClientSecret)
                .retrieve()
                .bodyToMono(NaverImageSearchResponse.class)
                .block();

            if (response == null || response.getItems() == null || response.getItems().isEmpty()) {
                log.warn("네이버 이미지 검색 결과 없음: {}", query);
                return null;
            }
            String imageUrl = response.getItems().get(0).getLink();
            log.info("검색된 이미지 URL : {}", imageUrl);
            return imageUrl;
        } catch (Exception e) {
            log.error("네이버 이미지 URL 검색 중 오류 발생: {}", e.getMessage());
            return null; // catch 블록에서도 null을 반환하도록 수정
        }
    }

    // 메서드의 반환 타입을 String에서 List<LocalItem>으로 수정합니다.
    @Tool(description = "자료를 찾기 위해 인터넷 검색을 합니다.")
    public List<LocalItem> getLocalInfo(String query) {
        log.info("네이버 지역 정보 검색 시작 : {}", query);
        try {
            NaverLocalSearchResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/v1/search/local") // 지역 검색 API 경로
                    .queryParam("query", query)
                    .queryParam("display", 5) // 최대 5개까지 가져오기
                    .build())
                .header("X-Naver-Client-Id", naverClientId)
                .header("X-Naver-Client-Secret", naverClientSecret)
                .retrieve()
                .bodyToMono(NaverLocalSearchResponse.class)
                .block();

            if (response == null || response.getItems() == null || response.getItems().isEmpty()) {
                log.warn("네이버 지역 정보 검색 결과 없음: {}", query);
                return null;
            }
            // 이제 이 반환문은 메서드 시그니처와 일치합니다.
            return response.getItems();
        } catch (Exception e) {
            log.error("네이버 지역 정보 검색 중 오류 발생: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 블로그 검색(Open API)
     */
    @Tool(description = "여행지 후기나 블로그 정보를 검색하여 제공합니다.")
    public List<BlogItem> getBlogInfo(String query) {
        log.info("네이버 블로그 검색 시작: {}", query);
        try {
            // 1. 블로그 검색 (Search WebClient 사용)
            BlogSearchResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/v1/search/blog.json")
                    .queryParam("query", query)
                    .queryParam("display", 5)
                    .queryParam("sort", "sim")
                    .build())
                .header("X-Naver-Client-Id", naverClientId)
                .header("X-Naver-Client-Secret", naverClientSecret)
                .retrieve()
                .bodyToMono(BlogSearchResponse.class)
                .block();

            if (response == null || response.getItems() == null) {
                log.warn("네이버 블로그 검색 결과 body가 비어있습니다.");
                return null;
            }

            List<BlogItem> items = response.getItems();
            log.info("블로그 검색 성공! 개수: {}", items.size()); // 성공 로그 확인용

            for (BlogItem item : items) {
                String cleanTitle = removeTags(item.getTitle());
                String cleanDesc = removeTags(item.getDescription());
                item.setTitle(cleanTitle);
                item.setDescription(cleanDesc);
            }
            return items;

        } catch (Exception e) {
            log.error("네이버 검색 API 호출 중 에러 발생: ", e); // 에러 로그를 확실히 봅니다.
            return null;
        }
    }

    private String removeTags(String text) {
        if (text == null) return "";
        return text.replaceAll("<[^>]*>", "").replaceAll("&quot;", "\"").replaceAll("&amp;", "&");
    }
}
