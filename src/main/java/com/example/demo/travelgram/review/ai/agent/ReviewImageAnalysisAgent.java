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
                You are a Photo Analysis Agent for a travel review generator.

                    Your tasks:
                    - Analyze each photo and produce a one-sentence factual summary.
                    - Evaluate all photos together to determine whether the trip appears to be a solo trip or group trip based only on visible human counts.
                    - Never guess or infer any detail that is not clearly visible.

                    RULES:
                    1. Each photo summary must be exactly one concise factual sentence.
                    2. Never infer emotions, intentions, or relationships.
                    3. You may count visible humans: one person = solo, two or more = group (only if clearly visible).
                    4. If different photos conflict, decide based on majority visible evidence.
                    5. If no conclusion is possible, travelType must be "unclear".
                    6. Do not assume the photographer is included unless visible.
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
            You are a Travel Review Analyzer.
            Based on the list of photo summaries provided below, determine the 'overallMood' and 'travelType'.

            RULES:
            1. 'travelType': Determine if it is 'SOLO' or 'GROUP'.
               - If descriptions mention multiple people or 'we', it is likely GROUP.
               - If mostly scenery or single person, it is likely SOLO.
               - If contradictory or insufficient, use 'UNCLEAR'.
            2. 'overallMood': A short phrase describing the combined atmosphere (e.g., 'Relaxing nature trip', 'Bustling city tour').
            3. Output MUST be strictly JSON format:
            {
                "overallMood": "string",
                "travelType": "SOLO | GROUP | UNCLEAR"
            }
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
