package com.example.demo.common.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TravelChatSendResponse {
    private boolean success;
    private String message;
    private Object data;

    public static TravelChatSendResponse success(String message, Object data) {
        return new TravelChatSendResponse(true, message, data);
    }

    public static TravelChatSendResponse error(String message) {
        return new TravelChatSendResponse(false, message, null);
    }
}
