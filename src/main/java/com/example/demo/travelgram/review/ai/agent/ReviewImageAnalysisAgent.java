package com.example.demo.travelgram.review.ai.agent;

import java.time.Duration;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.content.Media;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;

import com.example.demo.travelgram.review.dto.response.PhotoAnalysisResult;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ReviewImageAnalysisAgent {

  private final ChatClient chatClient;
  private final ObjectMapper objectMapper;

  public ReviewImageAnalysisAgent(ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper) {
    this.chatClient = chatClientBuilder.build();
    this.objectMapper = objectMapper;
  }

  // ======================================================
  // 1단계: 개별 사진 요약 (Vision AI)
  // ======================================================
  public String analyzeReviewImage(String contentType, byte[] bytes) {
    log.info("📸 Review Image Analysis Start...");
    // 1. 시스템 프롬프트: 여행스타그램 리뷰어 페르소나 부여
    SystemMessage systemMessage = new SystemMessage(
        """
            당신은 여행 리뷰 생성을 위한 사진 분석 에이전트입니다.

            ### 작업 내용
            - 각 사진을 분석하여 **정확한 사실 기반 한 문장 요약**을 생성합니다.
            - 모든 사진을 종합하여 **명확히 보이는 인원 수**만을 기준으로
              여행이 **솔로인지, 동행이 있는 그룹 여행인지** 판단합니다.
            - 사진에 명확히 드러나지 않는 정보는 절대 추측하거나 가정하지 않습니다.

            ### 규칙
            1. 각 사진 요약은 반드시 **사실만 기반한 간결한 한 문장**이어야 합니다.
            2. 감정, 의도, 관계 등 **추론은 금지**합니다.
            3. 사람 수가 보이는 경우만 판단:
               - 1명 보이면 SOLO
               - 2명 이상 보이면 GROUP
               (명확히 보이는 경우에 한함)
            4. 사진 간 결과가 상충되는 경우 **다수 사진의 정보**를 기준으로 판단합니다.
            5. 결론이 불가능한 경우 `travelType` 값은 **"unclear"**로 설정합니다.
            6. 촬영자가 사진에 보이지 않는 경우 **여행 인원에 포함하여 판단하지 않습니다.**
            """);

    // 2. 미디어(이미지) 객체 생성
    Resource resource = new ByteArrayResource(bytes);
    Media media = Media.builder()
        .mimeType(MimeType.valueOf(contentType))
        .data(resource)
        .build();

    // 3. 사용자 메시지 (이미지 포함)
    UserMessage userMessage = UserMessage.builder()
        .text("Analyze this image according to the system rules.")
        .media(media)
        .build();

    // 4. LLM 호출
    try {
      String response = chatClient.prompt()
          .messages(systemMessage, userMessage)
          .call()
          .content();

      log.info("🤖 Image Analysis Result: {}", response);
      return response;
    } catch (Exception e) {
      log.error("Image Analysis Failed", e);
      return "{}"; // 실패 시 빈 JSON 반환
    }
  }

  // ======================================================
  // 2단계: 전체 여행 분석 (Text AI)
  // ======================================================
  public PhotoAnalysisResult analyzeTripContext(List<String> summaries) {
    // 리스트를 하나의 문자열로 합침
    String combinedSummaries = String.join("\n- ", summaries);

    SystemMessage systemMessage = new SystemMessage(
        """
            당신은 여행 리뷰 생성을 위한 사진 분석 에이전트입니다.

            ### 작업 내용
            - 각 사진을 분석하여 **정확한 사실 기반 한 문장 요약**을 생성합니다.
            - 모든 사진을 종합하여 **명확히 보이는 인원 수**만을 기준으로
              여행이 **솔로인지, 동행이 있는 그룹 여행인지** 판단합니다.
            - 사진에 명확히 드러나지 않는 정보는 절대 추측하거나 가정하지 않습니다.

            ### 규칙
            1. 각 사진 요약은 반드시 **사실만 기반한 간결한 한국어 한 문장**이어야 합니다.
            2. 감정, 의도, 관계 등 **추론은 금지**합니다.
            3. 사람 수가 보이는 경우만 판단:
               - 1명 보이면 SOLO
               - 2명 이상 보이면 GROUP
               (명확히 보이는 경우에 한함)
            4. 사진 간 결과가 상충되는 경우 **다수 사진의 정보**를 기준으로 판단합니다.
            5. 결론이 불가능한 경우 `travelType` 값은 **"unclear"**로 설정합니다.
            6. 촬영자가 사진에 보이지 않는 경우 **여행 인원에 포함하여 판단하지 않습니다.**
                        """);

    UserMessage userMessage = new UserMessage(
        "Here are the photo summaries:\n- " + combinedSummaries);

    try {
      // 1. LLM에게 응답 받기 (아직은 String 상태)
      String jsonResponse = chatClient.prompt()
          .messages(systemMessage, userMessage)
          .call()
          .content();

      log.info("🤖 AI Raw JSON: {}", jsonResponse);

      // 2. [중요] 마크다운 코드 블록 제거 (```json ... ```)
      // LLM이 친절하게 코드 블록을 씌워줄 때가 있는데, 파싱 에러나니 벗겨야 함
      if (jsonResponse.startsWith("```")) {
        jsonResponse = jsonResponse.replaceAll("^```json", "").replaceAll("^```", "").replaceAll("```$", "");
      }

      // 3. ObjectMapper로 String -> Object 변환 (핵심!)
      // readValue(JSON문자열, 변환할클래스.class)
      PhotoAnalysisResult result = objectMapper.readValue(jsonResponse, PhotoAnalysisResult.class);

      return result;

    } catch (Exception e) {
      log.error("Trip Context Analysis Failed", e);
      return new PhotoAnalysisResult(); // 실패 시 빈 객체 반환
    }
  }
}
