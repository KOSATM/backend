package com.example.demo.common.chat.memory.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class VectorStoreChatMemoryService implements ChatMemory {
    
    @Autowired
    private VectorStore vectorStore;
    
    @Override
    public void add(String conversationId, List<Message> messages) {
        List<Document> documents = new ArrayList<>();
        
        for (int i = 0; i < messages.size(); i++) {
            Message msg = messages.get(i);
            String content = msg instanceof UserMessage ? ((UserMessage) msg).getText() :
                           msg instanceof AssistantMessage ? ((AssistantMessage) msg).getText() :
                           "";
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("conversationId", conversationId);
            metadata.put("role", msg.getClass().getSimpleName());
            metadata.put("timestamp", String.valueOf(System.currentTimeMillis()));
            metadata.put("index", String.valueOf(i));
            
            Document doc = new Document(content, metadata);
            documents.add(doc);
        }
        
        vectorStore.add(documents);
        log.info("벡터 저장: conversationId={}, 메시지 수={}", conversationId, messages.size());
    }
    
    @Override
    public List<Message> get(String conversationId) {
        List<Document> documents = vectorStore.similaritySearch(
            SearchRequest.builder()
                .query(" ")
                .filterExpression("conversationId == '" + conversationId + "'")
                .topK(100)
                .build()
        );
        
        return documents.stream()
                .sorted((d1, d2) -> {
                    long ts1 = Long.parseLong((String) d1.getMetadata().get("timestamp"));
                    long ts2 = Long.parseLong((String) d2.getMetadata().get("timestamp"));
                    return Long.compare(ts1, ts2);
                })
                .map(doc -> {
                    String role = (String) doc.getMetadata().get("role");
                    if ("AssistantMessage".equals(role)) {
                        return (Message) new AssistantMessage(doc.getText());
                    } else {
                        return (Message) new UserMessage(doc.getText());
                    }
                })
                .collect(Collectors.toList());
    }
    
    @Override
    public void clear(String conversationId) {
        log.info("벡터 삭제: conversationId={}", conversationId);
    }
    
    public List<Message> getSimilarMessages(String conversationId, String query, int topK) {
        List<Document> documents = vectorStore.similaritySearch(
            SearchRequest.builder()
                .query(query)
                .filterExpression("conversationId == '" + conversationId + "'")
                .topK(topK)
                .build()
        );
        
        return documents.stream()
                .map(doc -> (Message) new UserMessage(doc.getText()))
                .collect(Collectors.toList());
    }
}