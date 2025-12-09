package com.example.demo.common.chat.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.common.chat.service.ChatMemoryService;

import lombok.extern.slf4j.Slf4j;



@RestController
@Slf4j
public class ChatController {

    private final ChatMemoryService chatMemoryService;
    public ChatController(ChatMemoryService chatMemoryService) {
        this.chatMemoryService = chatMemoryService;
    }

}
