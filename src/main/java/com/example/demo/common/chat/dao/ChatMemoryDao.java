package com.example.demo.common.chat.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.common.chat.dto.ChatMemory;

@Mapper
public interface ChatMemoryDao {
    void insert(ChatMemory chatMemory);
    List<ChatMemory> selectByUserId(Long userId);
    void deleteByUserId(Long userId);
}
