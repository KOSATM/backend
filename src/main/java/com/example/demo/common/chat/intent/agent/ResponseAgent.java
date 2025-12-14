package com.example.demo.common.chat.intent.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ResponseAgent {
  private ChatClient chatClient;

  public ResponseAgent(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder.build();
  }

  public String generateMessage(String originalUserMessage, String agentMessage, Object data) {
    log.info("agentMessage: {}", agentMessage);
    if (data == null) {
      return "요청하신 정보에 해당하는 데이터가 없습니다.";
    }
    String dataStr = (data instanceof String) ? (String) data : data.toString();
    if (dataStr.isBlank()) {
      return "요청하신 정보에 해당하는 데이터가 없습니다.";
    }
    String answer = chatClient.prompt()
      .system("""
        사용자에게 받은 질문/지시의 원문과
        다른 에이전트로부터 전달 받은 메시지 및 데이터로
        자연스러운 답변을 생성하세요.

        응답 내용:
        - 여행 계획 생성에 대한 응답
          - 여행에 대한 콘셉트만 설명하세요.
          - 계획에 있는 활동을 나열하지 마세요.
          - 마크다운 형식은 사용하지 말고 반드시 순수 텍스트로 답변하세요.
          - 200자 이내로 응답을 생성하세요.

        - 장소 추천에 대한 응답
          - 장소 추천 에이전트로부터 받은 데이터를 항목과 속성별로 보기 좋게 정리하세요.

        - 계획 변경에 대한 응답(일정 추가/삭제/수정 등)
          - 변경 사항에 대해 간략히 응답하세요.

        주의: 답변에 사진은 포함하지 마세요.

        이전 에이전트가 생성한 응답 메시지: %s
        데이터: %s
      """.formatted(dataStr, agentMessage))
      .user(originalUserMessage)
      .call()
      .content();
    return answer;
  }

  public String generateMessage(String agentMessage, Object data) {
    return generateMessage("", agentMessage, data);
  }
}
