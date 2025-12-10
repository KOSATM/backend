package com.example.demo.common.chat.memory.service;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;

import com.example.demo.common.chat.dao.ChatMemoryDao;
import com.example.demo.common.chat.dto.ChatMemory;
import com.example.demo.common.chat.dto.ChatMemoryVector;

@Service
public class ChatMemoryService {
    private final EmbeddingModel embeddingModel;
    private final ChatMemoryDao dao;

    public ChatMemoryService(EmbeddingModel embeddingModel, ChatMemoryDao dao) {
        this.embeddingModel = embeddingModel;
        this.dao = dao;
    }

    // db에 대화메시지 저장 (String)
    public void add(Long userId, String message, String role) {
        int count = dao.countByConversationId(userId);
        ChatMemory chatMemory = ChatMemory.builder()
                .userId(userId)
                // 임시 처리
                .agentName(null)
                .orderIndex(count)
                .content(message)
                .tokenUsage(null)
                .createdAt(OffsetDateTime.now())
                .role(role)
                .build();
        dao.insertChatMemory(chatMemory);

        // 2) 단기기억 개수 확인
        int shortMemoryCount = dao.countByConversationId(userId);

        // 3) 20개 초과하면 가장 오래된 1개를 장기기억으로 이동
        if (shortMemoryCount > 20) {
            ChatMemory oldest = dao.findOldestMessage(userId);

            // --- 3-1. 오래된 메시지를 embedding하고 long-term으로 이동
            EmbeddingResponse embeddingResponse = embeddingModel.embedForResponse(List.of(oldest.getContent()));
            float[] embedding = embeddingResponse.getResult().getOutput();

            ChatMemoryVector chatMemoryVector = ChatMemoryVector.builder()
                    .userId(userId)
                    .orderIndex(oldest.getOrderIndex())
                    .content(oldest.getContent())
                    .embedding(embedding)
                    .createdAt(oldest.getCreatedAt())
                    .role(oldest.getRole())
                    .build();
            dao.insertChatMemoryVector(chatMemoryVector);

            // --- 3-2. short-term에서 삭제
            dao.deleteChatMemory(oldest.getId());
        }
    }

    // vector 비교 후 유사한 메시지 불러오기
    public List<ChatMemoryVector> getSimilarMessages(Long userId, String query, int topK) {
        EmbeddingResponse embeddingResponse = embeddingModel.embedForResponse(List.of(query));
        float[] embeddingArr = embeddingResponse.getResult().getOutput();

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("userId", userId);
        paramMap.put("queryEmbedding", embeddingArr);
        paramMap.put("topK", topK);
        // dao에서 벡터 유사도 기반으로 유사 메시지 topK개 조회

        return dao.getSimilarMessages(paramMap);
    }

    // 최근 20개 일반 메시지 불러오기
    public List<ChatMemory> getRecentMessage(Long userId, int limit) {
        return dao.findRecentMessages(userId, limit);
    }
}
