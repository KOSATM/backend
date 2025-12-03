package com.example.demo.planner.plan.agent;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.example.demo.common.chat.intent.dto.IntentCommand;
import com.example.demo.common.chat.pipeline.AiAgentResponse;
import com.example.demo.common.global.agent.AiAgent;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class PlaceSuggestAgent implements AiAgent {
  private ChatClient chatClient;

  @Autowired
  private EmbeddingModel embeddingModel;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  public PlaceSuggestAgent(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder.build();
  }

  @Override
  public AiAgentResponse execute(IntentCommand command) {
    long start = System.nanoTime();
    log.info(command.toString());
    StringBuilder sb = new StringBuilder();
    for (Object value : command.getArguments().values()) {
      sb.append((String) value);
    }
    String question = sb.toString();
    String answer = this.chatClient.prompt()
        .system("""
            당신은 여행 전문가입니다.
            사용자의 질문에서 키워드를 추출하여 키워드에 관한 데이터베이스를 조회하고 사용자에게 여행 장소를 추천하세요.
            데이터베이스 조회는 `dbSearch` 도구를 사용하세요.
            (중요)각 행의 컬럼 값을 바꾸지 말고 그대로 가져오세요.

            출력 형식은 다음과 같아야 합니다.
            [
              {
                "title": "<방문할 장소 이름>",
                "address": "<방문할 장소의 주소>",
                "lat": <방문할 장소의 위도 (숫자)>,
                "lng": <방문할 장소의 경도 (숫자)>,
              }
            ]
            """)
        .user(question)
        .tools(new DBSearchTools())
        .call()
        .content();
    AiAgentResponse response = AiAgentResponse.builder().message(answer).build();
    long end = System.nanoTime();
    log.info("실행시간: {}", (end - start) / 1000000);
    return response;
  }

  class DBSearchTools {
    @Tool(description = "자료를 찾기 위해 DB를 조회합니다", returnDirect = true)
    public Object dbSearch(String query) {
      long start = System.nanoTime();
      log.info(query);
      float[] vector = getQueryVector(query);
      String strVector = Arrays.toString(vector).replace(" ", "");
      String sql = """
          SELECT id, content_id, title, address, tel, first_image, first_image2, lat, lng, category_code, description, tags, detail_info, normalized_category, (embedding <=> ?::vector) AS distance
          FROM travel_places
          ORDER BY embedding <=> ?::vector
          LIMIT 10
          """;
      List<Map<String, Object>> res = jdbcTemplate.queryForList(sql, strVector, strVector);
      for (Map<String, Object> map : res) {
        String title = (String) map.get("title");
        Double distance = (Double) map.get("distance");
        log.info("장소: {}, 거리: {}", title, distance);
      }
      double minDistance = (Double) res.get(0).get("distance");
      if (minDistance > 0.7) {
        return "해당 키워드와 관련된 검색 결과가 없습니다.";
      }
      // log.info("res: {}", res.toString());
      long end = System.nanoTime();
      log.info("실행시간: {}", (end - start) / 1000000);
      return res;
    }

    private float[] getQueryVector(String query) {
      long start = System.nanoTime();
      EmbeddingResponse response = embeddingModel.embedForResponse(List.of(query));
      long end = System.nanoTime();
      log.info("실행시간: {}", (end - start) / 1000000);
      return response.getResult().getOutput();
    }
  }
}
