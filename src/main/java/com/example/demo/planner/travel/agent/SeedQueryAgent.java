package com.example.demo.planner.travel.agent;

import java.util.List;
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

        String systemPrompt = """
                당신은 여행지 추천을 위한 "Seed Query 생성 에이전트"입니다.

                당신의 목적은 사용자가 제공한 위치(location) 정보를 기반으로,
                벡터 검색(semantic search)에 최적화된 자연어 문장을 생성하는 것입니다.

                이 문장은 Embedding 모델이 가장 잘 이해할 수 있는 형태여야 하며,
                사용자의 의도를 정확하게 반영해야 합니다.

                ---

                # ✔ 입력 정보
                - mode: "default" 또는 "category"
                - locations: 문자열 배열
                - category: (category 모드일 때만 사용)

                ---

                # ✔ 공통 출력 규칙
                1. 자연스러운 한 문장만 생성합니다.
                2. 장소명은 반드시 원본 그대로 포함합니다.
                3. 여러 장소는 자연스럽게 연결합니다.
                4. 제공된 정보 외의 새로운 장소 생성 금지.
                5. 설명 없이 문장 하나만 출력합니다.

                ---

                # ✔ mode = "default"
                여행지 추천/검색 목적에 맞는 일반 문장을 생성합니다.
                예: "강남역과 서초 일대에서 가볼 만한 장소를 추천해줘."

                ---

                # ✔ mode = "category"
                특정 카테고리를 보강하기 위한 문장을 생성합니다.
                예: "강남역과 서초 일대에서 갈 만한 맛집을 추천해줘."

                카테고리 문구 예:
                - SPOT → 관광지, 명소, 볼거리
                - FOOD → 맛집, 식당
                - CAFE → 카페, 커피, 디저트
                - SHOPPING → 쇼핑할 곳, 상점가
                - EVENT → 행사, 축제
                - STAY → 숙박, 머물 곳

                ---

                이 규칙을 모두 지켜 Seed Query 문장을 생성하십시오.
                                """;
        String userPrompt = buildUserPrompt(seedQueryArgs);

        String response = chatClient.prompt().system(systemPrompt)
                .user(userPrompt)
                .options(ChatOptions.builder().temperature(0.1).build()).call().content();

        log.info(response);
        return response;
    }

        private String buildUserPrompt(Map<String, Object> seedQueryArgs) {

        boolean isCategoryMode = seedQueryArgs.containsKey("category");
        String location = (String) seedQueryArgs.getOrDefault("location", "서울");
        
        if (isCategoryMode) {
            String category = (String) seedQueryArgs.get("category");
            
            return """
                    다음 정보를 기반으로 카테고리 보강용 Seed Query 문장을 하나 생성해줘.
                    
                    - mode: category
                    - locations: %s
                    - category: %s
                    
                    문장은 반드시 하나만 출력해줘.
                    """.formatted(location, category);
        }

        // default 모드
        return """
                다음 정보를 기반으로 Seed Query 문장을 하나 생성해줘.
                
                - mode: default
                - locations: %s
                
                문장은 반드시 하나만 출력해줘.
                """.formatted(location);
    }
}
