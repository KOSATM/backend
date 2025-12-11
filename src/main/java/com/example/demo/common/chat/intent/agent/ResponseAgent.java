package com.example.demo.common.chat.intent.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class ResponseAgent {
  private ChatClient chatClient;

  public ResponseAgent(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder.build();
  }

  public String generateMessage(Object data) {
    String answer = chatClient.prompt()
      .system("""
        다른 에이전트로부터 전달 받은 데이터를 바탕으로
        자연스러운 답변을 생성하세요.

        데이터: %s
      """.formatted(data.toString()))
      .call()
      .content();

    return answer;
  }
}
