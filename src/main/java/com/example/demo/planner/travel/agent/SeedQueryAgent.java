package com.example.demo.planner.travel.agent;

import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SeedQueryAgent {

    private ChatClient chatClient;

    public SeedQueryAgent(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public String generateSeedQuery(Map<String, Object> seedQueryArgs) {
        String location = (String) seedQueryArgs.getOrDefault("location", "서울");

        String systemPrompt = """
                당신은 여행지 추천을 위한 "Seed Query 생성 에이전트"입니다.

                당신의 목적은 사용자가 제공한 위치(location) 정보를 기반으로,
                벡터 검색(semantic search)에 최적화된 자연어 문장을 생성하는 것입니다.

                이 문장은 Embedding 모델이 가장 잘 이해할 수 있는 형태여야 하며,
                사용자의 의도를 정확하게 반영해야 합니다.

                ---

                # ✔ 입력 정보
                - 중심 위치 목록: 문자열 배열 (예: ["강남역", "서초"])

                ---

                # ✔ 출력 규칙
                1. **자연스러운 한 문장만 생성합니다.**
                2. **장소명은 반드시 원본 그대로 포함합니다.**
                3. **여러 장소가 있을 경우 자연스럽게 연결합니다.**
                   - 예: ["강남역", "서초"] → "강남역과 서초 일대"
                4. **주어진 위치만 사용합니다. 새로운 장소 추론 금지.**
                5. **여행지/관광지 추천 목적에 맞는 문장으로 작성합니다.**
                6. **설명 없이 문장 하나만 출력합니다.**

                ---

                # ✔ intent별 문장 스타일 가이드

                ## TRAVEL_PLAN
                - 여행 일정 또는 동선을 계획하는 느낌
                - 예: "강남역과 서초 일대를 중심으로 여행 일정을 추천해줘."

                ## ATTRACTION_RECOMMEND
                - 단순 관광지 추천 느낌
                - 예: "강남역과 서초 주변에서 가볼 만한 여행지를 추천해줘."

                ---

                # ✔ 출력 예시

                입력:
                locations = ["강남역", "서초"]
                intent = TRAVEL_PLAN

                출력:
                "강남역과 서초 일대를 중심으로 여행 일정을 추천해줘."

                ---

                입력:
                locations = ["홍대입구"]
                intent = ATTRACTION_RECOMMEND

                출력:
                "홍대입구 주변에서 가볼 만한 여행지를 추천해줘."

                ---

                이 규칙을 모두 지켜서 seed query 문장을 생성하십시오.

                """;
        String userPrompt = """
                다음 정보를 기반으로 벡터 검색용 Seed Query 문장을 하나 생성해줘.

                - location: %s

                문장은 단 하나만 출력해줘.
                """.formatted(location);

        String response = chatClient.prompt().system(systemPrompt)
                .user(userPrompt)
                .options(ChatOptions.builder().temperature(0.1).build()).call().content();
        return response;
    }
}
