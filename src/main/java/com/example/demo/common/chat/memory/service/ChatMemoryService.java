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

    private static final int SHORT_TERM_LIMIT = 10; // <- 너가 말한 슬롯 10개 기준

    private final EmbeddingModel embeddingModel;
    private final ChatMemoryDao dao;

    public ChatMemoryService(EmbeddingModel embeddingModel, ChatMemoryDao dao) {
        this.embeddingModel = embeddingModel;
        this.dao = dao;
    }

    // 🔹 단기 기억 개수 로그용
    public int countShortTerm(Long userId) {
        return dao.countByConversationId(userId);
    }

    // 🔹 장기 기억 개수 로그용
    public int countLongTerm(Long userId) {
        return dao.countVectorByUserId(userId);
    }

    // db에 대화메시지 저장 (String)
    public void add(Long userId, String message, String role) {

        // 1) 현재 short-term 개수 (슬롯 제한 체크용)
        int shortMemoryCount = dao.countByConversationId(userId);

        // 2) 마지막 order_index 가져오기 (없으면 null)
        Integer lastOrderIndex = dao.findMaxOrderIndex(userId);
        int nextOrderIndex = (lastOrderIndex == null) ? 0 : lastOrderIndex + 1;

        // 3) 새 메시지 저장 (order_index는 항상 이전 + 1)
        ChatMemory chatMemory = ChatMemory.builder()
                .userId(userId)
                .agentName(null)
                .orderIndex(nextOrderIndex)
                .content(message)
                .tokenUsage(null)
                .createdAt(OffsetDateTime.now())
                .role(role)
                .build();
        dao.insertChatMemory(chatMemory);

        // 4) short-term 개수 + 1 이 제한을 넘는지 확인
        int afterCount = shortMemoryCount + 1;

        // 5) SHORT_TERM_LIMIT(예: 10) 초과하면 가장 오래된 1개를 long-term으로 이동
        if (afterCount > SHORT_TERM_LIMIT) {
            ChatMemory oldest = dao.findOldestMessage(userId);
            if (oldest == null) {
                return;
            }

            // 오래된 메시지를 embedding하고 long-term으로 이동
            EmbeddingResponse embeddingResponse =
                    embeddingModel.embedForResponse(List.of(oldest.getContent()));
            float[] embedding = embeddingResponse.getResult().getOutput();

            ChatMemoryVector chatMemoryVector = ChatMemoryVector.builder()
                    .userId(userId)
                    .orderIndex(oldest.getOrderIndex()) // 기존 order_index 유지
                    .content(oldest.getContent())
                    .embedding(embedding)
                    .createdAt(oldest.getCreatedAt())
                    .role(oldest.getRole())
                    .build();
            dao.insertChatMemoryVector(chatMemoryVector);

            // short-term에서 삭제
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
