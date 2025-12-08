package com.example.demo.common.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TravelChatSendResponse {
    private boolean success;
    private String message;
    private String response; // chat.html에서 사용하는 필드명
    private Object data;

    public static TravelChatSendResponse success(String message, Object data) {
        String safeMessage = message != null ? message : "";
        return new TravelChatSendResponse(true, safeMessage, safeMessage, data);
    }

    public static TravelChatSendResponse error(String message) {
        String safeMessage = message != null ? message : "An error occurred";
        return new TravelChatSendResponse(false, safeMessage, safeMessage, null);
    }

    /**
     * Null-safe getter for message
     */
    public String getMessage() {
        return message != null ? message : "";
    }

    /**
     * Null-safe getter for response
     */
    public String getResponse() {
        return response != null ? response : "";
    }
}
