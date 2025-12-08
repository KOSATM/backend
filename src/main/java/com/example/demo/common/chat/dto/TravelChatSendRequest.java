package com.example.demo.common.chat.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TravelChatSendRequest {
    private Long userId;
    private String message;
    private String currentUrl;  // 현재 페이지 URL (파이프라인 컨텍스트용)
}
