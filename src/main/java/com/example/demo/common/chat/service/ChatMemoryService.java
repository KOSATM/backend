package com.example.demo.common.chat.service;

import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ChatMemoryService {

    private final EmbeddingModel embeddingModel;
    private final JdbcTemplate jdbcTemplate;

    public ChatMemoryService(EmbeddingModel embeddingModel, JdbcTemplate jdbcTemplate) {
        this.embeddingModel = embeddingModel;
        this.jdbcTemplate = jdbcTemplate;
    }

    // 1. 메시지 저장 (user/assistant 모두)
    public void saveMessage(Long userId, String role, String message) {
        float[] embedding = getEmbedding(message);
        String sql = "INSERT INTO chat_history (user_id, role, message, embedding, created_at) VALUES (?, ?, ?, ?::vector, now())";
        jdbcTemplate.update(sql, userId, role, message, arrayToPgVector(embedding));
    }

    // 2. 유사 메시지 검색
    public List<Map<String, Object>> findSimilarMessages(Long userId, String question, int limit) {
        float[] questionEmbedding = getEmbedding(question);
        String sql = """
            SELECT message, role
              FROM chat_history
             WHERE user_id = ?
             ORDER BY embedding <=> ?::vector
             LIMIT ?
            """;
        return jdbcTemplate.queryForList(sql, userId, arrayToPgVector(questionEmbedding), limit);
    }

    // 3. 임베딩 생성
    private float[] getEmbedding(String text) {
        EmbeddingResponse response = embeddingModel.embedForResponse(List.of(text));
        return response.getResult().getOutput();
    }

    // 4. float[] → PostgreSQL vector 문자열 변환
    private String arrayToPgVector(float[] vector) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) sb.append(',');
        }
        sb.append(']');
        return sb.toString();
    }
    

}
