package com.example.demo.test.testchat.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.demo.common.chat.dto.MemoryBundle;
import com.example.demo.common.chat.memory.builder.MemoryPromptBuilder;
import com.example.demo.common.chat.memory.service.ChatMemoryService;
import com.example.demo.common.chat.memory.service.MemoryRetrievalService;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ConversationJHTestAgent {

    private ChatClient chatClient;
    private ChatMemoryService chatMemoryService;
    private MemoryRetrievalService memoryRetrievalService;

    @Autowired
    public ConversationJHTestAgent(
        ChatClient.Builder chatClientBuilder,
        ChatMemoryService chatMemoryService,
        MemoryRetrievalService memoryRetrievalService

    ) {
        this.chatClient = chatClientBuilder.build();
        this.chatMemoryService = chatMemoryService;
        this.memoryRetrievalService = memoryRetrievalService;
    }

    public String chat(Long userId, String userMessage) {
        log.info("📥 사용자 입력 (userId: {}): {}", userId, userMessage);

        // 1️⃣ 메모리 조회 (Short + Long term)
        MemoryBundle memoryBundle = memoryRetrievalService.retrieveAll(userId, userMessage, null);
        
        // 2️⃣ 메모리를 포함한 프롬프트 생성
        String memoryPrompt = MemoryPromptBuilder.build(memoryBundle, userMessage);

        // 3️⃣ LLM에 메모리를 포함한 프롬프트로 요청
        String llmResponse = chatClient.prompt()
                .system("""
                        당신은 친절한 채팅 어시스턴트입니다.
                        사용자와 자연스럽게 대화하세요.
                        이전 대화 내용을 참고하여 일관성 있는 답변을 제공하세요.
                        """)
                .user(memoryPrompt)
                .call()
                .content();

        // 4️⃣ 사용자 메시지 저장
        chatMemoryService.add(userId, userMessage, "user");
        
        // 5️⃣ AI 응답 저장
        chatMemoryService.add(userId, llmResponse, "assistant");
        
        return llmResponse;
    }
}