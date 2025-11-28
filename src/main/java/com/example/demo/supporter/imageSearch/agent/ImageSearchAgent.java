package com.example.demo.supporter.imageSearch.agent;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;

import com.example.demo.supporter.imageSearch.dto.entity.ImageFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.lang.Collections;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class ImageSearchAgent {
  /**
   * 총 4단계 수행
   * Step 1: 이미지 분석 (Vision 역할) - analyzeImage(byte[] bytes, String contentType, String placeType)
   * Step 2: 후보 5개 생성 - generateCandidates(List<ImageFeature> features, String placeType, double userLat, double userLng)
   * Step 3: RAG 검증 - verifyCandidatesWithRAG(List<CandidatePlace> candidates)
   * Step 4: 최종 3개 추천 - selectFinalRecommendations(List<CandidatePlace> candidates)
   * main : searchImagePlace(String placeType, byte[] bytes, String contentType, double userLat, double userLng)
   */
  private final ChatClient chatClient;
  private final ObjectMapper objectMapper;

  public ImageSearchAgent(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder.build();
    this.objectMapper = new ObjectMapper();
  }

  //메인 함수
  public void searchImagePlace(String placeType, byte[] bytes, String contentType, double userLat, double userLng) {

  }


  //1단계: 이미지 분석
  public Mono<List<ImageFeature>> analyzeImage(String placeType, String contentType, byte[] bytes) {
    // vision 분석 틀
    SystemMessage systemMessage = SystemMessage.builder()
        .text("""
            당신은 이미지 기반 장소 추천 전용 AI입니다.

            역할:
            1. 사용자가 제공한 placeType(places, restaurants, activities)에 맞춰 이미지 안 요소를 추출
            2. POI(landmark, specific place) 또는 Category(food, activity 등)를 구분
            3. 각 요소에 대한 confidence(0~1) 평가

            출력 형식(JSON 배열):
            [
              {
                "name": "요소명",
                "type": "poi | category",
                "visualFeatures": "이미지에서 추출된 특징 요약",
                "confidence": 0.0-1.0
              }
            ]

            제약사항:
            - 핵심 요소 위주로 반환
            - JSON 배열 외 다른 텍스트 없음
            - 한국어로 대답해줘
                              """)
        .build();

    Resource resource = new ByteArrayResource(bytes);
    Media media = Media.builder()
        .mimeType(MimeType.valueOf(contentType))
        .data(resource)
        .build();

    // 사용자 입력 + 이미지
    UserMessage userMessage = UserMessage.builder()
        .text("사용자 질문 : {question}, placeType: {placeType}")
        .media(media)
        .build();

    String response = chatClient.prompt()
        .messages(systemMessage, userMessage)
        .call()
        .content();

    // log.info("AI 응답: {}", response);

    List<ImageFeature> imageFeatures = parseJsonToList(response, new TypeReference<List<ImageFeature>>() {});
    log.info(imageFeatures.toString());
    return Mono.just(imageFeatures);
  }

  public <T> List<T> parseJsonToList(String jsonResponse, TypeReference<List<T>> typeReference) {
    try {
      return objectMapper.readValue(jsonResponse, typeReference);
    } catch (Exception e) {
      log.error("JSON 파싱 실패 : {}", jsonResponse, e);
      //파싱 실패 시 비어 있는 리스트를 반환하여 안정성 확보
      return Collections.emptyList();
    }
  }
}
