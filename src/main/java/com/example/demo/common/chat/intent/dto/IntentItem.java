package com.example.demo.common.chat.intent.dto;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;


@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class IntentItem {
    private String originalUserMessage;
    private String intent;              // plan_add, translation, create_post, etc
    private double confidence;              // LLM confidence
    private Map<String, Object> arguments;  // dynamic params
}