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

import com.example.demo.supporter.imageSearch.dto.response.PlaceCandidateResponse;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ImageSearchAgent {
  /**
   * 총 4단계 수행
   * Step 1: 이미지 분석 (Vision 역할) - analyzeImage(byte[] bytes, String contentType,
   * String placeType)
   * Step 2: 후보 5개 생성 - generateCandidates(List<ImageFeature> features, String
   * placeType, double userLat, double userLng)
   * -> Step2는 내부적으로 2-A(SearchPlan 생성) + 2-B(실제 googleSearch 기반 후보 생성)
   * Step 3: RAG 검증 - verifyCandidatesWithRAG(List<CandidatePlace> candidates)
   * Step 4: 최종 3개 추천 - selectFinalRecommendations(List<CandidatePlace>
   * candidates)
   * main : searchImagePlace(String placeType, byte[] bytes, String contentType,
   * double userLat, double userLng)
   */
  private final ChatClient chatClient;
  private final PoiSearchAgent poiSearchAgent;
  private final CategorySearchAgent categorySearchAgent;
  

  public ImageSearchAgent(ChatClient.Builder chatClientBuilder, PoiSearchAgent poiSearchAgent, CategorySearchAgent categorySearchAgent) {
    this.chatClient = chatClientBuilder.build();
    this.poiSearchAgent = poiSearchAgent;
    this.categorySearchAgent = categorySearchAgent;
  }

  // 메인 함수
  public List<PlaceCandidateResponse> searchImagePlace(String placeType, byte[] bytes, String contentType,
      String address) {

    /*
     * search test용
     * String result = internetSearchTool.googleSearch("경복궁 서울");
     * log.info(result);
     */
    /*
     * test 용
     * String response = """
     * [
     * {
     * \"address\": \"서울 종로구 사직로 161\",
     * \"name\": \"경복궁\",
     * \"type\": \"poi\",
     * \"location\": \"서울 종로구\",
     * \"visualFeatures\": \"조선 시대의 궁궐, 아름다운 건축물\",
     * \"similarity\": 0.95,
     * \"confidence\": 0.9
     * }
     * ]""";
     * List<PlaceCandidate> candidates = parseJsonToList(response, new
     * TypeReference<List<PlaceCandidate>>() {
     * });
     * log.info(candidates.toString());
     */
    String response = analyzeImage(placeType, contentType, bytes);
    List<PlaceCandidateResponse> candidates = generateCandidates(response.toString(), placeType, address);
    return candidates;
  }

  // 1단계: 이미지 분석 (요소 추출, type 지정, confidence 평가)
  public String analyzeImage(String placeType, String contentType, byte[] bytes) {
    // vision 분석 틀
    SystemMessage systemMessage = SystemMessage.builder()
        .text("""
            당신은 이미지 기반 장소 추천 전용 AI입니다.

            ## 역할
            1. 사용자가 업로드한 이미지를 분석하여 **핵심 요소(feature)**만 추출합니다.
            2. 각 요소가 실제 존재하는 장소, landmark, 체험/활동, 또는 명확한 POI인지 판단합니다.
              - landmark / specific place → type="poi"
              - 음식점, 체험, 활동, 풍경 등 장소와 관련된 카테고리 → type="category"
            3. 각 요소에 대해 confidence(0~1)를 평가합니다.

            ## 필수 조건
            - 핵심 요소만 포함합니다. (배경 요소나 지나가는 사람, 구름, 나무 한 그루 등은 제외)
            - 요소명은 반드시 **실제 존재 가능성이 있는 고유 명칭**이나 **실제 체험/활동 이름**이어야 합니다.
              - 예시:
                - O: "경복궁", "남산타워", "한복 체험관", "암벽 등반 체험", "골든 브릿지"
                - X: "클라이밍 벽", "대회 로고", "인공 암벽 시설", "구름", "풀 한 그루"
            - POI(type="poi")는 실제 존재 가능한 고유 landmark 또는 장소만 포함합니다.
              **지역 제한은 두지 않으며, 서울 외 landmark도 추출합니다.**
            - type 필드에는 **“poi” 또는 “category”만 사용**합니다. placeType 값은 절대 사용하지 않습니다.
            - JSON 배열 외 다른 텍스트는 절대 출력하지 않습니다.
            - 한국어로 응답합니다.
            - 절대로 임의로 요소명을 만들어 넣지 마세요. 반드시 실제 존재 가능성이 있는 이름이어야 합니다.

            ## 출력 형식(JSON 배열)
            [
              {
                "name": "요소명",
                "type": "poi | category",
                "visualFeatures": "이미지에서 추출된 특징 요약",
                "confidence": 0.0-1.0
              }
            ]

            아래는 사용자 입력입니다.
            - placeType: %s
            - address: "%s"

            이미지에서 실제 존재 가능한 핵심 요소만 분석하여 JSON 배열로 출력하세요.
            서울 외 landmark는 Step1에서 추출하되, Step2에서 후보를 찾는 것은 서울 내로 제한됩니다.
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

    return response;
  }

  // 2단계: 후보 최대 3개 생성 (라우팅)
  public List<PlaceCandidateResponse> generateCandidates(String llmResponse, String placeType, String address) {
    // poi인지 category인지 판단

    List<PlaceCandidateResponse> candidates;

    if (llmResponse.indexOf("poi") != -1) {
      candidates = poiSearchAgent.generatePoiCandidates(llmResponse, placeType, address);
    } else {
      // category인 경우
      candidates = categorySearchAgent.generateCategoryCandidates(llmResponse, placeType, address);
    }
    return candidates;
  }

}
