package com.example.demo.common.chat.pipeline;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class AiAgentResponse {

    private final String message;       // AI 모델이 만든 응답 메시지
    private final boolean requirePageMove;
    private final String targetUrl;     // 페이지 이동 필요 시 이동 URL

    public static AiAgentResponse of(String message) {
        return AiAgentResponse.builder()
                .message(message != null ? message : "")
                .requirePageMove(false)
                .build();
    }

    public static AiAgentResponse pageMove(String message, String url) {
        return AiAgentResponse.builder()
                .message(message != null ? message : "")
                .requirePageMove(true)
                .targetUrl(url != null ? url : "")
                .build();
    }

    /**
     * message가 null이면 빈 문자열 반환
     */
    public String getMessage() {
        return message != null ? message : "";
    }
}
