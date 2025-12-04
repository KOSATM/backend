package com.example.demo.travelgram.review.ai.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class PlanTitleGenerateAgent {
    private final ChatClient chatClient;

    public PlanTitleGenerateAgent(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public String generatePlanTitle(String inputJsonString) {
        String response = chatClient.prompt()
                .system("""
                          You are the Generate Plan Title Agent for a SEOUL-ONLY travel service.
                          Create a smart, short, emotional travel plan title.
                        """)
                .user("plan_data:\n" + inputJsonString)
                .call()
                .content();
        return response;
    }
}