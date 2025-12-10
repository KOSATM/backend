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
    int countByConversationId(Long userId);
    List<ChatMemoryVector> getSimilarMessages(java.util.Map<String, Object> paramMap);
    List<ChatMemory> findRecentMessages(@Param("userId") Long userId, @Param("limit") int limit);
    void deleteChatMemory(Long userId);
    ChatMemory findOldestMessage(Long userId);
}
