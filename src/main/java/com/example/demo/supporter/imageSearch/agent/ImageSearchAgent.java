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

  public ImageSearchAgent(ChatClient.Builder chatClientBuilder, PoiSearchAgent poiSearchAgent,
      CategorySearchAgent categorySearchAgent) {
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
    // return null;
  }

  // 1단계: 이미지 분석 (요소 추출, type 지정, confidence 평가) //요소 추출 -> type 지정
  public String analyzeImage(String placeType, String contentType, byte[] bytes) {
    // vision 분석 틀
    SystemMessage systemMessage = SystemMessage.builder()
        .text("""
            당신은 "이미지 기반 장소 추천 시스템"의 Step1을 담당하는 전용 AI입니다.

            ## 역할(핵심)
            1. 업로드된 이미지를 분석하여, 그 안에서 시각적으로 식별 가능한 요소(feature)를 추출합니다.
            2. 추출된 요소를 사용자가 지정한 type(landscape | food | activities)에 따라 **정확히 필터링**합니다.
            3. placeType과 직접적으로 연결되지 않는 요소는 어떤 경우에도 포함하면 안 됩니다.
            4. 최종 결과는 JSON 배열로만 출력합니다.

            ## 요소 분류 규칙
            각 요소는 반드시 다음 중 하나로 분류합니다:
            - type="poi": 실제 존재하는 고유명사 장소/랜드마크/특정 지점
            - type="category": 음식/음식점 종류/체험·활동·액티비티 등 카테고리성 요소

            ## type 절대 규칙 (가장 중요)
            type은 선택지가 아니라 **절대적인 필터 기준**입니다.
            아무리 이미지에서 크게 보이고 중요해도, type에 맞지 않으면 **절대 포함 금지**입니다.

            ### type = "landscape"
            - 포함 가능:
              - 실제 랜드마크, 건물, 관광지 등 POI만
            - 포함 금지:
              - 음식, 식당 카테고리, 사람, 활동/체험, 소품 등

            ### type = "food"
            - 포함 가능:
              - 실제 음식 이름 (예: 김밥, 라멘)
              - 음식점 카테고리 (예: 김밥집, 카페)
            - 포함 금지:
              - 랜드마크, 체험·활동, 건물·풍경 등의 장소

            ### type = "activities"
            장소에서 사람들이 무엇을 하고 싶은지 설명해
            - 포함 가능:
              - 실제 활동/체험 이름 (예: 암벽 등반, 서핑 수업, 한복 체험)
              - 활동 카테고리 (예: 실외 액티비티, 전통 체험)
            - 포함 금지:
              - 음식, 음식점, 랜드마크, 일반 건물·풍경

            ## 필수 조건
            - 임의 창작 금지 (허구 장소/허구 활동/허구 음식 생성 금지)
            - 요소 이름은 반드시 실제 세계에서 존재 가능한 명칭이어야 함
            - 이미지의 배경적 요소(하늘, 구름, 그림자, 사람 자체 등) 제외
            - JSON 외 다른 텍스트 출력 금지
            - 한국어로 작성

            ## 출력 형식(JSON 배열)
            [
              {
                "name": "요소명",
                "type": "poi | category",
                "visualFeatures": "이미지에서 추출된 특징 설명",
                "confidence": 0.0-1.0
              }
            ]

            ## 사용자 입력
            - type: %s

            이미지에서 type 목적에 가장 부합하는 실제 존재 가능한 요소만 분석하여 위 JSON 배열 형식으로만 출력하세요.

            """)
        .build();

    Resource resource = new ByteArrayResource(bytes);
    Media media = Media.builder()
        .mimeType(MimeType.valueOf(contentType))
        .data(resource)
        .build();

    // 사용자 입력 + 이미지
    UserMessage userMessage = UserMessage.builder()
        .text("type: " + placeType)
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
