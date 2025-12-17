package com.example.demo.common.chat.prompt.builder;

import java.util.List;

import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import com.example.demo.common.chat.memory.builder.MemoryPromptBuilder;
import com.example.demo.common.chat.prompt.PromptBuilder;
import com.example.demo.common.chat.prompt.PromptContext;

/**
 * 기본 프롬프트 빌더
 * 
 * 설계 원칙:
 * - 기존 MemoryPromptBuilder 로직을 감싸서 Spring AI Prompt로 변환
 * - ChatMemory를 상태 저장소로 활용
 * - PendingAction 없이도 턴 간 컨텍스트 유지 가능
 * 
 * PendingAction을 사용하지 않는 이유:
 * - chatMemory에 충분한 대화 맥락 포함
 * - LLM이 자연스럽게 이전 대화 참조 가능
 * - "2번으로 해줘", "응", "그래" 등의 짧은 응답도 처리 가능
 * - 명시적 상태 관리 오버헤드 제거
 * 
 * ⚠️ 이 단계에서는 기존 로직을 그대로 재사용하며,
 * 새로운 프롬프트 로직을 추가하지 않음
 */
@Component
public class DefaultPromptBuilder implements PromptBuilder {

    @Override
    public Prompt build(PromptContext ctx) {

        // 1. MemoryBundle이 있으면 Memory 기반 프롬프트 사용
        String content;
        if (ctx.getMemoryBundle() != null) {
            content = MemoryPromptBuilder.build(
                ctx.getMemoryBundle(),
                ctx.getUserMessage()
            );
        } else {
            // MemoryBundle이 없으면 단순 메시지만 사용
            content = ctx.getUserMessage();
        }

        // 2. PlanContext 요약 (Level 1: Light Snapshot)
        String planSummary = "";
        if (ctx.getPlanContext() != null && ctx.getPlanContext().hasActivePlan()) {
            planSummary = "\n\n[현재 여행 일정 요약]\n" + ctx.getPlanContext().toSummary();
        }

        // 3. Spring AI Prompt로 래핑
        // 💡 Tool 호출 여부는 LLM이 자율 판단 → 시스템은 결과만 필터링
        // 💡 요약된 일정만 제공 → 정확한 정보는 tool로 조회
        return new Prompt(List.of(
            new SystemMessage("""
            너는 서울 여행 일정과 장소 관리를 도와주는 AI 여행 플래너다.
            
            규칙:
            - 불확실한 정보는 tool로 검색
            - 정확한 일정 정보가 필요하면 tool로 조회
            - 사용자 확인이 필요하면 자연스럽게 질문
            - 확정된 정보만 DB에 저장
            """ + planSummary),
            new UserMessage(content)
        ));
    }
}
