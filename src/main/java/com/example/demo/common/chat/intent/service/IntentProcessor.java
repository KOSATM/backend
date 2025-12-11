package com.example.demo.common.chat.intent.service;

import org.springframework.stereotype.Component;

import com.example.demo.common.chat.intent.IntentType;
import com.example.demo.common.chat.intent.dto.IntentCommand;
import com.example.demo.common.chat.intent.dto.IntentItem;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class IntentProcessor {

    public IntentCommand toCommand(IntentItem item) {
        log.info("item: {}", item);
        // 문자열로 온 Intent를 Enum으로 변환
        IntentType intentType = IntentType.fromValue(item.getIntent());

        // Enum이 이미 requiredUrl, category를 포함하고 있음
        return IntentCommand.builder()
                .originalUserMessage(item.getOriginalUserMessage())
                .category(intentType.getCategory())
                .intent(intentType)
                .confidence(item.getConfidence())
                .arguments(item.getArguments())
                .requiredUrl(intentType.getRequiredUrl())
                .build();
    }
}
