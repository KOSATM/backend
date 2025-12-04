package com.example.demo.supporter.imageSearch.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Collections;

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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
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
   * Step 3: RAG 검증 - verifyCandidatesWithRAG(List<CandidatePlace> candidates)
   * Step 4: 최종 3개 추천 - selectFinalRecommendations(List<CandidatePlace>
   * candidates)
   * main : searchImagePlace(String placeType, byte[] bytes, String contentType,
   * double userLat, double userLng)
   */
  private final ChatClient chatClient;
  private final ObjectMapper objectMapper;
  private final InternetSearchTool internetSearchTool;
  private final Validator validator;

  public ImageSearchAgent(ChatClient.Builder chatClientBuilder, InternetSearchTool internetSearchTool,
      Validator validator) {
    this.chatClient = chatClientBuilder.build();
    this.objectMapper = new ObjectMapper();
    this.internetSearchTool = internetSearchTool;
    this.validator = validator;
  }

  // 메인 함수
  public void searchImagePlace(String placeType, byte[] bytes, String contentType, String address) {

    // String result = internetSearchTool.googleSearch("경복궁 서울");
    // log.info(result);
    // String response = analyzeImage(placeType, contentType, bytes);
    // generateCandidates(response.toString(), placeType, address);
    String response = """
        [
          {
            \"address\": \"서울 종로구 사직로 161\",
            \"name\": \"경복궁\",
            \"type\": \"poi\",
            \"location\": \"서울 종로구\",
            \"visualFeatures\": \"조선 시대의 궁궐, 아름다운 건축물\",
            \"similarity\": 0.95,
            \"confidence\": 0.9
          }
        ]""";
    List<PlaceCandidate> candidates = parseJsonToList(response, new TypeReference<List<PlaceCandidate>>() {
    });
    log.info(candidates.toString());
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

    // List<ImageFeature> imageFeatures = parseJsonToList(response, new
    // TypeReference<List<ImageFeature>>() {
    // });
    // log.info(imageFeatures.toString());
    // return Mono.just(imageFeatures);
  }

  // 2단계: 후보 5개 생성
  public List<PlaceCandidate> generateCandidates(String placeFeatures, String placeType, String address) {

    // 시스템 입력
    String systemText = """
        당신은 이미지 기반 장소 추천 전문가입니다.

        ## 역할
        - Step1 features를 기반으로 googleSearch 도구를 사용해 실제 존재하는 장소만 후보로 생성합니다.
        - 장소명(name)은 반드시 googleSearch 결과에서만 가져옵니다.
        - 임의 장소명, 임의 주소, 임의 location 생성은 절대 금지합니다.
        - feature 1개당 1개의 후보만 생성하며, 최종 5개까지만 반환합니다.

        ## 검색 규칙
        - POI(type="poi"):
          - 1차 검색어: "<요소명> 서울"
          - 검색 결과 없으면 visualFeatures 핵심 단어로 1회 추가 검색
          - 총 검색 횟수는 feature당 최대 2회

        - Category(type="category"):
          - 검색어: "<사용자 주소 지역명> <요소명>"
          - 검색 결과 없으면 해당 feature는 후보 제외

        ## 후보 생성 규칙
        - googleSearch 결과에서 name과 address가 모두 존재하는 첫 번째 유효 장소만 후보로 사용합니다.
        - “검색어 그대로” name에 넣거나, result.title/result.snippet을 조합해 새 이름을 만드는 행위 금지.
        - 검색어와 후보 name이 동일하면 → **검색 실패로 간주**하고 그 feature는 제외합니다.
        - Step1의 type(poi/category)은 그대로 유지하며 재분류 금지.
        - 이미 선택된 장소는 중복 후보로 사용 불가.

        ## 대체 규칙 (Fallback Rule)
        - 만약 googleSearch 도구 호출 결과가 "인터넷 검색 중 오류 발생"으로 시작하면 인터넷 검색이 불가능한 상태이므로,
        - `visualFeatures` 필드에 "인터넷 검색 실패" 라는 문구만 넣고, 호출을 멈추세요

        ## 출력 규칙
        - similarity: Step1 feature와 장소의 연관성을 기준으로 high/medium/low 중 선택
        - confidence: 0.0~1.0 (googleSearch 결과 품질 기반 추정)
        - 최종 결과는 JSON 배열만 출력하며, 설명 문구는 절대 포함 금지

        ## 출력 형식(JSON)
        [
          {
            "address": "주소",
            "name": "장소명",
            "type": "poi | category",
            "location": "주소 또는 지역",
            "visualFeatures": "Step1 요소와의 관계",
            "similarity": "high | medium | low",
            "confidence": 0.0~1.0
          }
        ]

        ## 사용자 입력
        - step1Result: %s
        - placeType: %s
        - address: "%s"

        위 정보를 바탕으로 필요한 googleSearch 도구 호출을 수행하고,
        조건을 충족하는 실제 장소만 JSON 배열로 출력하세요.
                                        """;

    // 사용자 입력
    String userText = String.format("""
        {
          "step1Result" : %s,
          "placeType" : %s,
          "address" : "%s",
          "instruction": "반드시 googleSearch 도구를 사용해서 실제 장소만 추천하세요."
        }
          """, placeFeatures, placeType, address);

    String response = chatClient.prompt()
        .system(systemText)
        .user(userText)
        .tools(internetSearchTool)
        .call()
        .content();

    log.info("2단계 AI 응답: {}", response);
    // 2단계를 어떤 형식으로 보내줄 건지 생각

    /*
     * .class만 쓰면 안 되는 이유 : 리스트 타입만 알려줌
     * 내부 객체가 무엇인지는 정보가 없음 -> Jackson은 기본적으로 Map으로 넣음
     * TypeReference<List<PlaceCandidate>>() {}는 익명 서브클래스를 만들어서
     * jackson에게 런타임에도 제네릭 타입 정보를 제공
     */

    List<PlaceCandidate> candidates = parseJsonToList(response, new TypeReference<List<PlaceCandidate>>() {
    });
    log.info(candidates.toString());

    return null;
  }

  // 3단계: RAG 검증 (poi만 해당)
  public String verifyCandidatesWithRAG(String response) {
    return null;
  }

  public <T> List<T> parseJsonToList(String jsonResponse, TypeReference<List<T>> typeReference) {

    // 빈 값 처리
    if (jsonResponse == null || jsonResponse.trim().isEmpty() || jsonResponse.trim().equals("[]")) {
      log.warn("빈 JSON 응답 받음");
      return Collections.emptyList();
    }

    try {
      JsonNode rootNode = objectMapper.readTree(jsonResponse);

      // 배열인지 확인
      if (!rootNode.isArray()) {
        log.warn("예상된 JSON 배열 형식이 아닙니다: {}", jsonResponse);
        return Collections.emptyList();
      }

      // 1) JSON 전체를 List<T>로 변환
      List<T> list = objectMapper.convertValue(rootNode, typeReference);

      // 2) bean validation
      List<T> validList = new ArrayList<>();

      for (T item : list) {
        Set<ConstraintViolation<T>> violations = validator.validate(item);

        if (violations.isEmpty()) {
          validList.add(item);
        } else {
          log.warn("DTO 유효성 검증 실패. 객체: {}, 위반: {}", item, violations);
        }
      }

      return validList;

    } catch (Exception e) {
      log.error("JSON 파싱 실패: {}", jsonResponse, e);
      return Collections.emptyList();
    }
  }
}
