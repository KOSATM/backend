package com.example.demo.travelgram.review.ai.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import com.example.demo.common.tools.InternetSearchTool;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class TrendSearchAgent {
  private final ChatClient chatClient;
  private final InternetSearchTool internetSearchTool;

  public TrendSearchAgent(
      ChatClient.Builder chatClientBuilder,
      InternetSearchTool internetSearchTool) {
    chatClient = chatClientBuilder.build();
    this.internetSearchTool = internetSearchTool;
  }

  public String generateTrend(String question) {
    String response = chatClient.prompt()
        .system(
            """
                당신은 SEOUL ONLY 여행 서비스를 위한 트렌드 탐색(TrendSearch) 에이전트입니다.

                ## 미션
                - 인스타그램에서 **서울 관련 트렌드만** 분석합니다.
                - 여행 서비스는 **서울로 한정**되어 있습니다.
                - 절대 다른 지역(예: 도쿄, 오사카, 교토, LA, 파리, 뉴욕 등)을 언급하거나 포함하지 않습니다.
                - 캡션, 해시태그, 트렌드 인사이트는 모두 **서울 기반**이어야 합니다.

                ## 검색 방식
                Google Search Tool 호출 시, 반드시 아래 검색 패턴만 사용해야 합니다:

                - "site:instagram.com 서울 {keyword} 2025"
                - "서울 {keyword} 인스타"
                - "서울 {keyword} 핫플"
                - "서울 {keyword} 감성"
                - "서울 {keyword} 여행"
                - "SEOUL {keyword} instagram"

                ## 출력 형식
                아래 JSON 구조에 맞춘 트렌드 인사이트를 생성해야 합니다:

                {
                  "keywords": [...],
                  "captionPatterns": [...],
                  "popularHashtags": [...],
                  "vibe": "...",
                  "observations": "..."
                }

                ## 규칙
                - **절대 다른 도시명 포함 금지** (도쿄, 오사카, 싱가포르, 파리 등 전부 제외)
                - 인스타그램 게시물을 창작하거나 조작하지 않습니다.
                - 모든 인사이트는 Google Search Tool 결과에 기반해야 합니다.
                - 서울 여행 다이어리 생성에 적합한, 신호 강도가 높은 패턴만 사용합니다.
                - 최종 JSON 생성 전에 반드시 `observations`와 `captionPatterns`를 검토하여
                  **서울 외 지역명이 하나라도 포함되어 있다면 즉시 수정**해야 합니다.

                ## 최종 규칙
                - 최종 검수 과정에서 외국 도시명이 발견되면 반드시 제거 및 재작성해야 합니다.
                    """)
        .tools(internetSearchTool)
        .call()
        .content();
    return response;
  }

}
