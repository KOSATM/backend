package com.example.demo.travelgram.review.ai.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import com.example.demo.common.tools.InternetSearchTool;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class PlanTitleGenerateAgent {
    private ChatClient chatClient;

  public PlanTitleGenerateAgent(
      ChatClient.Builder chatClientBuilder) {
    chatClient = chatClientBuilder.build();
  }

  public String generatePlanTitle(String question) {
    String response = chatClient.prompt()
        .system(
    """
    You are the Generate Plan Title Agent for a SEOUL-ONLY travel service.
    """)
        .call()
        .content();
    return response;
  }
}