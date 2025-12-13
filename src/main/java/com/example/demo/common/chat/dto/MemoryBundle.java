package com.example.demo.common.chat.dto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MemoryBundle {
    private List<ChatMemory> shortMemory;
    private List<ChatMemoryVector> longMemory;
    private Object context; //향후 멀티모달 확장
}
