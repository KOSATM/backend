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
import com.example.demo.common.tools.NaverInternetSearchTool.PapagoResponse;
import com.example.demo.planner.recommendation.BlogItem;
import com.example.demo.planner.recommendation.NaverBlogSearchResponse;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class NaverInternetSearchTool {
   // 1. 검색용 WebClient (Open API)
    private final WebClient searchWebClient;
    private final String searchClientId;
    private final String searchClientSecret;

    // 2. 파파고용 WebClient (Naver Cloud Platform)
    private final WebClient papagoWebClient;
    private final String papagoClientId;
    private final String papagoClientSecret;
    public NaverInternetSearchTool(
            // 검색용 키 주입
            @Value("${naver.api.client-id}") String searchClientId,
            @Value("${naver.api.client-secret}") String searchClientSecret,
            // 파파고용 키 주입
            @Value("${naver.papago.client.id}") String papagoClientId,
            @Value("${naver.papago.client.secret}") String papagoClientSecret,
            WebClient.Builder webClientBuilder) {
        this.searchClientId = searchClientId;
        this.searchClientSecret = searchClientSecret;
        this.papagoClientId = papagoClientId;
        this.papagoClientSecret = papagoClientSecret;
        // [검색용] 네이버 개발자 센터 (openapi.naver.com)
        this.searchWebClient = webClientBuilder
                .baseUrl("https://openapi.naver.com")
                .defaultHeader("Accept", "application/json")
                .defaultHeader("X-Naver-Client-Id", searchClientId)
                .defaultHeader("X-Naver-Client-Secret", searchClientSecret)
                .build();

        // [번역용] 네이버 클라우드 플랫폼 (papago.apigw.ntruss.com)
        // 주의: 헤더 이름이 다릅니다! (X-NCP-APIGW-API-KEY-ID)
        this.papagoWebClient = webClientBuilder
                .baseUrl("https://naveropenapi.apigw.ntruss.com")
                .defaultHeader("X-NCP-APIGW-API-KEY-ID", papagoClientId)
                .defaultHeader("X-NCP-APIGW-API-KEY", papagoClientSecret)
                .build();
    }


    @Tool(description = "이미지 자료를 찾기 위해 인터넷 검색을 합니다.")
    public String getImgUrl(String query) {
        log.info("네이버 이미지 URL 검색 시작 : {}", query);
        try {
            NaverImageSearchResponse response = searchWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/search/image") // 이미지 검색 API 경로
                            .queryParam("query", query)
                            .queryParam("display", 1)
                            .queryParam("sort", "sim")
                            .build())
                    .header("X-Naver-Client-Id", searchClientId)
                    .header("X-Naver-Client-Secret", searchClientSecret)
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
            NaverLocalSearchResponse response = searchWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/search/local") // 지역 검색 API 경로
                            .queryParam("query", query)
                            .queryParam("display", 5) // 최대 5개까지 가져오기
                            .build())
                    .header("X-Naver-Client-Id", searchClientId)
                    .header("X-Naver-Client-Secret", searchClientSecret)
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
     * 블로그 검색(Open API) + 번역(NCP) 통합 메서드
     */
    @Tool(description = "여행지 후기나 블로그 정보를 검색하고 영어로 번역하여 제공합니다.")
    public List<BlogItem> getBlogInfo(String query) {
        log.info("네이버 블로그 검색 시작: {}", query);
        try {
            // 1. 블로그 검색 (Search WebClient 사용)
            NaverBlogSearchResponse response = searchWebClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/v1/search/blog")
                    .queryParam("query", query)
                    .queryParam("display", 5)
                    .queryParam("sort", "sim")
                    .build())
                .retrieve()
                .bodyToMono(NaverBlogSearchResponse.class)
                .block();

            if (response == null || response.getItems() == null) return null;

            List<BlogItem> items = response.getItems();
            
            // 2. 번역 (Papago WebClient 사용)
            for (BlogItem item : items) {
                String cleanTitle = removeTags(item.getTitle());
                String cleanDesc = removeTags(item.getDescription());

                item.setTitle(translateText(cleanTitle));
                item.setDescription(translateText(cleanDesc));
            }
            return items;

        } catch (Exception e) {
            log.error("검색/번역 중 오류: {}", e.getMessage());
            return null;
        }
    }

    private String translateText(String text) {
        if (text == null || text.trim().isEmpty()) return "";
        try {
            // NCP 파파고 URL: /nmt/v1/translation
            PapagoResponse response = papagoWebClient.post()
                .uri("/nmt/v1/translation") 
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
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
            log.error("번역 실패 [{}]: {}", text, e.getMessage());

        }
        return text;
    }

    private String removeTags(String text) {
        if (text == null) return "";
        return text.replaceAll("<[^>]*>", "").replaceAll("&quot;", "\"").replaceAll("&amp;", "&");
    }

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