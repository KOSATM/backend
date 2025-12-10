package com.example.demo.test.testchat.agent;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ConversationTestAgent {

    private ChatClient chatClient;
    private ChatMemory chatMemory;

    @Autowired
    public ConversationTestAgent(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory) {
        this.chatClient = chatClientBuilder.build();
        this.chatMemory = chatMemory;
    }

    public String chat(Long userId, String userMessage) {
        String conversationId = "user_" + userId;
        
        // 1) 메모리 상태 출력 (저장 전)
        debugChatMemory(conversationId, "저장 전");
        
        // 2) 이전 대화 조회
        List<org.springframework.ai.chat.messages.Message> previousMessages = chatMemory.get(conversationId);
        
        // 3) LLM 호출
        log.info("\n📥 사용자 입력: {}", userMessage);
        
        var promptBuilder = chatClient.prompt()
                .system("""
                        당신은 친절한 채팅 어시스턴트입니다.
                        사용자와 자연스럽게 대화하세요.
                        이전 대화 내용을 고려하여 일관성 있게 응답하세요.
                        """)
                .user(userMessage);
        
        // 이전 대화가 있으면 추가
        if (previousMessages != null && !previousMessages.isEmpty()) {
            promptBuilder.messages(previousMessages);
            log.info("✅ 이전 대화 {} 개 포함", previousMessages.size());
        } else {
            log.info("ℹ️ 이전 대화 없음 (첫 메시지)");
        }
        
        String llmResponse = promptBuilder.call().content();
        
        log.info("📤 AI 응답: {}", llmResponse);
        
        // 4) 메모리에 저장
        chatMemory.add(conversationId, List.of(
                new UserMessage(userMessage),
                new AssistantMessage(llmResponse)
        ));
        log.info("💾 대화 저장 완료");
        
        // 5) 저장 후 메모리 상태 출력
        debugChatMemory(conversationId, "저장 후");
        
        return llmResponse;
    }

    private void debugChatMemory(String conversationId, String stage) {
        List<org.springframework.ai.chat.messages.Message> messages = chatMemory.get(conversationId);
        
        log.info("\n========== 📊 ChatMemory 상태 ({}) ==========", stage);
        log.info("conversationId: {}", conversationId);
        log.info("총 메시지 수: {}", messages != null ? messages.size() : 0);
        
        if (messages != null && !messages.isEmpty()) {
            for (int i = 0; i < messages.size(); i++) {
                org.springframework.ai.chat.messages.Message msg = messages.get(i);
                String type = msg.getClass().getSimpleName();
                String content = msg instanceof UserMessage ? 
                    ((UserMessage) msg).getText() :
                    msg instanceof AssistantMessage ?
                    ((AssistantMessage) msg).getText() : "";
                
                String truncated = content.length() > 80 ? content.substring(0, 80) + "..." : content;
                log.info("[메시지 #{}] {}: {}", i, type, truncated);
            }
        } else {
            log.info("저장된 메시지 없음");
        }
        log.info("====================================================\n");
    }
}