package com.example.demo.common.chat.intent;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Component;

import com.example.demo.common.chat.intent.dto.IntentCommand;
import com.example.demo.common.chat.intent.dto.request.IntentRequest;
import com.example.demo.common.chat.intent.dto.response.IntentResponse;
import com.example.demo.common.chat.intent.service.IntentProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class IntentAnalysisAgent {

  private ChatClient chatClient;
  private IntentProcessor intentProcessor;
  private ObjectMapper objectMapper = new ObjectMapper();

  public IntentAnalysisAgent(ChatClient.Builder chatClientBuilder, IntentProcessor intentProcessor) {
    this.chatClient = chatClientBuilder.build();
    this.intentProcessor = intentProcessor;
  }

  // public IntentResponse analyze(IntentRequest request){
  public String analyze() {
    // IntentRequest intentRequest =
    // IntentRequest.builder().currentUrl("/planner").userMessage("강남 위주로 여행지
    // 추천해줘").build();
    IntentRequest intentRequest = IntentRequest.builder().currentUrl("/planner")
        .userMessage("오늘 날씨 알려주고 2일차 일정 수정하고 싶어?").build();

    String systemPrompt = """
            당신은 여행 서비스 전체에서 공통으로 사용되는 "의도 분석 에이전트(Intent Classification Agent)"입니다.
            당신의 임무는 사용자의 자연어 입력을 분석하여, 해당 메시지가 어느 기능군(Category)에 속하는지,
            그리고 그 안에서 어떤 Intent인지 정확하게 분류하고 구조화된 JSON으로 반환하는 것입니다.

            하나의 메시지 안에 여러 요청이 포함될 수 있으므로, 가능한 모든 Intent를 추출하여 배열 형태로 반환해야 합니다.

            ---

            # ✔ Category(기능군)

            - planner       : 여행 일정 생성·관리·추천 등 일정 관련 기능
            - supporter     : 번역, 환율, 날씨 등 여행 도우미 기능
            - travelgram    : 여행 기록 작성, 사진 업로드 등 기록 기능
            - etc           : 위 어느 범주에도 속하지 않는 경우

            ---

            # ✔ IntentType 목록

            ## PLANNER
            - travel_plan
            - plan_add
            - plan_delete
            - plan_modify
            - plan_place_recommend
            - attraction_recommend
            - hotel_recommend

            ## SUPPORTER
            - currency_exchange
            - translation
            - weather

            ## TRAVELGRAM
            - create_post
            - add_photo

            ## ETC
            - etc

            ---

            # ✔ arguments 규칙
            - Intent 수행에 필요한 정보만 key-value 형태로 추출한다.
            - 명확하게 파악되지 않는 값은 만들지 않는다.
            - 예시:
                - plan_add → { "date": "내일", "place": "롯데타워" }
                - plan_modify → { "oldPlace": "롯데타워", "newPlace": "코엑스", "day": "1일차" }
                - attraction_recommend → { "location": "부산" }
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
                  "category": "planner",
                  "intent": "plan_add",
                  "confidence": 0.92,
                  "arguments": {
                    "date": "내일",
                    "place": "롯데타워"
                  }
                }
              ]
            }

            - intents: 감지된 Intent 배열
            - category: planner | supporter | travelgram | etc
            - intent: IntentType 중 하나
            - confidence: 0.0~1.0
            - arguments: 필요한 정보만 포함

            ---

            # ✔ 입력

            USER_MESSAGE: "%s"
            CURRENT_URL: "%s"
            (CURRENT_URL는 참고 정보이며 Intent 판단에 직접 사용하지 않는다.)

            ---

            # ✔ 출력
            오직 JSON만 출력한다.

        """;

    String userPrompt = """
        USER_MESSAGE: "%s"
        CURRENT_URL: "%s"
        """.formatted(intentRequest.getUserMessage(), intentRequest.getCurrentUrl());

    String responseJSON = chatClient.prompt()
        .system(systemPrompt)
        .user(userPrompt)
        .options(ChatOptions.builder().temperature(0.0).build()).call().content();

    // log.info("responseJSON: {}", responseJSON);

    try {
      IntentResponse IntentResponse = objectMapper.readValue(responseJSON, IntentResponse.class);
      List<IntentCommand> intentCommands = IntentResponse.getIntents().stream()
          .map(intentProcessor::toCommand)
          .toList();
      log.info(intentCommands.toString());
    } catch (Exception e) {
      log.info("JSON 역직렬화 실패");
    }
    return responseJSON;
  }
}
