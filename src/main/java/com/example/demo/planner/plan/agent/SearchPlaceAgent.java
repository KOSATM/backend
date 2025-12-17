package com.example.demo.planner.plan.agent;

import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import com.example.demo.common.naver.dto.LocalItem;
import com.example.demo.planner.plan.agent.tools.SearchPlaceTools;

import lombok.extern.slf4j.Slf4j;

/**
 * 장소 검색 전문 에이전트
 * - Tool-Calling 방식으로 네이버/DB 검색 수행
 * - SmartPlanAgent → SearchPlaceAgent → SearchPlaceTools
 */
@Component
@Slf4j
public class SearchPlaceAgent {

    private final ChatClient chatClient;
    private final SearchPlaceTools searchPlaceTools;

    public SearchPlaceAgent(
            ChatClient.Builder builder,
            SearchPlaceTools searchPlaceTools) {
        this.chatClient = builder.build();
        this.searchPlaceTools = searchPlaceTools;
    }

    /**
     * ✅ SmartPlanAgent에서 호출되는 @Tool 메서드
     * 장소 검색 (네이버 Local API + DB Vector Search)
     * @param searchQuery 검색어
     * @return 검색 결과 설명 + 후보 리스트
     */
    @Tool(description = "네이버와 DB에서 장소를 검색하여 여러 후보를 추천합니다 (추천/교체/추가 시 사용)")
    public String searchPlaces(String searchQuery) {
        log.info("🔍 [SearchPlaceAgent @Tool] 장소 검색: query={}", searchQuery);

        String systemPrompt = """
                당신은 여행 장소 검색 전문가입니다.

                ## 사용 가능한 도구:
                1. naverLocalSearch: 네이버 지역 검색 API (실시간 정보, 주소/전화번호)
                2. dbVectorSearch: DB 벡터 검색 (관광지 데이터, 카테고리/태그/상세설명)

                ## 검색 전략:
                - "맛집", "카페", "식당" → naverLocalSearch 우선
                - "관광지", "명소", "박물관" → dbVectorSearch 우선
                - 둘 다 사용하여 결과 보완

                ## 응답 형식:
                - 검색 결과를 자연스러운 한국어로 정리
                - 최대 5개의 후보 제시
                - 각 장소: 이름, 주소, 카테고리, 특징
                - JSON이나 배열 기호는 사용하지 말 것

                예시:
                "🔍 '강남역 맛집' 검색 결과 3개를 찾았습니다:

                1. **대왕족발 강남점**
                   - 카테고리: 한식 > 족발
                   - 주소: 서울 강남구 강남대로 지하 396
                   - 특징: 족발과 보쌈 전문, 푸짐한 양"
                """;

        String result = chatClient.prompt()
                .system(systemPrompt)
                .user("'%s' 검색을 수행하고 결과를 정리해주세요.".formatted(searchQuery))
                .tools(searchPlaceTools)
                .toolContext(Map.of("searchQuery", searchQuery))
                .call()
                .content();

        log.info("🔍 [SearchPlaceAgent @Tool] 검색 완료");
        return result;
    }

    /**
     * 단순 네이버 검색 (Tool 없이 직접 호출)
     * @param searchQuery 검색어
     * @return 검색 결과 리스트
     */
    public List<LocalItem> directNaverSearch(String searchQuery) {
        return searchPlaceTools.naverLocalSearch(searchQuery);
    }
}
