package com.example.demo.planner.plan.agent.tools;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.example.demo.common.naver.dto.LocalItem;
import com.example.demo.planner.plan.service.action.PlanAddAction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 장소 검색 도구 모음
 * - SearchPlaceAgent가 사용
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SearchPlaceTools {

    private final PlanAddAction planAddAction;
    private final EmbeddingModel embeddingModel;
    private final JdbcTemplate jdbcTemplate;

    @Tool(description = "네이버 지역 검색 API로 장소를 검색합니다 (실시간 정보, 주소, 전화번호)")
    public List<LocalItem> naverLocalSearch(String searchQuery) {
        log.info("🔧 [Tool] naverLocalSearch: query={}", searchQuery);
        try {
            List<LocalItem> results = planAddAction.searchNaverLocal(searchQuery);
            log.info("✅ 네이버 검색 완료: {} 개", results.size());
            return results;
        } catch (Exception e) {
            log.error("❌ 네이버 검색 실패", e);
            return List.of();
        }
    }

    @Tool(description = "DB 벡터 검색으로 관광지 데이터를 조회합니다 (카테고리, 태그, 상세설명)")
    public List<Map<String, Object>> dbVectorSearch(String searchQuery) {
        log.info("🔧 [Tool] dbVectorSearch: query={}", searchQuery);
        try {
            float[] vector = getQuestionVector(searchQuery);
            String strVector = Arrays.toString(vector).replace(" ", "");

            String sql = """
                    SELECT id, content_id, title, address, tel,
                           first_image, first_image2, lat, lng,
                           category_code, description, tags, detail_info, normalized_category
                    FROM travel_places
                    ORDER BY embedding <=> ?::vector
                    LIMIT 10
                    """;

            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, strVector);
            log.info("✅ DB 벡터 검색 완료: {} 개", results.size());
            return results;
        } catch (Exception e) {
            log.error("❌ DB 벡터 검색 실패", e);
            return List.of();
        }
    }

    private float[] getQuestionVector(String question) {
        EmbeddingResponse response = embeddingModel.embedForResponse(List.of(question));
        return response.getResult().getOutput();
    }
}
