package com.example.demo.travelgram.review.ai.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import com.example.demo.travelgram.review.ai.dto.response.GeneratedStyleResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class ReviewStyleGenerateAgent {

    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper;

    public GeneratedStyleResponse generateStyles(String tripJson, String mood, String travelType) {
        ChatClient chatClient = chatClientBuilder.build();

        String systemPrompt = """
                당신은 전문 인스타그램 여행 콘텐츠 크리에이터입니다.
                제공된 여행 데이터와 전체 분위기를 기반으로, 서로 다른 4가지 스타일의 캡션과 해시태그를 생성하는 것이 당신의 역할입니다.

                ## 입력 데이터
                1. 여행 데이터(JSON): 장소, 메모, 비용, 날짜 등 포함
                2. 전체 분위기(Overall Mood): 예) 편안함, 설렘, 활기 등
                3. 여행 유형(Travel Type): SOLO, GROUP, UNCLEAR

                ## 출력 요구사항 (Strict JSON)
                아래 구조대로 4가지 스타일을 포함한 JSON 객체를 반환해야 합니다.

                ### 스타일 유형
                1. **EMOTIONAL: 감정, 분위기, 풍경 중심. 부드럽고 감성적인 톤.**
                2. **INFORMATIVE: 팁, 장소명, 동선 중심. 유용한 톤. (구체 가격 숫자 언급 금지)**
                3. **WITTY: 짧고 재치 있는 톤, 가벼운 유머, 이모지 활용 가능.**
                4. **SIMPLE: 매우 간결, 시크, 해시태그 중심. 최소 표현.**

                ## 규칙
                1. 언어: 반드시 **한국어 ONLY**.
                2. 해시태그: **정확히 10개의 한국어 해시태그**만 생성.
                3. 콘텐츠 핵심 사항:
                   - 입력 데이터의 `memo`, `place_name` 반드시 활용
                   - 존재하지 않는 장소명 생성 금지
                   - 구체 비용 언급 금지 (예: $50, 10000원 → 불가)
                     - 대신 “플렉스”, “소확행”, “가성비” 등의 표현은 가능
                   - `caption`에는 **텍스트만** 작성 (해시태그 포함 금지)
                   - 절대 요약하지 말고 풍부하게 표현
                   - 분위기, 장면감, 시간감 묘사 허용
                   - 최소 5문장을 생성해야 하며, 최대 10문장까지 생성해야 합니다.
                   - 줄바꿈도 적절히 넣어주세요.
                4. 톤 이름 규칙:
                   - `toneName`은 **한국어로 작성**
                   - `toneCode`보다 길고 스타일적 분위기가 드러나야 함
                     - 예) toneCode: EMOTIONAL
                         toneName: 깊고 서정적인 분위기 표현

                ## JSON 구조
                ## Output Requirements (Strict JSON)
                {
                "styles": [
                    {
                    "toneCode": "EMOTIONAL",
                    "toneName": "여운이 오래 남는 서정적 감성",
                    "caption": "... (최소 5문장, 감축 금지)",
                    "hashtags": ["#...", "#..."]
                    },
                    {
                    "toneCode": "INFORMATIVE",
                    "toneName": "동선과 팁 중심의 실용적 안내",
                    "caption": "... (최소 5문장)",
                    "hashtags": ["#...", "#..."]
                    },
                    {
                    "toneCode": "WITTY",
                    "toneName": "재치 있고 가볍게 웃음주는 톤",
                    "caption": "... (짧고 위트, 5문장)",
                    "hashtags": ["#...", "#..."]
                    },
                    {
                    "toneCode": "SIMPLE",
                    "toneName": "미니멀하고 시크한 표현",
                    "caption": "... (5문장, 깔끔)",
                    "hashtags": ["#...", "#..."]
                    }
                ]
                }


                """;

        String userPrompt = String.format("""
                ## Trip Data:
                %s

                ## Context:
                - Mood: %s
                - Type: %s
                """, tripJson, mood, travelType);

        try {
            String response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();

            // 마크다운 제거 (```json ...)
            String cleanJson = response.replaceAll("```json", "").replaceAll("```", "").trim();

            return objectMapper.readValue(cleanJson, GeneratedStyleResponse.class);

        } catch (Exception e) {
            log.error("Style Generation Failed", e);
            throw new RuntimeException("AI Style Generation Error");
        }
    }
}