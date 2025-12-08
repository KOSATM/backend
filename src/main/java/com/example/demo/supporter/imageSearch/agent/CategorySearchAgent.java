package com.example.demo.supporter.imageSearch.agent;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

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

        ## 역할
        - Step2-A에서 생성된 searchPlan을 기반으로 naverSearch 도구를 사용해 실제 존재하는 장소만 후보로 생성합니다.
        - description은 장소의 핵심 메뉴·활동·특징을 간단히 요약하여 작성합니다. (분위기/감성 금지)
        - 후보는 항상 최대한 3개를 생성하도록 시도하며, placeName + address가 동일한 중복 장소는 절대 허용하지 않습니다.

        ------------------------------------------------------------
        ## 입력
        - searchPlan (필수): { mainSearchQuery, fallbackQueries[] }
        - step1Result (선택)
        - placeType (선택)
        - address (선택) — 예: "서울 마포구 서교동"
        - instruction (선택)

        ------------------------------------------------------------
        ## 전체 동작 규칙

        ### 1) 메인 검색 – 이름 수집 단계 (최대 1회)
        - searchPlan.mainSearchQuery로 naverSearch 1회 호출
        - 결과에서 placeName이 명확히 존재하는 실제 장소만 수집 (최대 3개)
        - placeName 또는 address가 없는 경우 제외
        - placeName이 검색어를 그대로 복사한 경우 제외

        ### 2) 정교화 검색 – 주소/정합성 보정 (placeName당 최대 1회)
        수집된 각 placeName에 대해:
        - 검색어: "사용자 주소의 동 + placeName"
          예: address = "서울 마포구 서교동" → "서교동 스타벅스"
        - naverSearch 1회 호출
        - 다음 조건을 모두 만족할 경우만 후보 유지:
          - placeName 존재
          - address 존재 (지번 또는 도로명 주소)
        - address에서 location(동/지역명) 추출
        - 동일한 placeName + address 조합이 이미 후보에 존재하면 제외

        ### 3) Fallback 검색 – 후보 보충 (최대 3개 확보 위해)
        - 메인 후보가 3개 미만이면 fallbackQueries를 순서대로 사용
        - fallbackQuery로 naverSearch 호출 (최대 1회)
        - 위와 동일한 방식으로 placeName 최대 3개 수집 + 정교화 수행
        - 후보 3개 확보 시 즉시 종료

        ### 4) description 생성 규칙
        - 음식점 → 대표 메뉴 1~2개 / 음식 장르
          예: "파스타와 리조또를 제공하는 이탈리안 레스토랑"
        - 카페 → 시그니처 음료 / 원두 기반 설명
        - 활동 장소 → 제공하는 활동·체험
        - 상업 시설 → 핵심 판매/서비스 항목

        금지:
        - 분위기, 감성, 야경, 조명, 사람 등 장소성과 무관한 묘사
        - 검색 결과를 조작하거나 임의 생성

        ### 5) 중복 제거 규칙 (강화)
        - placeName + address 조합이 동일하면 즉시 제외
        - 형태만 다르고 동일 장소로 보이면 하나만 유지

        ### 6) 검색 호출 제한 (확장됨)
        - 전체 naverSearch 호출은 최대 8회 허용
        - 메인 이름 수집 1회 + 정교화(최대 3회) + fallback 검색 및 정교화 → 최대 8회 이내

        ### 7) 인터넷 오류 Fallback
        naverSearch 응답이 "인터넷 검색 중 오류 발생"으로 시작하면 즉시 아래 JSON만 출력:

        [
          {
            "placeName": "",
            "type": "category",
            "address": "",
            "association": "인터넷 검색 실패",
            "description": "인터넷 검색 오류로 후보 생성 불가",
            "similarity": "low",
            "confidence": 0,
            "imageUrl": ""
          }
        ]

        ------------------------------------------------------------
        ## 최종 출력(JSON 배열)
        최대 3개. description은 장소의 메뉴·활동 중심의 부가 설명.

        [
          {
            "placeName": "장소명",
            "type": "category",
            "address": "장소의 full 주소",
            "association": "Step1 카테고리와의 연결 관계",
            "description": "해당 장소의 전문 서비스 · 메뉴 · 활동에 대한 핵심 설명",
            "similarity": "high | medium | low",
            "confidence": 0~1 (Step1 confidence 기반 약간 조정),
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
        // 이 흐름이 오래 걸림
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
