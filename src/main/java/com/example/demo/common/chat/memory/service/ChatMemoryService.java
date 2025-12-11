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

    // 1) 현재 short-term 메모리 개수 (해당 user 기준)
    int beforeCount = dao.countByConversationId(userId);

    // 2) 새 메시지 저장
    ChatMemory chatMemory = ChatMemory.builder()
            .userId(userId)
            .agentName(null)
            .orderIndex(beforeCount)  // 0부터 시작한다고 가정
            .content(message)
            .tokenUsage(null)
            .createdAt(OffsetDateTime.now())
            .role(role)
            .build();
    dao.insertChatMemory(chatMemory);

    // 3) 추가된 후 개수 = beforeCount + 1
    int afterCount = beforeCount + 1;

    // 4) 20개 초과면 가장 오래된 1개를 long-term으로 이동
    // if (afterCount > 20) {
    if (afterCount > 10) {
        ChatMemory oldest = dao.findOldestMessage(userId);
        if (oldest == null) {
            // 안전장치: 혹시 null이면 로그 찍고 그냥 리턴
            return;
        }

        // 4-1) embedding 생성 후 long-term 테이블로 저장
        EmbeddingResponse embeddingResponse =
                embeddingModel.embedForResponse(List.of(oldest.getContent()));
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

        // 4-2) short-term에서 삭제
        dao.deleteChatMemoryById(oldest.getId());
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
