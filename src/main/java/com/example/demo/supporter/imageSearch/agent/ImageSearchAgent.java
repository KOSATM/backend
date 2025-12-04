package com.example.demo.supporter.imageSearch.agent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;

import com.example.demo.common.tools.InternetSearchTool;
import com.example.demo.supporter.imageSearch.dto.response.PlaceCandidateResponse;
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
  public List<PlaceCandidateResponse> searchImagePlace(String placeType, byte[] bytes, String contentType, String address) {

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

  // 2단계: 후보 5개 생성
  public List<PlaceCandidateResponse> generateCandidates(String llmResponse, String placeType, String address) {
    // poi인지 category인지 판단

    List<PlaceCandidateResponse> candidates;

    if (llmResponse.indexOf("poi") != -1) {
      candidates = generatePoiCandidates(llmResponse, placeType, address);
    } else {
      // category인 경우
      candidates = generateCategoryCandidates(llmResponse, placeType, address);
    }
    return candidates;
  }

  public List<PlaceCandidateResponse> generatePoiCandidates(String llmResponse, String placeType, String address) {
    // 시스템 입력
    String systemText = """
        당신은 이미지 기반 장소 추천 전문가입니다.

        ## 역할
        - Step1 features(type="poi")를 기반으로 googleSearch 도구를 사용해 실제 존재하는 장소만 후보로 생성합니다.
        - 장소명(name), address, location은 반드시 googleSearch 결과에서만 가져오며 임의 생성 금지.
        - POI는 feature 1개당 메인 1개 + 확장 최대 2개 후보 생성 가능, 전체 후보는 최대 5개.
        - Step1에서 도출된 POI 명(예: "경복궁")은 최우선 존중.

        ## 공통 규칙
        - 검색 결과에서 name 또는 address가 없으면 후보 제외.
        - 검색어 그대로 name에 넣거나 title/snippet을 조합하여 이름 생성 금지.
        - 검색어와 반환된 name이 동일하면 검색 실패로 간주.
        - 이미 선택된 장소는 중복 사용 금지.
        - similarity/confidence는 Step1 feature와의 연관성 및 검색 품질 기준.

        ## 대체 규칙(Fallback)
        - googleSearch 도구 응답이 "인터넷 검색 중 오류 발생"으로 시작하면
          즉시 검색 중단 후 visualFeatures에 "인터넷 검색 실패" 포함 단일 JSON 출력.

        --------------------------------------------
        ## POI 처리 규칙

        ### 1) 메인 후보 생성
        - 검색어: "서울 <요소명>"
        - googleSearch 결과 중 name+address 존재하는 첫 번째 실제 장소 사용.

        ### 2) visualFeatures 기반 확장 후보 생성 (최대 2개)
        - 핵심 단어는 POI의 장소성을 반영:
          예) 경복궁 → 궁궐 / 전통 건축, 청계천 → 하천 / 도심 하천, 롯데타워 → 전망대 / 초고층 빌딩
        - 야경, 조명, 사람, 건물들 등 맥락·분위기 단어 제외
        - 검색어: "서울 <핵심단어>"
        - 사용자 주소의 지역(구 기준) 근접 장소 우선 선택
        - 메인 후보와 동일한 장소는 제외

        ### 3) 최종 후보 구성
        - 메인 후보 1개 + 확장 후보 최대 2개 = feature별 최대 3개
        - 전체는 similarity/confidence 기준 상위 5개까지 출력

        --------------------------------------------
        ## 출력 형식(JSON)
        [
          {
            "name": "장소명",
            "type": "poi",
            "address": "주소",
            "lat": 위도,
            "lng": 경도,
            "location": "주소 또는 지역",
            "visualFeatures": "Step1 요소와의 관계",
            "similarity": "high | medium | low",
            "confidence": 0.0~1.0,
            "imageUrl": ""
          }
        ]

        ## 사용자 입력
        - step1Result: %s
        - placeType: %s
        - address: "%s"

        위 정보를 기반으로 googleSearch 도구 호출 후 실제 장소 후보만 JSON 배열로 출력하세요.
        """;

    // 사용자 입력
    String userText = String.format("""
        {
          "step1Result" : %s,
          "placeType" : %s,
          "address" : "%s",
          "instruction": "반드시 googleSearch 도구를 사용해서 실제 장소만 추천하세요."
        }
          """, llmResponse, placeType, address);

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

    List<PlaceCandidateResponse> candidates = parseJsonToList(response, new TypeReference<List<PlaceCandidateResponse>>() {
    });
    log.info(candidates.toString());

    return candidates;
  }

  public List<PlaceCandidateResponse> generateCategoryCandidates(String placeFeatures, String placeType, String address) {
    // 시스템 입력
    String systemText = """
        당신은 이미지 기반 장소 추천 전문가입니다.

        ## 역할
        - Step1 features(type="category")를 기반으로 googleSearch 도구를 사용해 실제 존재하는 장소만 후보로 생성합니다.
        - 장소명(name), address, location은 반드시 googleSearch 결과에서만 가져오며 임의 생성 금지.
        - feature 1개당 최대 1개의 후보를 원칙으로 하되, Step1 결과 feature가 하나뿐인 경우
          동일 feature를 기반으로 최대 3개 후보 생성 가능.
        - 전체 최종 후보 수는 최대 5개.

        ## 공통 규칙
        - 검색 결과에서 name 또는 address가 없으면 후보 제외.
        - 검색어 그대로 name 삽입 금지.
        - 검색어와 반환된 name이 동일하면 검색 실패.
        - 이미 선택한 장소는 중복 배제.
        - Step1 type(category) 유지.
        - similarity/confidence는 Step1 feature와 검색 결과의 연관성 기준.

        ## 대체 규칙(Fallback)
        - googleSearch 도구 호출 결과가 "인터넷 검색 중 오류 발생"으로 시작하면
          즉시 검색 중단 후 visualFeatures 필드에 "인터넷 검색 실패" 문구만 포함한 단일 JSON 결과 출력.

        --------------------------------------------
        ## Category 처리 규칙
        - 검색어: "<사용자 주소 지역명> <요소명>"
        - googleSearch 결과 중 name+address 존재하는 첫 번째 장소 선택
        - 검색 결과 없으면 해당 feature는 후보 제외
        - Step1 feature가 하나만 존재하면, 동일 feature로 추가 후보 최대 3개 선택 가능

        --------------------------------------------
        ## 출력 형식(JSON)
        [
          {
            "name": "장소명",
            "type": "category",
            "address": "주소",
            "lat": 위도,
            "lng": 경도,
            "location": "주소 또는 지역",
            "visualFeatures": "Step1 요소와의 관계",
            "similarity": "high | medium | low",
            "confidence": 0.0~1.0,
            "imageUrl": ""
          }
        ]

        ## 사용자 입력
        - step1Result: %s
        - placeType: %s
        - address: "%s"

        위 정보를 기반으로 googleSearch 도구를 사용해 후보를 생성하고 JSON 배열로 출력하세요.
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

    List<PlaceCandidateResponse> candidates = parseJsonToList(response, new TypeReference<List<PlaceCandidateResponse>>() {
    });
    log.info(candidates.toString());

    return candidates;
  }

  // json -> list<dto>로 변환해주는 메서드
  public <T> List<T> parseJsonToList(String jsonResponse, TypeReference<List<T>> typeReference) {

    // 빈 값 처리
    if (jsonResponse == null || jsonResponse.trim().isEmpty() || jsonResponse.trim().equals("[]")) {
      log.warn("빈 JSON 응답 받음");
      return Collections.emptyList();
    }

    String cleanedJson = jsonResponse.trim();
    if (cleanedJson.startsWith("```json")) {
      cleanedJson = cleanedJson.substring(7); // "```json" 제거
    }
    if (cleanedJson.startsWith("```")) {
      cleanedJson = cleanedJson.substring(3); // "```" 제거
    }
    if (cleanedJson.endsWith("```")) {
      cleanedJson = cleanedJson.substring(0, cleanedJson.length() - 3);
    }
    cleanedJson = cleanedJson.trim(); // 앞뒤 공백 최종 제거

    if (cleanedJson.isEmpty() || cleanedJson.equals("[]")) {
      return Collections.emptyList();
    }

    // 정제된 JSON 문자열을 사용
    try {
      JsonNode rootNode = objectMapper.readTree(cleanedJson);

      // 배열인지 확인
      if (!rootNode.isArray()) {
        log.warn("예상된 JSON 배열 형식이 아닙니다: {}", cleanedJson);
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
