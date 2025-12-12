package com.example.demo.common.chat.intent.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class ResponseAgent {
  private ChatClient chatClient;

  public ResponseAgent(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder.build();
  }

  public String generateMessage(String originalUserMessage, Object data) {
    String answer = chatClient.prompt()
      .system("""
        사용자에게 받은 질문/지시의 원문과
        다른 에이전트로부터 전달 받은 데이터로
        자연스러운 답변을 생성하세요.

        데이터: %s
      """.formatted(data.toString()))
      .user(originalUserMessage)
      .call()
      .content();

    return answer;
  }
}
