package com.example.demo.common.chat.intent.agent;

import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

import com.example.demo.common.chat.intent.dto.request.FilteredRequest;
import com.example.demo.common.chat.intent.dto.request.IntentRequest;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 입력 필터링 및 정규화 에이전트
 * IntentAnalysisAgent 앞단에서 사용자 입력을 전처리
 * 
 * 책임:
 * 1. 위험 콘텐츠 필터링 (욕설, 불법, 개인정보)
 * 2. 날짜/시간 표현 정규화
 * 3. 장소명 오타 보정
 * 4. 숫자/단위 표준화
 * 5. IntentAnalysisAgent가 읽기 쉬운 형태로 변환
 */
@Component
@Slf4j
public class InputFilteringAgent {

    private final ChatClient chatClient;

    public InputFilteringAgent(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * 사용자 입력 필터링 (위험 콘텐츠만 체크)
     * 정규화는 각 도메인 Agent에서 처리
     */
    public FilteredRequest filter(IntentRequest request) {
        BeanOutputConverter<FilteringResponse> converter = new BeanOutputConverter<>(FilteringResponse.class);

        String systemPrompt = """
            당신은 입력 필터링 에이전트입니다.
            
            임무:
            1. 위험 콘텐츠 감지 (욕설, 불법 행위, 개인정보 노출)
               - 감지 시 isBlocked=true, blockReasons에 사유 기록
            
            2. 정규화 없음
               - 날짜/시간/장소/숫자는 원본 그대로 유지
               - normalizedText = rawText (변경 없음)
               - 각 도메인 Agent가 자체적으로 처리
            
            규칙:
            - 위험 콘텐츠만 차단
            - 나머지는 모두 통과
            
            출력 형식 (JSON만 출력):
            {
              "isBlocked": false,
              "blockReasons": [],
              "rawText": "원본 메시지",
              "normalizedText": "원본 메시지",
              "entities": {}
            }

            규칙:
            - JSON만 출력
            - 위험 콘텐츠만 차단
            - normalizedText = rawText (변경 없음)
            """;

        String userPrompt = """
            원본 메시지: "%s"
            
            위 메시지에 위험 콘텐츠가 있는지만 확인하세요.
            """.formatted(request.getUserMessage());

        try {
            String responseJson = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .options(ChatOptions.builder()
                    .temperature(0.0)
                    .build())
                .call()
                .content();

            log.info("Filtering Response JSON: {}", responseJson);

            if (responseJson == null || responseJson.isBlank()) {
                log.warn("Empty response from LLM, returning unfiltered");
                return createFallbackResponse(request);
            }

            FilteringResponse response = converter.convert(responseJson);

            if (response == null) {
                log.warn("Failed to parse filtering response, returning unfiltered");
                return createFallbackResponse(request);
            }

            return FilteredRequest.builder()
                .isBlocked(response.isBlocked)
                .blockReasons(response.blockReasons != null ? response.blockReasons : List.of())
                .rawText(response.rawText != null ? response.rawText : request.getUserMessage())
                .normalizedText(response.normalizedText != null ? response.normalizedText : request.getUserMessage())
                .entities(response.entities != null ? response.entities : Map.of())
                .currentUrl(request.getCurrentUrl())
                .build();

        } catch (Exception e) {
            log.error("Input filtering failed: {}", e.getMessage(), e);
            return createFallbackResponse(request);
        }
    }

    /**
     * LLM 실패 시 Fallback 응답 (원본 그대로 전달)
     */
    private FilteredRequest createFallbackResponse(IntentRequest request) {
        return FilteredRequest.builder()
            .isBlocked(false)
            .blockReasons(List.of())
            .rawText(request.getUserMessage())
            .normalizedText(request.getUserMessage())
            .entities(Map.of())
            .currentUrl(request.getCurrentUrl())
            .build();
    }

    /**
     * LLM 응답 매핑용 내부 클래스
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FilteringResponse {
        @JsonProperty("isBlocked")
        private boolean isBlocked;

        @JsonProperty("blockReasons")
        private List<String> blockReasons;

        @JsonProperty("rawText")
        private String rawText;

        @JsonProperty("normalizedText")
        private String normalizedText;

        @JsonProperty("entities")
        private Map<String, Object> entities;
    }
}
