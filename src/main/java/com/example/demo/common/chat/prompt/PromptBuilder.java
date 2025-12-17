package com.example.demo.common.chat.prompt;

import org.springframework.ai.chat.prompt.Prompt;

/**
 * 프롬프트 빌더 인터페이스
 * 다양한 상황에 맞는 프롬프트를 생성하기 위한 전략 패턴
 */
public interface PromptBuilder {
    /**
     * PromptContext를 기반으로 Prompt를 생성
     *
     * @param context 프롬프트 생성에 필요한 모든 재료
     * @return Spring AI Prompt 객체
     */
    Prompt build(PromptContext context);
}
