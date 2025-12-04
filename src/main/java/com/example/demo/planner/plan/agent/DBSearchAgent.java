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

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class DBSearchAgent {
  private ChatClient chatClient;

  @Autowired
  private EmbeddingModel embeddingModel;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  public DBSearchAgent(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder.build();
  }

  public String getPlacesFromDB(String question) {
    log.info(question);
    String answer = this.chatClient.prompt()
        .system("""
            여행 코스 추천인 경우 장소 카테고리별(쇼핑/행사/카페/식당/명소/기타)로 데이터베이스를 조회하세요.
            데이터베이스 조회는 `dbSearch` 도구를 사용하세요.
            각 행의 컬럼 값을 바꾸지 말고 그대로 가져오세요.

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
    return answer;
  }

  class DBSearchTools {
    @Tool(description = "자료를 찾기 위해 DB를 조회합니다")
    public Object dbSearch(String question) {
      log.info(question);
      float[] vector = getQuestionVector(question);
      String strVector = Arrays.toString(vector).replace(" ", "");
      String sql = """
          SELECT id, content_id, title, address, tel, first_image, first_image2, lat, lng, category_code, description, tags, detail_info, normalized_category
          FROM travel_places
          ORDER BY embedding <=> ?::vector
          LIMIT 10
          """;
      List<Map<String, Object>> res = jdbcTemplate.queryForList(sql, strVector);
      log.info("res: {}", res.toString());
      return res;
    }

    private float[] getQuestionVector(String question) {
      EmbeddingResponse response = embeddingModel.embedForResponse(List.of(question));
      return response.getResult().getOutput();
    }
  }
}
