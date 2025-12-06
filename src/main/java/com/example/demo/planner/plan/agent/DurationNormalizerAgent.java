package com.example.demo.planner.plan.agent;

import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Component;

@Component
public class DurationNormalizerAgent {
    private ChatClient chatClient;

    public DurationNormalizerAgent(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public String normalized(Map<String, Object> arguments) {
        String duration = (String) arguments.getOrDefault("duration", "1");

        String systemPrompt = """
                당신은 여행 일정 서비스에서 사용되는 "duration 정규화 에이전트(Duration Normalizer)"입니다.

                당신의 임무는 사용자의 자연어 기간(duration) 표현을 분석하여,
                여행 일정 생성에 필요한 일수(day)를 1~7 범위의 정수로 변환하는 것입니다.

                ---

                # 출력 규칙
                - 오직 1~7 사이의 정수만 출력합니다.
                - 정수 외의 텍스트(단어/문장/기호/설명)는 절대 포함하지 않습니다.
                - 소수점, 단위(일/박), 문장 금지.

                ---

                # 해석 규칙

                ## 명확한 기간 표현
                - "3일", "삼일", "사흘", "이틀", "하루", "5일" → 그대로 숫자로 변환
                - "3박 4일" → 4
                - "당일치기" → 1
                - "주말" → 2

                ## 범위/애매한 표현
                - "2~3일" → 3
                - "하루 반" → 2
                - "며칠" → 3
                - "짧게" → 2
                - "길게" → 4
                - "여유롭게" → 4
                - "적당히" → 3

                ## 숫자 보정(Clamping)
                - 결과가 1보다 작으면 → 1
                - 결과가 7보다 크면 → 7

                ---

                # 출력 예시

                입력: "삼일 정도"
                출력:
                3

                입력: "주말 코스로"
                출력:
                2

                입력: "일주일 이상 넉넉하게"
                출력:
                7

                ---

        """;
        String userPrompt = """

                %s

                """.formatted(duration);

        String response = chatClient.prompt().system(systemPrompt)
                .user(userPrompt)
                .options(ChatOptions.builder().temperature(0.0).build()).call().content();
        return response;
    }
}
