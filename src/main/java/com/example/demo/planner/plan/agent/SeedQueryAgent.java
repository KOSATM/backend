package com.example.demo.planner.plan.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SeedQueryAgent {

    private final ChatClient chatClient;

    public SeedQueryAgent(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    // ====================================================================================
    //  Multi SeedQuery 생성 (여러 목적을 반영하여 검색 풍부화)
    // ====================================================================================
    public List<String> generateMultiSeedQueries(Map<String, Object> args) {
        List<String> queries = new ArrayList<>();

        // 1) 기본 SeedQuery
        queries.add(generateSeedQuery(args));

        String location = (String) args.getOrDefault("location", "서울");
        String theme = (String) args.getOrDefault("theme", "");
        String companion = (String) args.getOrDefault("companion", "");
        List<String> mustPlaces = (List<String>) args.get("mustPlace");

        // 2) 테마 기반 쿼리
        if (theme != null && !theme.isBlank()) {
            queries.add("%s에서 %s와 관련된 여행지를 추천해줘."
                    .formatted(location, theme));
        }

        // 3) 동반자 기반 쿼리
        if (companion != null && !companion.isBlank()) {
            queries.add("%s에서 %s과(와) 여행하기 좋은 장소를 추천해줘."
                    .formatted(location, companion));
        }

        // 4) mustPlace 기반 쿼리
        if (mustPlaces != null && !mustPlaces.isEmpty()) {
            for (String p : mustPlaces) {
                queries.add("'%s' 주변에서 가볼 만한 여행지를 추천해줘."
                        .formatted(p));
            }
        }

        // 5) 카테고리 보강 — SPOT & FOOD 기본 제공
        queries.add(generateCategoryQuery(location, "SPOT"));
        queries.add(generateCategoryQuery(location, "FOOD"));

        // 중복 제거
        queries = queries.stream().distinct().toList();

        log.info("== Multi SeedQuery ==");
        queries.forEach(q -> log.info(" * {}", q));

        return queries;
    }

    private String generateCategoryQuery(String location, String category) {

        return switch (category) {
            case "SPOT" -> "%s에서 관광 명소나 볼거리를 추천해줘.".formatted(location);
            case "FOOD" -> "%s에서 갈 만한 맛집을 추천해줘.".formatted(location);
            case "CAFE" -> "%s에서 카페나 디저트를 즐길 곳을 추천해줘.".formatted(location);
            case "SHOPPING" -> "%s에서 쇼핑하기 좋은 곳을 추천해줘.".formatted(location);
            case "EVENT" -> "%s에서 열리는 행사나 축제를 추천해줘.".formatted(location);
            default -> "%s에서 방문할 만한 장소를 추천해줘.".formatted(location);
        };
    }

    // 기존 함수 (카테고리 보강 시 사용됨)
    public String generateSeedQuery(Map<String, Object> seedQueryArgs) {

        String systemPrompt = """
                당신은 여행지 추천을 위한 "Seed Query 생성 에이전트"입니다.
                목적: 벡터 검색에 최적화된 자연어 문장을 생성하는 것입니다.
                오직 한 문장만 출력하십시오.
                """;

        String userPrompt = buildUserPrompt(seedQueryArgs);

        String response = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .options(ChatOptions.builder().temperature(0.1).build())
                .call()
                .content()
                .trim();

        log.info("[SeedQuery] {}", response);

        return response;
    }

    private String buildUserPrompt(Map<String, Object> seedQueryArgs) {

        boolean isCategoryMode = seedQueryArgs.containsKey("category");
        String location = (String) seedQueryArgs.getOrDefault("location", "서울");
        String theme = (String) seedQueryArgs.getOrDefault("theme", "");

        if (isCategoryMode) {
            String category = (String) seedQueryArgs.get("category");

            return """
                    다음 정보를 기반으로 카테고리 보강용 Seed Query 문장을 하나 생성해줘.

                    - mode: category
                    - locations: %s
                    - category: %s

                    반드시 한 문장만 출력해줘.
                    """.formatted(location, category);
        }

        // default 모드
        return """
                다음 정보를 기반으로 Seed Query 한 문장을 생성해줘.

                - mode: default
                - locations: %s
                - theme: %s

                반드시 한 문장만 출력해줘.
                """.formatted(location, theme);
    }
}
