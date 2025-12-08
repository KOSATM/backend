package com.example.demo.common.tools;

import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.demo.common.naver.dto.LocalItem;
import com.example.demo.common.naver.dto.NaverImageSearchResponse;
import com.example.demo.common.naver.dto.NaverLocalSearchResponse;
import com.example.demo.planner.recommendation.BlogItem;
import com.example.demo.planner.recommendation.NaverBlogSearchResponse;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class NaverInternetSearchTool {
    private final WebClient webClient;
    private final String naverClientId;
    private final String naverClientSecret;
    private final String papagoClientId;
    private final String papagoClientSecret;

    public NaverInternetSearchTool(
            // 검색용 키 주입
            @Value("${naver.api.client-id}") String naverClientId,
            @Value("${naver.api.client-secret}") String naverClientSecret,
            // 파파고용 키 주입
            @Value("${naver.papago.client.id}") String papagoClientId,
            @Value("${naver.papago.client.secret}") String papagoClientSecret,
            WebClient.Builder webClientBuilder) {
        this.naverClientId = naverClientId;
        this.naverClientSecret = naverClientSecret;
        this.papagoClientId = papagoClientId;
        this.papagoClientSecret = papagoClientSecret;
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

    // ▼▼▼ [수정됨] 블로그 검색 + 영어 번역 기능 ▼▼▼
    @Tool(description = "여행지 후기나 블로그 정보를 검색하고 영어로 번역하여 제공합니다.")
    public List<BlogItem> getBlogInfo(String query) {
        log.info("네이버 블로그 검색 시작 : {}", query);
        try {
            // 1. 블로그 검색 API 호출
            NaverBlogSearchResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/v1/search/blog")
                    .queryParam("query", query)
                    .queryParam("display", 5) // 5개 가져옴
                    .queryParam("sort", "sim")
                    .build())
                .header("X-Naver-Client-Id", naverClientId)
                .header("X-Naver-Client-Secret", naverClientSecret)
                .retrieve()
                .bodyToMono(NaverBlogSearchResponse.class)
                .block();

            if (response == null || response.getItems() == null || response.getItems().isEmpty()) {
                return null;
            }

            // 2. 검색 결과 하나씩 꺼내서 영어로 번역 (Loop)
            List<BlogItem> items = response.getItems();
            for (BlogItem item : items) {
                // HTML 태그 제거 (<b> 등)
                String cleanTitle = removeTags(item.getTitle());
                String cleanDesc = removeTags(item.getDescription());

                // 번역 실행 (한국어 -> 영어)
                item.setTitle(translateText(cleanTitle));
                item.setDescription(translateText(cleanDesc));
            }

            return items;

        } catch (Exception e) {
            log.error("블로그 검색 및 번역 중 오류: {}", e.getMessage());
            return null;
        }
    }

    // ▼▼▼ [추가] 파파고 번역 메서드 ▼▼▼
    private String translateText(String text) {
        if (text == null || text.isEmpty()) return "";
        try {
            PapagoResponse response = webClient.post()
                .uri("/v1/papago/n2mt") // 파파고 NMT API 경로
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header("X-Naver-Client-Id", papagoClientId)
                .header("X-Naver-Client-Secret", papagoClientSecret)
                .body(BodyInserters.fromFormData("source", "ko")
                                   .with("target", "en")
                                   .with("text", text))
                .retrieve()
                .bodyToMono(PapagoResponse.class)
                .block();

            if (response != null && response.getMessage() != null && response.getMessage().getResult() != null) {
                return response.getMessage().getResult().getTranslatedText();
            }
        } catch (Exception e) {
            log.warn("번역 실패 (원본 반환): {}", text);
        }
        return text; // 번역 실패 시 원본 그대로 반환
    }

    // ▼▼▼ [추가] HTML 태그 제거 헬퍼 메서드 ▼▼▼
    private String removeTags(String text) {
        if (text == null) return "";
        // <b>, </b> 같은 태그를 제거하는 정규식
        return text.replaceAll("<[^>]*>", "").replaceAll("&quot;", "\"").replaceAll("&amp;", "&");
    }

    // ▼▼▼ [추가] 파파고 응답 DTO (내부 클래스) ▼▼▼
    @Data
    public static class PapagoResponse {
        private Message message;
        @Data
        public static class Message {
            private Result result;
        }
        @Data
        public static class Result {
            private String translatedText;
        }
    }

}