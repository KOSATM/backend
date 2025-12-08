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
            - 입력 메시지는 이미 전처리되어 날짜/시간/장소가 정규화된 상태입니다.

            - 예시:
                - travel_plan → { "location": "...", "duration": "..." }
                - plan_query → {} (내 계획 조회, show plan, 내 일정 보여줘 등)
                - plan_add → { "date": "...", "place": "..." }
                - plan_modify → { "oldPlace": "...", "newPlace": "...", "day": "..." }
                - attraction_recommend → { "location": "..." }
                - plan_place_recommend → { "location": "홍대" } 또는 { "keyword": "케이팝" } 또는 { "location": "성수동", "keyword": "카페" }
                - currency_exchange → { "from": "USD", "to": "KRW" }

            ---

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
        """.formatted(intentRequest.getUserMessage(), intentRequest.getCurrentUrl());
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
    log.info("responseJSON: {}", responseJSON);

    IntentResponse intentResponse = beanOutputConverter.convert(responseJSON);

    if (intentResponse == null)
      // fallback — etc 단일 intent 생성
      return IntentResponse.builder()
          .intents(List.of(
              IntentItem.builder()
                  .intent("etc")
                  .confidence(0.0)
                  .arguments(Map.of())
                  .build()))
          .build();

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
