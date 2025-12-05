package com.example.demo.common.chat.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TravelChatSendRequest {
    private Long userId;
    private String message;
}
