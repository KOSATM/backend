package com.example.demo.travelgram.review.ai.agent;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
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
              ⭐ 모든 출력값(사진 요약문 포함)은 반드시 한국어로 작성합니다.
              ⭐ 단, travelType은 예외적으로 영어 값(SOLO, GROUP, UNCLEAR)으로만 출력합니다.

              당신은 여행 리뷰 생성을 위한 사진 분석 에이전트입니다.

              ### 작업 내용
              - 각 사진을 분석하여 정확한 사실 기반 한국어 한 문장 요약을 생성합니다.
              - 모든 사진을 종합해, 명확히 보이는 사람 수를 기준으로 travelType을 결정합니다.
              - 사진에 명확히 드러나지 않는 정보는 절대 추측하지 않습니다.

              ### 규칙
              1. 사진 요약은 반드시 한국어 한 문장.
              2. 감정·의도·관계 추론 금지.
              3. 사람 수 기준:
                 - 1명 보이면 travelType = SOLO
                 - 2명 이상 보이면 travelType = GROUP
              4. 판단 불가 시 travelType = UNCLEAR

              {
              "photoSummaries": [
                "한 명의 여성이 강가를 따라 걸어가는 모습이 보입니다.",
                "사람이 보이지 않는 자연 풍경 사진입니다."
              ],
              "travelType": "SOLO"
            }
                          """);

    // 2. 미디어(이미지) 객체 생성
    Resource resource = new ByteArrayResource(bytes);
    Media media = Media.builder()
        .mimeType(MimeType.valueOf(contentType))
        .data(resource)
        .build();

    // 3. 사용자 메시지 (이미지 포함)
    UserMessage userMessage = UserMessage.builder()
        .text("⭐ 모든 출력은 한국어로 작성하며, travelType만 영어 (SOLO, GROUP, UNCLEAR)로 출력합니다.\\n")
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
            당신은 Travel Review Analyzer입니다.

            아래에 제공되는 사진 요약 리스트를 기반으로 'overallMood'와 'travelType'을 결정해야 합니다.

            ⭐ 언어 규칙
            - 설명 및 분위기 표현(overallMood)은 반드시 한국어로 작성합니다.
            - travelType은 영어 (SOLO, GROUP, UNCLEAR) 값으로만 출력합니다.

            ## 판단 규칙
            1. travelType 결정
               - 여러 사람이 언급되거나 복수 표현(예: "둘", "함께")이 반복되면 GROUP.
               - 대부분 풍경이거나 한 명만 언급되면 SOLO.
               - 정보가 모호하거나 서로 충돌하면 UNCLEAR.

            2. overallMood 결정
               - 전체 사진이 주는 분위기를 한국어 한 문장으로 표현합니다.
               - 예: "조용하고 여유로운 자연 감성", "활기 넘치는 도시 산책 분위기"

            3. 출력 형식(반드시 STRICT JSON)
            {
              "overallMood": "string",
              "travelType": "SOLO | GROUP | UNCLEAR"
            }

            출력 시 한국어 설명과 영어 ENUM 규칙을 반드시 지키세요.
            """);

    UserMessage userMessage = new UserMessage(
        "사진 요약 모음입니다. :\n- " + combinedSummaries);

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
      log.error("여행 상황 분석이 실패했습니다.", e);
      return new PhotoAnalysisResult(); // 실패 시 빈 객체 반환
    }
  }
}
