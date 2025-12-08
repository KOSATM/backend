package com.example.demo.common.chat.pipeline;

import com.example.demo.common.chat.intent.dto.request.IntentRequest;

public interface ChatPipeline {
    PipelineResult execute(IntentRequest request, Long userId);
}
