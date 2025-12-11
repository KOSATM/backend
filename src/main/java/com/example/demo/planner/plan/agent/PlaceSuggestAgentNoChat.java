package com.example.demo.planner.plan.agent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.example.demo.common.chat.intent.dto.IntentCommand;
import com.example.demo.common.chat.pipeline.AiAgentResponse;
import com.example.demo.common.global.agent.AiAgent;
import com.example.demo.planner.plan.dto.entity.TravelPlaces;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class PlaceSuggestAgentNoChat {
  private ChatClient chatClient;

  @Autowired
  private EmbeddingModel embeddingModel;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  public PlaceSuggestAgentNoChat(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder.build();
  }

  public List<TravelPlaces> execute(IntentCommand command, Long userId) {
    long start = System.nanoTime();
    log.info(command.toString());
    ObjectMapper objectMapper = new ObjectMapper();

    BeanOutputConverter<List<TravelPlaces>> beanOutputConverter = new BeanOutputConverter<>(
        new ParameterizedTypeReference<List<TravelPlaces>>() {
        });

    StringBuilder sb = new StringBuilder();
    for (Object value : command.getArguments().values()) {
      sb.append(value.toString());
    }
    String question = sb.toString();

    String answer = this.chatClient.prompt()
        .system("""
            당신은 여행 전문가입니다.
            장소에서 위도와 경도를 추출하여 데이터베이스를 조회하고 사용자에게 여행 장소를 추천하세요.
            데이터베이스 조회는 `dbSearch` 도구를 사용하세요.
            """)
        .user(question)
        .tools(new DBSearchTools())
        .call()
        .content();

    // String response = AiAgentResponse.builder().message(answer).build();
    log.info("answer: {}", answer);
    long end = System.nanoTime();
    log.info("실행시간: {}ms", (end - start) / 1_000_000);
    log.info("converting...");
    List<TravelPlaces> suggestList = beanOutputConverter.convert(answer);
    log.info("suggestList: {}", suggestList.toString());
    return suggestList;
  }

  class DBSearchTools {
    @Tool(description = "자료를 찾기 위해 DB를 조회합니다", returnDirect = true)
    public Object dbSearch(@ToolParam(description = "위도")Double lat, @ToolParam(description = "경도")Double lng) {
      long start = System.nanoTime();
      log.info("위도: {}, 경도: {}", lat, lng);

      try {
        String sql = """
            SELECT id, content_id, title, address, tel, first_image AS firstImage, lat, lng,
                   description, normalized_category,
                    (6371 * acos(
                        cos(radians(?)) * cos(radians(lat)) *
                        cos(radians(lng) - radians(?)) +
                        sin(radians(?)) * sin(radians(lat))
                    )) AS distance
            FROM travel_places
            ORDER BY distance
            LIMIT 10
            """;

        List<Map<String, Object>> res = jdbcTemplate.queryForList(sql, lat, lng, lat);
        log.info("res: {}", res.toString());

        List<TravelPlaces> travelPlacesList = new ArrayList<>();
        // for (Map<String, Object> map : res) {
        //   TravelPlaces travelPlaces = new TravelPlaces();
        //   travelPlaces.
        // }

        // 검색 결과 없음
        if (res.isEmpty()) {
          return "해당 위치에 있는 장소가 없습니다.";
        }

        // 첫 번째 결과의 거리 확인
        // double minDistance = ((Number) res.get(0).get("distance")).doubleValue();
        // log.info("최소 거리: {}", minDistance);

        // if (minDistance > 0.7) {
        //   log.warn("거리 임계값 초과: {}", minDistance);
        //   return "해당 키워드와 관련된 검색 결과가 없습니다.";
        // }

        // 결과 로깅
        res.forEach(map -> {
          String title = (String) map.get("title");
          double distance = ((Number) map.get("distance")).doubleValue();
          log.info("장소: {}, 거리: {}", title, distance);
        });

        long end = System.nanoTime();
        log.info("DB 검색 실행시간: {}ms", (end - start) / 1_000_000);

        return res;

      } catch (Exception e) {
        log.error("DB 검색 실패: {}", e.getMessage(), e);
        return "검색 중 오류가 발생했습니다.";
      }
    }

    private float[] getQueryVector(String query) {
      long start = System.nanoTime();
      try {
        EmbeddingResponse response = embeddingModel.embedForResponse(List.of(query));
        float[] vector = response.getResult().getOutput();
        long end = System.nanoTime();
        log.info("임베딩 실행시간: {}ms", (end - start) / 1_000_000);
        return vector;
      } catch (Exception e) {
        log.error("임베딩 생성 실패: {}", e.getMessage(), e);
        throw new RuntimeException("임베딩 생성 실패", e);
      }
    }
  }
}