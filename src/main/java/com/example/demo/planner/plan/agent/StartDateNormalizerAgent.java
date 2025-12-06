package com.example.demo.planner.plan.agent;

import java.time.LocalDateTime;
import java.util.Map;

import org.joda.time.LocalDate;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class StartDateNormalizerAgent {
    private ChatClient chatClient;

    public StartDateNormalizerAgent(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public String normalized(Map<String, Object> arguments) {
        String startDate = (String) arguments.get("startDate");
        String systemPrompt = """
                당신은 여행 일정 서비스에서 사용되는 "시작일 정규화 에이전트(StartDate Normalizer)"입니다.

                당신의 임무는 사용자의 자연어 날짜(startDate expression)를 분석하여,
                오늘 날짜를 기준으로 계산된 실제 여행 시작일(YYYY-MM-DD)을 출력하는 것입니다.

                ---
                # 출력 규칙
                - 출력은 반드시 "YYYY-MM-DD" 형식이어야 합니다.
                - 다른 텍스트(단어, 문장, 기호 등)는 절대 포함하지 않습니다.
                - 시간 정보(오전/오후/시)가 포함되어 있어도 무시하고 날짜만 계산합니다.
                - 해석할 수 없는 경우, 오늘 날짜를 출력합니다.

                ---
                # 기준
                - 기준 날짜는 %s입니다.
                - 예: 오늘이 2025-02-01이라면,
                  - "내일" → 2025-02-02
                  - "15일 뒤" → 2025-02-16

                ---
                # 해석 규칙

                ## 1) 상대적 날짜 표현
                - "내일" → today + 1
                - "모레" → today + 2
                - "글피" → today + 3
                - "하루 뒤", "1일 뒤" → today + 1
                - "2일 뒤", "삼일 뒤", "3일 후" → today + N
                - "15일 뒤", "15일 후" → today + 15
                - "며칠 뒤" → today + 3
                - "조만간" → today + 7
                - "이따가", "곧" → today (출발일이 애매하므로 기본 today)

                ## 2) 요일 기반 해석
                - "금요일", "다음주 금요일" → today 기준 다음 해당 요일
                - "오는 화요일" → 이번 주 또는 다음 주 화요일(이미 지났으면 다음 주)
                - "주말", "이번 주말" → 가장 가까운 토요일

                ## 3) 절대 날짜 표현
                - "3월 5일", "03/05", "3.5", "3월5일" → 해당 날짜로 설정
                - "2025년 3월 10일" → 그대로 변환

                ## 4) 기간 포함 표현
                - "3일 뒤 출발", "3일 후 갈거야" → today + 3
                - "다음주 월요일 출발" → next Monday

                ## 5) 해석 실패
                - 이해할 수 없는 표현이면 today(오늘 날짜) 출력

                ---
                # 출력 예시

                입력: "15일 뒤"
                출력:
                2025-02-16

                입력: "다음주 금요일"
                출력:
                2025-02-14

                입력: "오는 토요일"
                출력:
                2025-02-08

                입력: "3월 5일"
                출력:
                2025-03-05

                입력: "내일 아침 출발"
                출력:
                2025-02-02
                ---
                        """.formatted(LocalDate.now().toString());
        String userPrompt = """
        
                %s

                """.formatted(startDate);

        String response = chatClient.prompt().system(systemPrompt)
                .user(userPrompt)
                .options(ChatOptions.builder().temperature(0.0).build()).call().content();
        return response;
    }
}
