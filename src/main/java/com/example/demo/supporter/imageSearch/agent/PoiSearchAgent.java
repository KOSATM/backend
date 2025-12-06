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

    public PoiSearchAgent(ChatClient.Builder chatClientBuilder, NaverInternetSearchTool internetSearchTool, JsonParser jsonParser) {
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
                - POI는 feature 1개당 메인 1개 + 확장 최대 2개 후보 생성 가능, 전체 후보는 최대 3개.
                - Step1에서 도출된 POI 명(예: "경복궁")은 최우선 존중.

                ## 공통 규칙
                - 검색 결과에서 placeName 또는 address가 없으면 후보 제외.
                - 검색어 그대로 placeName에 넣거나 title/snippet을 조합하여 이름 생성 금지.
                - 검색어와 반환된 placeName이 동일하면 검색 실패로 간주.
                - 이미 선택된 장소는 중복 사용 금지.
                - similarity/confidence는 Step1 feature와의 연관성 및 검색 품질 기준.

                ## 대체 규칙(Fallback)
                - naverSearch 도구 응답이 "인터넷 검색 중 오류 발생"으로 시작하면
                  즉시 검색 중단 후 visualFeatures에 "인터넷 검색 실패" 포함 단일 JSON 출력.

                --------------------------------------------
                ## POI 처리 규칙

                ### 1) 메인 후보 생성
                - 검색어: "서울 <요소명>"
                - naverSearch 결과 중 placeName+address 존재하는 첫 번째 실제 장소 사용.

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
                    "placeName": "장소명",
                    "type": "poi",
                    "address": "주소"
                    "location": "주소 또는 지역",
                    "association": "Step1 요소와의 관계",
                    "description" : "장소명에 대한 간단한 설명",
                    "similarity": "high | medium | low",
                    "confidence": 0.0~1.0,
                    "imageUrl": ""
                  }
                ]

                ## 사용자 입력
                - step1Result: %s
                - placeType: %s
                - address: "%s"

                위 정보를 기반으로 naverSearch 도구 호출 후 실제 장소 후보만 JSON 배열로 출력하세요.
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
