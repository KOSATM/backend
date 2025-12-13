package com.example.demo.common.chat.intent.agent;

import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

import com.example.demo.common.chat.intent.CategoryType;
import com.example.demo.common.chat.intent.IntentType;
import com.example.demo.common.chat.intent.dto.IntentItem;
import com.example.demo.common.chat.intent.dto.request.IntentRequest;
import com.example.demo.common.chat.intent.dto.response.IntentResponse;
import com.example.demo.common.chat.intent.service.IntentProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@ToString
public class IntentAnalysisAgent {

  private ChatClient chatClient;
  private IntentProcessor intentProcessor;
  private ObjectMapper objectMapper = new ObjectMapper();

  public IntentAnalysisAgent(ChatClient.Builder chatClientBuilder, IntentProcessor intentProcessor) {
    this.chatClient = chatClientBuilder.build();
    this.intentProcessor = intentProcessor;
  }

  public IntentResponse analyze(IntentRequest intentRequest) {
    BeanOutputConverter<IntentResponse> beanOutputConverter = new BeanOutputConverter<>(IntentResponse.class);

    String systemPrompt = """
                당신은 여행 서비스 전체에서 공통으로 사용되는 "의도 분석 에이전트(Intent Classification Agent)"입니다.
                당신의 임무는 사용자의 자연어 입력을 분석하여, 해당 메시지가 어느 기능군(Category)에 속하는지,
                그리고 그 안에서 어떤 Intent인지 정확하게 분류하고 구조화된 JSON으로 반환하는 것입니다.

                하나의 메시지 안에 여러 요청이 포함될 수 있으므로, 가능한 모든 Intent를 추출하여 배열 형태로 반환해야 합니다.


                ---

                # ✔ Category(기능군)

                %s

                ---

                # ✔ IntentType 목록

                %s

                ---

                # ✔ arguments 규칙
                - Intent 수행에 필요한 정보만 key-value 형태로 추출한다.
                - 명확하게 파악되지 않는 값은 만들지 않는다.
                - 값의 형식을 강제하지 않습니다.(숫자/정규화/전처리 등 하지 않음)

                - 예시:
                  - travel_plan →
                    - pace, budget, timePreference 등은 서버 로직에서 해석된다.
                    {
                      "location": "...", 
                      // 여행을 진행할 주요 지역 (예: 강남, 서울, 홍대 등)

                      "duration": "...", 
                      // 여행 기간 표현 (예: 하루, 1박 2일, 주말, 3일간 등)

            - 입력이 의미 없는 문자이거나 의도를 추론할 수 없는 경우, intent를 "other"로 분류하고 arguments는 빈 객체로 반환한다.
            - 분류가 불확실하거나 모호한 경우도 "other"로 분류하면 SmartPlanAgent가 대화형으로 처리합니다.
            - arguments는 빈 객체 {}로 반환한다.

            # ✔ confidence 규칙
            - 0.0 ~ 1.0 사이 값
            - 0.8 이상: 매우 확신
            - 0.5 ~ 0.8: 중간 확신
            - 0.5 미만: 불확실 → "other"로 분류하면 SmartPlanAgent가 처리

                      "pace": "...", 
                      // 일정 밀도에 대한 사용자 표현 (예: 느긋하게, 적당히, 빡빡하게 등)

                      "companion": "...", 
                      // 여행 동반자 정보 (예: 혼자, 연인, 가족, 친구, 아이와 등)

                      "timePreference": "...", 
                      // 선호 시간대 또는 일정 집중 시간 (예: 아침, 오후, 저녁, 야간 등)

                      "mustPlace": ["..."], 
                      // 반드시 포함하고 싶은 장소, 거리, 지역명 목록

                      "exclude": ["..."] 
                      // 일정에서 제외하고 싶은 장소 유형 또는 키워드 목록
                    }

                  - plan_add →
                    {
                      "date": "...",
                      "place": "..."
                    }

                  - plan_modify →
                    {
                      "oldPlace": "...",
                      "newPlace": "...",
                      "day": "...",
                      "oldDayindex": "...",
                      "newDayindex": "..."
                    }

                  - attraction_recommend →
                    {
                      "location": "..."
                    }

                  - plan_place_recommend →
                    { "location": "홍대" }
                    또는
                    { "keyword": "케이팝" }
                    또는
                    { "location": "성수동", "keyword": "카페" }

                  - currency_exchange →
                    {
                      "from": "USD",
                      "to": "KRW"
                    }

                  - navigate →
                    {
                      "targetIntent": "travel_plan"
                    }
                ---

                # ✔ 예외 / 분석 불가 입력 규칙

                - 입력이 의미 없는 문자이거나 의도를 추론할 수 없는 경우, intent를 unknown로 분류하고 arguments는 빈 객체로 반환한다.
                - arguments는 빈 객체 {}로 반환한다.

                # ✔ confidence 규칙
                - 0.0 ~ 1.0 사이 값
                - 0.8 이상: 매우 확신
                - 0.5 ~ 0.8: 중간 확신
                - 0.5 미만: 불확실 → etc 가능성 높음

                ---

                # ✔ 출력 형식 (반드시 지킬 것)

                아래 JSON 형식만 출력한다. 설명·주석 추가 금지.

                {
                  "intents": [
                    {
                      "intent": "plan_add",
                      "confidence": 0.92,
                      "arguments": {
                        "key1": "value1",
                        "key2": "value2"
                      }
                    }
                  ]
                }

                - intent: IntentType 중 하나
                - confidence: 0.0~1.0
                - arguments: 필요한 정보만 포함

                ---

                # ✔ 출력
                오직 JSON만 출력한다.


            """
        .formatted(CategoryType.buildCategoryList(), IntentType.buildIntentList());
    // log.info(systemPrompt);
    String userPrompt = """
        USER_MESSAGE: "%s"
        CURRENT_URL: "%s"

        ※ CURRENT_URL은 참고용 맥락 정보이며, Intent 분류에는 절대 사용하지 마십시오.
        """.formatted(intentRequest.getMessage(), intentRequest.getCurrentUrl());
    log.info(userPrompt);
    // String responseJSON = chatClient.prompt()
    // .system(systemPrompt)
    // .user(userPrompt)
    // .options(ChatOptions.builder().temperature(0.0).build()).call().content();

    // IntentResponse intentResponse = chatClient.prompt()
    // .system(systemPrompt)
    // .user(userPrompt)
    // .options(ChatOptions.builder().temperature(0.0).build()).call().entity(IntentResponse.class);

    // log.info("responseJSON: {}", responseJSON);
    // log.info(intentResponse.toString());
    // return null;

    String responseJSON = chatClient.prompt()
        .system(systemPrompt)
        .user(userPrompt)
        .options(ChatOptions.builder().temperature(0.0).build()).call().content();
    log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    log.info("🔍 [IntentAnalysis] 사용자 메시지: {}", intentRequest.getMessage());
    log.info("📋 [IntentAnalysis] LLM 응답 JSON:");
    log.info("{}", responseJSON);
    log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

    IntentResponse intentResponse = beanOutputConverter.convert(responseJSON);

    if (intentResponse == null) {
      log.warn("⚠️ IntentResponse is null! Fallback to 'other'");
      // fallback — other 단일 intent 생성 (SmartPlanAgent로 라우팅됨)
      return IntentResponse.builder()
          .intents(List.of(
              IntentItem.builder()
                  .intent("other")
                  .confidence(0.0)
                  .arguments(Map.of())
                  .build()))
          .build();
    }

    log.info("✅ [IntentAnalysis] 분류 결과: intent={}, confidence={}",
        intentResponse.getIntents().get(0).getIntent(),
        intentResponse.getIntents().get(0).getConfidence());

    return intentResponse;

    // try {
    // IntentResponse intentResponse = objectMapper.readValue(responseJSON,
    // IntentResponse.class);
    // return intentResponse;

    // } catch (JsonProcessingException e) {
    // log.warn("▶ JSON 파싱 실패: {}", e.getMessage());

    // fallback — etc 단일 intent 생성
    // return IntentResponse.builder()
    // .intents(List.of(
    // IntentItem.builder()
    // .category("etc")
    // .intent("etc")
    // .confidence(0.0)
    // .arguments(Map.of())
    // .build()))
    // .build();
    // }

  }
}
