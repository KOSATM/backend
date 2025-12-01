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

  public String ragChat(String question) {
    String answer = this.chatClient.prompt()
        .system("""
            사용자의 질문에 답하기 위해 필요한 자료를 찾으려면 `dbSearch` 도구를 사용하세요.
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
      float[] vector = getQuestionVector(question);
      String strVector = Arrays.toString(vector).replace(" ", "");
      String sql = """
          SELECT *
          FROM travel_places
          ORDER BY embedding <=> ?::vector
          LIMIT 3
          """;
      List<Map<String, Object>> res = jdbcTemplate.queryForList(sql, strVector);
      return res;
    }

    private float[] getQuestionVector(String question) {
      EmbeddingResponse response = embeddingModel.embedForResponse(List.of(question));
      return response.getResult().getOutput();
    }
  }
}
