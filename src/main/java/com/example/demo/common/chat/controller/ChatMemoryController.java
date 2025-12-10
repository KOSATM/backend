package com.example.demo.common.chat.controller;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.common.chat.dto.ChatMemoryVector;
import com.example.demo.common.chat.dto.MemoryBundle;
import com.example.demo.common.chat.memory.builder.MemoryPromptBuilder;
import com.example.demo.common.chat.memory.service.ChatMemoryService;
import com.example.demo.common.chat.memory.service.MemoryRetrievalService;

import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class ChatMemoryController {

    private final ChatMemoryService chatMemoryService;
    private ChatClient chatClient;
    private MemoryRetrievalService memoryRetrievalService;

    public ChatMemoryController(ChatMemoryService chatMemoryService, ChatClient.Builder chatClientBuilder,
            MemoryRetrievalService memoryRetrievalService) {
        this.chatMemoryService = chatMemoryService;
        this.chatClient = chatClientBuilder.build();
        this.memoryRetrievalService = memoryRetrievalService;
    }

    @PostMapping("/chat/send")
    public String chatWithAI(
            @RequestParam("userId") Long userId,
            @RequestParam("message") String message) {

        // 1. 유저 메시지 저장
        chatMemoryService.add(userId, message, "user");

        // 2. MemoryBundle 획득
        MemoryBundle memoryBundle = memoryRetrievalService.retrieveAll(userId, message, null);

        // 3. MemoryPromptBuilder로 프롬프트 생성
        String builtPrompt = MemoryPromptBuilder.build(memoryBundle, message);

        log.info("\n====== Generated Prompt ======\n{}\n==============================", builtPrompt);

        // 4. LLM 호출 (SystemMessage로 프롬프트 전체 삽입)
        String llmResponse = chatClient
            .prompt()
            .messages(List.of(
                    new SystemMessage(builtPrompt),
                    new UserMessage(message)))
            .call()
            .content();

        // 5. assintant 저장
        chatMemoryService.add(userId, llmResponse, "assistant");

        return llmResponse;
    }

    // 기존 테스트 엔드포인트 유지
    @PostMapping("/test/add")
    public void testAdd(@RequestParam("userId") Long userId, @RequestParam("message") String message,
            @RequestParam("role") String role) {
        chatMemoryService.add(userId, message, role);
    }

    @GetMapping("/test/similar")
    public List<ChatMemoryVector> testSimilar(@RequestParam("userId") Long userId, @RequestParam("query") String query,
            @RequestParam("topK") int topK) {
        return chatMemoryService.getSimilarMessages(userId, query, topK);
    }
}
