package com.example.demo.common.chat.intent.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntentRequest {

    private String userMessage;         // 사용자가 입력한 자연어 메시지
    private String currentUrl;          // 현재 클라이언트 페이지 URL
}
