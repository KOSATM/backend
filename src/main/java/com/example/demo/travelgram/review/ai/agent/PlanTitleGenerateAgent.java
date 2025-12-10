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
                        당신은 SEOUL ONLY 여행 서비스를 위한 ‘여행 플랜 제목 생성 에이전트’입니다.
                        감각적이고, 간결하며, 감정이 살아있는 여행 플랜 제목을 만들어주세요.

                        """)
                .user("plan_data:\n" + inputJsonString)
                .call()
                .content();
        return response;
    }
}