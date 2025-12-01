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

import com.example.demo.common.tools.InternetSearchTool;
import com.example.demo.supporter.imageSearch.dto.entity.PlaceCandidate;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.lang.Collections;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class ImageSearchAgent {
  /**
   * 총 4단계 수행
   * Step 1: 이미지 분석 (Vision 역할) - analyzeImage(byte[] bytes, String contentType,
   * String placeType)
   * Step 2: 후보 5개 생성 - generateCandidates(List<ImageFeature> features, String
   * placeType, double userLat, double userLng)
   * Step 3: RAG 검증 - verifyCandidatesWithRAG(List<CandidatePlace> candidates)
   * Step 4: 최종 3개 추천 - selectFinalRecommendations(List<CandidatePlace>
   * candidates)
   * main : searchImagePlace(String placeType, byte[] bytes, String contentType,
   * double userLat, double userLng)
   */
  private final ChatClient chatClient;
  private final ObjectMapper objectMapper;
  private final InternetSearchTool internetSearchTool;

  public ImageSearchAgent(ChatClient.Builder chatClientBuilder, InternetSearchTool internetSearchTool) {
    this.chatClient = chatClientBuilder.build();
    this.objectMapper = new ObjectMapper();
    this.internetSearchTool = internetSearchTool;
  }

  // 메인 함수
  public void searchImagePlace(String placeType, byte[] bytes, String contentType, double userLat, double userLng) {
    Flux<String> response = analyzeImage(placeType, contentType, bytes);
    generateCandidates(response.toString(), placeType, userLat, userLng);

  }

  // 1단계: 이미지 분석
  public Flux<String> analyzeImage(String placeType, String contentType, byte[] bytes) {
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

    log.info("1단계 AI 응답: {}", response);

    return Flux.just(response);

    // List<ImageFeature> imageFeatures = parseJsonToList(response, new
    // TypeReference<List<ImageFeature>>() {
    // });
    // log.info(imageFeatures.toString());
    // return Mono.just(imageFeatures);
  }

  // 2단계: 후보 5개 생성
  public Flux<List<PlaceCandidate>> generateCandidates(String placeFeatures, String placeType, double userLat,
      double userLng) {

    // 시스템 입력
    String systemText = """
                당신은 이미지 기반 장소 추천 전문가입니다.

        ## 역할
        1. Step1에서 추출된 요소(feature) 반드시 googleSearch 도구를 사용해 실제 존재하는 장소를 기반으로 후보 장소 5개를 추천합니다.
        2. 도구 호출은 1회가 아니라, 필요한 만큼 여러 번 호출할 수 있습니다.
        3. 도구 호출 없이 장소 이름을 직접 생성하는 것은 금지입니다.

        ## 중요 규칙
        - 도구 호출 이전에는 절대 JSON을 출력하지 마세요.
        - 도구 호출은 아래 형식만 사용하세요:
          {
            "tool": "googleSearch",
            "query": "<검색어>"
          }

        ## 검색 규칙
        - 검색어는 “요소명 + 지역명(서울 또는 사용자의 GPS 근처 지역)”으로 구성합니다.
        - POI로 판단되면 서울 중심 지역 기준으로 검색합니다.
        - Category로 판단되면 사용자 GPS 주변으로 검색합니다.

        ## 최종 출력 규칙
        - googleSearch 도구 호출 → 검색 결과 분석 후 → 최종 JSON 출력
        - 최종 JSON은 정확히 5개의 후보만 포함해야 합니다.
        - 그 외 텍스트는 절대 포함하지 마세요.

        ## 최종 출력 형식(JSON 배열)
        [
          {
            "name": "장소명",
            "type": "poi | category",
            "location": "주소 또는 지역",
            "visualFeatures": "Step1 요소와의 관계",
            "similarity": "high | medium | low",
            "confidence": 0.0~1.0
          }
        ]

        아래는 사용자 입력입니다.
        - step1Result: %s
        - placeType: %s
        - userGps: {"lat": %f, "lng": %f}

        위 정보를 사용하여 필요한 만큼 googleSearch 도구를 호출한 후,
        모든 검색 결과를 분석하여 최종 JSON 5개만 출력하세요.
                                """;

    // 사용자 입력
    String userText = String.format("""
        {
          "step1Result" : %s,
          "placeType" : %s,
          "userGps" : {"lat" : %f, "lng" : %f},
          "instruction": "반드시 googleSearch 도구를 사용해서 실제 장소만 추천하세요."
        }
          """, placeFeatures, placeType, userLat, userLng);

    String response = chatClient.prompt()
        .system(systemText)
        .user(userText)
        .tools(internetSearchTool)
        .call()
        .content();

    // String exResponse = chatClient.prompt()
    // .system("인터넷 검색도구를 활용하여 응답을 해주세요")
    // .user("오늘 삼성 주식 얼마야?")
    // .tools(internetSearchTool)
    // .call()
    // .content();

    log.info("2단계 AI 응답: {}", response);
    // log.info("2단계 AI 응답: {}", exResponse);

    /*
     * .class만 쓰면 안 되는 이유 : 리스트 타입만 알려줌
     * 내부 객체가 무엇인지는 정보가 없음 -> Jackson은 기본적으로 Map으로 넣음
     * TypeReference<List<PlaceCandidate>>() {}는 익명 서브클래스를 만들어서
     * jackson에게 런타임에도 제네릭 타입 정보를 제공
     */
    // List<PlaceCandidate> candidates = parseJsonToList(response, new
    // TypeReference<List<PlaceCandidate>>() {
    // });

    return null;
  }

  public <T> List<T> parseJsonToList(String jsonResponse, TypeReference<List<T>> typeReference) {
    try {
      return objectMapper.readValue(jsonResponse, typeReference);
    } catch (Exception e) {
      log.error("JSON 파싱 실패 : {}", jsonResponse, e);
      // 파싱 실패 시 비어 있는 리스트를 반환하여 안정성 확보
      return Collections.emptyList();
    }
  }
}
