package com.example.demo.common.chat.intent.dto;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntentItem {

    private String category;          // planner, supporter, travelgram, etc
    private String intent;              // plan_add, translation, create_post, etc
    private double confidence;              // LLM confidence
    private Map<String, Object> arguments;  // dynamic params
}