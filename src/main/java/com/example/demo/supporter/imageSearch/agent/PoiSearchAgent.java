package com.example.demo.supporter.imageSearch.agent;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import org.stringtemplate.v4.compiler.CodeGenerator.primary_return;

import com.example.demo.common.tools.InternetSearchTool;
import com.example.demo.common.tools.NaverInternetSearchTool;
import com.example.demo.common.util.JsonParser;
import com.example.demo.supporter.imageSearch.dto.response.PlaceCandidateResponse;
import com.fasterxml.jackson.core.type.TypeReference;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PoiSearchAgent {
  private final ChatClient chatClient;
  private final NaverInternetSearchTool internetSearchTool;
  private final JsonParser jsonParser;

  public PoiSearchAgent(ChatClient.Builder chatClientBuilder, NaverInternetSearchTool internetSearchTool,
      JsonParser jsonParser) {
    this.chatClient = chatClientBuilder.build();
    this.internetSearchTool = internetSearchTool;
    this.jsonParser = jsonParser;
  }

  // ------------------------------------------------------------
  // Step2-A: SearchPlan 생성 (POI)
  // -> googleSearch 호출하지 않고 검색계획(SearchPlan) JSON을 반환
  // ------------------------------------------------------------
  public String generatePoiSearchPlan(String step1ResultJson, String address) {
    String systemText = """
        당신은 이미지 기반 장소 추천을 위한 "검색 계획(SearchPlan)" 생성 전문가입니다.

        ## 역할
        - Step1 features(type="poi")를 기반으로, 실제 검색에 필요한 정보를 구조화해서 SearchPlan JSON을 생성합니다.
        - 이 단계에서는 googleSearch 도구를 사용하지 않습니다.
        - 이 단계의 목적은 다음 항목을 정확하게 도출하는 것입니다:

        ### 반드시 생성해야 할 정보
        1) featureName — Step1 POI 명 그대로
        2) mainSearchQuery — "서울 <요소명>"
        3) extensionKeywords — 장소성 기반 핵심 단어 목록 (최대 2개)
        4) extensionSearchQueries — extensionKeywords 각각에 대해 "서울 <핵심단어>" 형태로 생성
        5) userRegion — 사용자의 주소에서 구 단위 지역명만 추출

        ### 핵심 규칙
        - Step1 POI 명(예: "경복궁")을 최우선으로 존중합니다.
        - 핵심 단어는 장소성 기반 단어만 포함하고 분위기 단어(야경/사람/조명/건물들 등)는 제외합니다.
        - 확장 키워드는 최대 2개까지만 생성합니다.
        - SearchPlan은 검색 실행용 "입력 템플릿"이며 후보 생성은 하지 않습니다.
        - JSON 이외의 설명, 문장 출력 금지.

        ## 출력 형식(JSON)
        {
          "poiList": [
            {
              "featureName": "",
              "mainSearchQuery": "",
              "extensionKeywords": [],
              "extensionSearchQueries": []
            }
          ],
          "userRegion": ""
        }

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

    log.info("POI SearchPlan 응답(2-A): {}", response);
    return response;
  }

  // ------------------------------------------------------------
  // Step2-B: 기존 generatePoiCandidates 프롬프트 (원본 그대로 사용)
  // -> 여기에 searchPlan JSON을 추가해서 LLM에 전달
  // ------------------------------------------------------------
  public List<PlaceCandidateResponse> generatePoiCandidates(String llmResponse, String placeType, String address) {
    // 먼저 Step2-A로 SearchPlan 생성
    String searchPlanJson = generatePoiSearchPlan(llmResponse, address);

    // 원본 systemText (프롬프트 내용은 절대 변경하지 않음)
    String systemText = """
        당신은 이미지 기반 장소 추천 전문가입니다.

        ## 역할
        - Step1 features(type="poi")를 기반으로 naverSearch 도구를 사용해 실제 존재하는 장소만 후보로 생성합니다.
        - 장소명(placeName), address, location은 반드시 naverSearch 결과에서만 가져오며 임의 생성 금지.
        - Step1에서 도출된 POI명(예: "경복궁")은 메인 후보 생성에서 최우선으로 사용합니다.
        - 후보 장소는 이름(placeName)과 주소(address)가 동일한 경우 절대 중복 포함 금지.

        ## 후보 생성 목표 (매우 중요)
        - feature 1개당 “가능하면 반드시 3개의 고유 POI 후보”를 생성해야 합니다.
        - 검색 결과가 부족할 경우:
          - 확장 검색 2회 + 추가 fallback 검색 2회까지 수행하여 최대한 3개 확보
        - 그래도 3개를 만들 수 없다면 생성된 후보만 반환합니다.

        --------------------------------------------

        ## 랜드마크 우선 규칙 (중요 · 새로 추가됨)
        - 이 에이전트는 POI 전용이므로 “랜드마크 중심의 확장 후보”를 생성하는 것이 핵심입니다.
        - Step1 feature가 특정 랜드마크라면 확장 후보 역시 반드시 **랜드마크/대표 건축물/문화유산 등 장소성 높은 POI**를 우선 선택합니다.
        - 즉, feature: 경복궁 → 확장 후보는 창덕궁, 창경궁, 덕수궁, 숭례문 등 동일 계열의 랜드마크를 우선 고려해야 합니다.
        - 다음과 같은 카테고리는 확장 후보에서 제외합니다:
          - 상점, 카페, 음식점, 체험시설, 호텔 등 "일반 상업시설"
        - 확장 후보는 반드시 “관광지/고유명사 장소/대표 랜드마크”로만 구성되어야 합니다.

        --------------------------------------------

        ## 공통 규칙
        - 검색 결과에서 placeName 또는 address가 없으면 후보 제외.
        - 검색어 그대로 placeName에 넣거나 title/snippet을 조합하여 이름 생성 금지.
        - 검색어와 반환된 placeName이 동일하면 검색 실패로 간주.
        - 이미 선택한 장소(placeName + address)는 중복 금지.

        ## 대체 규칙(Fallback)
        - naverSearch 도구 응답이 "인터넷 검색 중 오류 발생"으로 시작하면
          즉시 모든 검색을 중단하고 아래 형식의 단일 JSON을 출력:
          [
            {
              "placeName": "",
              "type": "poi",
              "address": "",
              "location": "",
              "association": "인터넷 검색 실패",
              "description": "인터넷 검색 오류로 후보 생성 불가",
              "similarity": "low",
              "confidence": 0,
              "imageUrl": ""
            }
          ]

        --------------------------------------------

        ## POI 후보 생성 절차

        ### 1) 메인 후보 생성(1개)
        - 검색어: "서울 <featureName>"
        - naverSearch 결과 중 placeName+address 존재하는 첫 번째 장소 선택

        ### 2) 확장 후보 생성(최대 2개)
        - Step1 visualFeatures에서 장소성을 대표하는 핵심단어 추출
          예) 경복궁 → 궁궐 / 전통 건축
              롯데타워 → 전망대 / 빌딩
              청계천 → 하천 / 도심 하천
        - 분위기 요소(야경, 조명, 사람 등) 제외
        - 검색어: "서울 <핵심단어>"
        - **랜드마크/유적지/대표 관광지 중심으로 후보 우선 선택**
        - 사용자 주소(구 단위)와 가까운 장소 우선
        - 메인 후보와 동일 장소 제외

        ### 3) 부족 시 추가 검색(최대 2회)
        - featureName 또는 핵심단어를 더 넓은 개념으로 확장하여 랜드마크 중심으로 검색
          예) 경복궁 → 궁궐 → 전통문화 → 문화유적 → 관광지
        - 동일 장소 중복 금지
        - 일반 상업시설 제외

        --------------------------------------------
        ## 출력 형식(JSON)
        description은 장소에 대한 핵심 설명 (200자 이내).
        [
          {
            "placeName": "장소명",
            "type": "poi",
            "address": "주소",
            "location": "주소 또는 지역 (구까지 포함)",
            "association": "Step1 요소와의 관계",
            "description": "해당 장소에 대한 핵심 설명 (200자 이내)",
            "similarity": "high | medium | low",
            "confidence": 0~1,
            "imageUrl": ""
          }
        ]

        ## 사용자 입력
        - step1Result: %s
        - placeType: %s
        - address: "%s"

        위 정보를 기반으로 naverSearch 도구를 사용해 실제 존재하는 장소만 후보로 생성하여 JSON 배열로 출력하세요.
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
          """, searchPlanJson, llmResponse, placeType, address);

    String response = chatClient.prompt()
        .system(systemText)
        .user(userText)
        .tools(internetSearchTool)
        .call()
        .content();

    log.info("2단계 AI 응답(POI - 2-B): {}", response);

    List<PlaceCandidateResponse> candidates = jsonParser.parseJsonToList(response,
        new TypeReference<List<PlaceCandidateResponse>>() {
        });
    log.info(candidates.toString());

    return candidates;
  }

}
