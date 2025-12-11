package com.example.demo.common.chat.memory.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.common.chat.dto.ChatMemory;
import com.example.demo.common.chat.dto.ChatMemoryVector;
import com.example.demo.common.chat.dto.MemoryBundle;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
//LLM의 이해를 돕기 위해서는 틀을 갖추는 것이 중요
public class MemoryRetrievalService {
    private final ChatMemoryService chatMemoryService;

    public MemoryBundle retrieveAll(Long userId, String query, Object context ) {
        // 1) Short-term: 최근 N개 (기본 20)
        List<ChatMemory> shortMemory = chatMemoryService.getRecentMessage(userId, 20);

        // 2) Long-term: 벡터 유사도 기반 topK (기본 3)
        List<ChatMemoryVector> longMemory = chatMemoryService.getSimilarMessages(userId, query, 3);

        return MemoryBundle.builder()
            .shortMemory(shortMemory)
            .longMemory(longMemory)
            .build();
    }
}
