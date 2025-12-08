package com.example.demo.common.global.agent;

import org.springframework.stereotype.Component;

import com.example.demo.common.chat.intent.dto.IntentCommand;
import com.example.demo.common.chat.pipeline.AiAgentResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
// 에이전트 라우터를 위한 예시 클래스
public class SampleAiAgent implements AiAgent{@Override
    
    public AiAgentResponse execute(IntentCommand command, Long userId) {
        log.info("SampleAiAgent 에이전트 실행");
        return AiAgentResponse.of("SampleAiAgent 기능 실행 결과");
    }
}
