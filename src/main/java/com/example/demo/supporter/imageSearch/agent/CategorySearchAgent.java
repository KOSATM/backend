package com.example.demo.supporter.imageSearch.agent;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import com.example.demo.common.tools.InternetSearchTool;
import com.example.demo.common.tools.NaverInternetSearchTool;
import com.example.demo.common.util.JsonParser;
import com.example.demo.supporter.imageSearch.dto.response.PlaceCandidateResponse;
import com.fasterxml.jackson.core.type.TypeReference;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CategorySearchAgent {
  private final ChatClient chatClient;
  private final NaverInternetSearchTool internetSearchTool;
  private final JsonParser jsonParser;

  public CategorySearchAgent(ChatClient.Builder chatClientBuilder, NaverInternetSearchTool internetSearchTool,
      JsonParser jsonParser) {
    this.chatClient = chatClientBuilder.build();
    this.internetSearchTool = internetSearchTool;
    this.jsonParser = jsonParser;
  }

  // ------------------------------------------------------------
  // Step2-A: SearchPlan 생성 (Category)
  // -> naverSearch 호출하지 않고 검색계획(SearchPlan) JSON을 반환
  // ------------------------------------------------------------
  public String generateCategorySearchPlan(String step1ResultJson, String address) {

    String systemText = """
        당신은 이미지 기반 장소 추천을 위한 SearchPlan 생성 전문가입니다.

        ## 역할
        - Step1 features(type="category")와 사용자 주소를 기반으로,
          전체 주소가 아닌 **동(…동) 단위만을 지역명으로 추출하여 `<지역>` 값으로 사용합니다.
        - 카테고리별 고정 템플릿 규칙만 적용하여 검색용 Query만 생성합니다.
        - 분석/확장/의미추론 금지. 오직 아래 규칙대로 문자열 조합만 수행합니다.
        - 임의 키워드 추가 금지.
        - JSON 이외의 어떤 설명도 출력 금지.

        ------------------------------------------------------------
        ## 지역(동) 추출 규칙
        - 주소 문자열에서 가장 마지막에 등장하는 `"동"`으로 끝나는 구간을 `<지역>`으로 사용합니다.
        - 예시:
          - "서울특별시 강남구 역삼동 123-4" → "역삼동"
          - "부산 해운대구 우동" → "우동"
          - "성남시 분당구 정자동 12" → "정자동"
        - 만약 "동"이 없다면 전체 주소 중 **공백 단위 마지막 토큰**을 지역으로 사용합니다.

        ------------------------------------------------------------
        ## 카테고리별 Query 생성 규칙

        ### 1) category = "Food"
        - mainSearchQuery : "<지역> <요소명>집"
        - fallbackQueries : ["<지역> <요소명> 맛집"]

        ### 2) category = "Dessert"
        - mainSearchQuery : "<지역> <요소명> 카페"
        - fallbackQueries : ["<지역> <요소명>집"]

        ### 3) category = "Exercise"
        - mainSearchQuery : "<지역> <요소명>"
        - fallbackQueries : [
            "<지역> <요소명>장",
            "<지역> <요소명> 시설"
          ]

        ### 4) category = "Place"
        - mainSearchQuery : "<지역> <요소명>"
        - fallbackQueries : []

        ------------------------------------------------------------
        ## 출력 형식(JSON)

        {
          "categoryList": [
            {
              "featureName": "",
              "category": "",
              "mainSearchQuery": "",
              "fallbackQueries": []
            }
          ],
          "userRegion": ""
        }

        ------------------------------------------------------------
        ## 사용자 입력
        - step1Result: %s
        - address: "%s"

        위 정보를 기반으로 SearchPlan JSON만 출력하세요.


        """;

    String userText = String.format("""
        {
          "step1Result": %s,
          "address": "%s"
        }
        """, step1ResultJson, address);

    String response = chatClient.prompt()
        .system(systemText)
        .user(userText)
        .call()
        .content();

    log.info("Category SearchPlan 응답(개선버전): {}", response);
    return response;
  }

  // ------------------------------------------------------------
  // Step2-B: 기존 generateCategoryCandidates 프롬프트 (원본 그대로 사용)
  // -> 여기에 searchPlan JSON을 추가해서 LLM에 전달
  // ------------------------------------------------------------
  public List<PlaceCandidateResponse> generateCategoryCandidates(String placeFeatures, String placeType,
      String address) {

    // 먼저 Step2-A로 SearchPlan 생성
    String searchPlanJson = generateCategorySearchPlan(placeFeatures, address);

    // 원본 systemText (프롬프트 내용은 절대 변경하지 않음)
    String systemText = """
        당신은 이미지 기반 장소 추천을 위한 Category 후보 생성 전문가입니다.
        요구: Step2-A에서 생성된 "searchPlan"을 기반으로 naverSearch 도구를 최소한으로 사용하여 실제 존재하는 장소만 후보로 생성합니다.
        특별지시(2단계 검색 방식 자동화):
        1) 입력:
           - searchPlan (필수): {mainSearchQuery, fallbackQueries[]}
           - step1Result (optional)
           - placeType (optional)
           - address (optional) — 예: "서울 마포구 서교동"
           - instruction (optional)
        2) 동작:
           A. 1차(이름 수집): mainSearchQuery으로 naverSearch 호출(최대 1회).
              - 결과에서 placeName 후보(최대 3개까지 수집). (placeName은 페이지에서 명확한 가게명으로 파싱)
              - 만약 검색결과 없음 또는 placeName 누락 시 fallbackQueries[0]으로 이동.
           B. 2차(주소 정교화): 각 수집한 placeName에 대해 "동 + placeName" 문자열로 두 번째 naverSearch 호출(각 placeName당 최대 1회).
              - 2차 결과에서 반드시 placeName(일치 여부)과 address(동 포함)를 찾음.
              - 조건 충족 시 해당 후보 채택(필수 필드: placeName, address). location은 address에서 추출.
           C. 중복, placeName이 검색어 그대로(복붙)인 경우 해당 후보 배제.
        3) 호출 제한:
           - feature(카테고리) 당 총 naverSearch 호출 <= 2 (main + fallback[0]) — 단, 위 2단계 구현에서는 "main"이 이름 수집, "동+이름"이 정교화용으로 간주되어 내부적으로 같은 feature에 대해 2회 초과하지 않음.
           - 전체 naverSearch 호출 절대 6회 초과 금지.
        4) 예외(Fallback 규칙):
           - naverSearch 응답이 "인터넷 검색 중 오류 발생"으로 시작하면 즉시 아래 JSON 하나만 출력:
             [
               {
                 "placeName": "",
                 "type": "category",
                 "address": "",
                 "location": "",
                 "association": "인터넷 검색 실패",
                 "description": "인터넷 검색 오류로 후보 생성 불가",
                 "similarity": "low",
                 "confidence": 0,
                 "imageUrl": ""
               }
             ]
        5) 출력(JSON 배열, 절대 설명 문구 금지):
           [
             {
               "placeName": "장소명",
               "type": "category",
               "address": "주소(도로명/지번/동 등)",
               "location": "지역명(예: 서교동, 홍대)",
               "association": "Step1 카테고리와의 관계 설명",
               "description": "간단 문장 설명",
               "similarity": "high | medium | low",
               "confidence": 0~1,
               "imageUrl": ""
             }
           ]
                """;

    // 원본 userText에 searchPlan 필드만 추가 (searchPlanJson은 JSON 문자열)
    String userText = String.format("""
        {
          "searchPlan" : %s,
          "step1Result" : %s,
          "placeType" : %s,
          "address" : "%s",
          "instruction": "반드시 naverSearch 도구를 사용해서 실제 장소만 추천하세요."
        }
        """, searchPlanJson, placeFeatures, placeType, address);

    String response = chatClient.prompt()
        .system(systemText)
        .user(userText)
        .tools(internetSearchTool)
        .call()
        .content();

    log.info("2단계 AI 응답(Category - 2-B): {}", response);

    List<PlaceCandidateResponse> candidates = jsonParser.parseJsonToList(response,
        new TypeReference<List<PlaceCandidateResponse>>() {
        });
    log.info(candidates.toString());

    return candidates;
  }
}
