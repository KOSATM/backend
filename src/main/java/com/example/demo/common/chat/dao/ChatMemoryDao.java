package com.example.demo.common.chat.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.demo.common.chat.dto.ChatMemory;
import com.example.demo.common.chat.dto.ChatMemoryVector;

@Mapper
public interface ChatMemoryDao {
    void insertChatMemory(ChatMemory chatMemory);
    void insertChatMemoryVector(ChatMemoryVector chatMemoryVector);

    // 참조는 userId로 확실히
    int countByConversationId(@Param("userId") Long userId);

    List<ChatMemoryVector> getSimilarMessages(java.util.Map<String, Object> paramMap);
    List<ChatMemory> findRecentMessages(@Param("userId") Long userId, @Param("limit") int limit);

    // 삭제는 id(pk) 기준으로
    void deleteChatMemoryById(@Param("id") Long id);
    
    ChatMemory findOldestMessage(Long userId);

    // ✅ 추가: 해당 user의 마지막 order_index 조회
    Integer findMaxOrderIndex(@Param("userId") Long userId);

    // 🔹 장기 기억(벡터) 개수
    int countVectorByUserId(@Param("userId") Long userId);
    
}
