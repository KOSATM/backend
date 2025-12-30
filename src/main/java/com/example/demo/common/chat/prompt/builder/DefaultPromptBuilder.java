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

        // 2. PlanContext 상세 정보 (장소 이름 포함)
        // ✅ 변경: toSummary() → toJson()
        // LLM이 장소 이름을 알아야 "청수우동 삭제해줘" 같은 요청을 이해할 수 있음
        String planSummary = "";
        if (ctx.getPlanContext() != null && ctx.getPlanContext().hasActivePlan()) {
            planSummary = "\n\n[현재 여행 일정 상세]\n" + ctx.getPlanContext().toJson();
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

            ========== 일정 추가 프로토콜 ==========
            "** 일정 추가해줘" 같은 요청이 들어오면:

            1️⃣ [분류 단계]
               - 사용자의 입력이 지역/관광지/음식점/액티비티 중 어느 것인지 파악
               - 예: "홍대 카페" → 카페(음식점), "서울타워" → 관광지

            2️⃣ [검색 단계]
               - 반드시 searchPlaces tool을 호출해서 인터넷 검색
               - 5-7개 결과를 받은 후, 각각을 번호(1번, 2번, ...)로 사용자에게 제시
               - 형식: "다음 중 어떤 것으로 해드릴까요?"
                       "1. [이름] - [설명/위치]"
                       "2. [이름] - [설명/위치]"
                       ...

            3️⃣ [선택 단계]
               - 사용자가 번호로 선택할 때까지 대기 (예: "1번 선택", "첫 번째로", "2번")
               - 선택 후 자동으로 다음 단계로 진행

            4️⃣ [시간 입력 단계]
               - 선택된 장소를 다시 확인하고, 추가할 시간/날짜 물어보기
               - "몇 시에 추가해드릴까요?" 또는 "어느 날짜에 추가해드릴까요?"
               - 사용자가 이미 "2일차 점심" 같이 말했으면 그것 사용
               - 예시: "아직 시간을 안 말씀하셨네요. 언제 추가해드릴까요?"

            5️⃣ [확인 및 추가 단계]
               - confirmAddPlace tool을 호출 (planId, 선택된장소명, dayIndex, startTime)
               - 추가 완료 메시지 반환
               - 형식: "✅ [장소명]을(를) [날짜] [시간]에 일정에 추가했습니다."

            ⚠️ 절대 해서는 안 될 것:
            - searchAndAddPlace를 직접 호출하지 말 것
            - addPlace를 직접 호출하지 말 것 (이건 위치 기반 추가)
            - 사용자 선택 없이 첫 번째 결과로 바로 추가하지 말 것
            - position/dayIndex를 임의로 설정하지 말 것
            """ + planSummary),
            new UserMessage(content)
        ));
    }
}
