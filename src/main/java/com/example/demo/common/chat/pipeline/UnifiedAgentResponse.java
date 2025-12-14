package com.example.demo.common.chat.pipeline;

import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * 모든 Agent의 응답을 통일하는 DTO
 * AiAgentResponse를 확장하여 변경 내역(changes) 정보를 추가
 * 
 * message: 사용자에게 보여줄 채팅 메시지
 * changes: 일정 변경 내역 (key-value 형태) - 예: "added_places": 3, "modified_days": 1
 * data: 추가 데이터 (옵션)
 */
@Getter
@Builder
@ToString
@AllArgsConstructor
public class UnifiedAgentResponse {
    
    /** 사용자에게 보여줄 채팅 메시지 */
    private final String message;
    
    /** 일정 변경 내역 (예: "일정 3개 추가", "2일차 수정", "장소 교체" 등) */
    @Builder.Default
    private final Map<String, Object> changes = new HashMap<>();
    
    /** 추가 데이터 (선택사항) */
    private final Object data;
    
    /** 페이지 이동 필요 여부 */
    @Builder.Default
    private final boolean requirePageMove = false;
    
    /** 이동할 페이지 URL */
    private final String targetUrl;
    
    /**
     * 메시지만 있는 간단한 응답
     */
    public static UnifiedAgentResponse message(String message) {
        return UnifiedAgentResponse.builder()
                .message(message)
                .build();
    }
    
    /**
     * 메시지 + 변경사항
     */
    public static UnifiedAgentResponse withChanges(String message, Map<String, Object> changes) {
        return UnifiedAgentResponse.builder()
                .message(message)
                .changes(changes)
                .build();
    }
    
    /**
     * 메시지 + 변경사항 + 페이지 이동
     */
    public static UnifiedAgentResponse withPageMove(String message, String url, Map<String, Object> changes) {
        return UnifiedAgentResponse.builder()
                .message(message)
                .changes(changes)
                .requirePageMove(true)
                .targetUrl(url)
                .build();
    }
    
    /**
     * 메시지 + 추가 데이터
     */
    public static UnifiedAgentResponse withData(String message, Object data) {
        return UnifiedAgentResponse.builder()
                .message(message)
                .data(data)
                .build();
    }
}
