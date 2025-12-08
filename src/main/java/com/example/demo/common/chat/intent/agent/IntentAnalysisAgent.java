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
            You are an "Intent Classification Agent" used across the entire travel service.
            Your mission is to analyze user's natural language input, classify which category it belongs to,
            and what Intent it is, and return it as structured JSON.

            One message may contain multiple requests, so extract all possible Intents and return them as an array.

            ---

            # ✔ Category List

            %s

            ---

            # ✔ IntentType List

            %s

            ---

            # ✔ Language & Standardization Rules (CRITICAL)

            1. **Language Detection**
               - Detect user input language (Korean "ko" or English "en")
               - Add "lang" field to arguments: { "lang": "ko" } or { "lang": "en" }
               - All subsequent responses from agents MUST use English only

            2. **Date/Day Standardization**
               - Convert ALL date expressions to standard format
               - Examples:
                 * "첫날", "first day", "day 1", "1일차" → dayIndex: 1
                 * "둘째날", "second day", "day 2", "2일차" → dayIndex: 2
                 * "12월 6일", "December 6th", "Dec 6" → date: "2025-12-06" (use YYYY-MM-DD)
                 * "오늘", "today" → date: "2025-12-08"
                 * "내일", "tomorrow" → date: "2025-12-09"

            3. **Time Standardization & Time Range Queries**
               - Convert time expressions to standard format
               - Examples:
                 * "지금", "now", "현재" → timeQuery: "current"
                 * "다음", "next" → timeQuery: "next"
                 * "3시", "3pm", "15:00" → time: "15:00"
               
               - Time Range Queries (view_plan_time_range):
                 * "아침 일정", "morning schedule", "morning plans" → range: "morning"
                 * "점심 일정", "lunch activities", "what's for lunch" → range: "lunch"
                 * "저녁 일정", "evening schedule", "dinner plans" → range: "evening"
               
               Time Range Definitions:
                 * morning = 05:00 - 11:00
                 * lunch = 11:00 - 15:00
                 * evening = 17:00 - 23:59

            4. **Place Name Normalization (CRITICAL)**
               - Output place names EXACTLY as they appear in the user's travel plan database
               - The backend uses fuzzy matching, so output the CLOSEST possible match
               - DO NOT translate between Korean and English unless user's input clearly indicates it
               - Preserve the original language and format as much as possible
               
               Examples of normalization:
                 * "스탈릿성수", "스탈릿 성수", "starlit seongsu" → "스탈릿성수" (DB format, no space)
                 * "명동교자", "명동 교자", "myeongdong kyoja" → "명동교자"
                 * "gangnam", "강 남", "Gangnam Station" → "강남"
                 * "adelabailey", "adela bailey", "아델라 베일리" → "아델라베일리" (DB format, Korean)
                 * "익선동", "익 선동", "ikseon dong" → "익선동"
               
               Rules:
                 * Keep Korean place names in Korean (don't translate to English)
                 * Keep English place names in English (don't translate to Korean)
                 * Remove unnecessary spacing for compound Korean words
                 * Backend fuzzy matching will handle minor differences
                 * When unsure, prefer Korean format for Korean locations

            5. **Place-to-Day Query Detection (CRITICAL - Korean + English)**
               - Detect when user asks WHICH DAY they visit a specific place
               - This is VIEW_PLACE_DAY intent - returns day number/date of when place is visited
               - This is DIFFERENT from view_plan_place (which shows full place details)
               
               KOREAN Examples (all VIEW_PLACE_DAY):
                 * "스탈릿 성수 언제 가?"
                 * "서울숲 몇 일차야?"
                 * "롯데타워 언제 방문해?"
                 * "이태원은 몇 번째야?"
                 * "명동 일정 알려줘" (context dependent)
                 * "성수 카페 언제 가?"
                 * "아델라 베일리는 몇일차에 가?"
                 * "명동교자 언제 방문하는데?"
               
               ENGLISH Examples (all VIEW_PLACE_DAY):
                 * "When do I visit Starlit Seongsu?"
                 * "What day am I going to Starlit Seongsu?"
                 * "Which day is Starlit Seongsu on?"
                 * "When do I go to Seoul Forest?"
                 * "What day is Myeongdong scheduled?"
                 * "Show me the day for Lotte Tower."
                 * "When am I visiting Hongdae?"
                 * "What number stop is Gyeongbokgung?"
                 * "When am I going to Adela Bailey?"
                 * "What day do I visit Gangnam?"
               
               PLACE NAME EXTRACTION RULE:
                 * Output placeName MUST be normalized to match existing travel plan places
                 * Normalize spacing, capitalization, and language
                   - "스탈릿성수" → "스탈릿 성수"
                   - "starlit seongsu" → "스탈릿 성수"
                   - "Seongsu Starlit" → "스탈릿 성수"
                 * If user uses partial or misspelled names, infer closest matching place
                 * NEVER output raw user text - always normalize
               
               OUTPUT FORMAT:
                 {
                   "intent": "view_place_day",
                   "arguments": { "placeName": "<normalized place name>", "lang": "en" }
                 }

            6. **Order/Position Standardization**
               - Convert ordinal expressions to numeric index (1-based)
               - Examples:
                 * "첫번째", "first", "1번째" → placeIndex: 1
                 * "두번째", "second", "2번째" → placeIndex: 2
                 * "마지막", "last" → placeIndex: -1

            ---

            # ✔ Intent-specific Arguments Examples

            ## View Intents (조회)
            - view_plan → { "lang": "ko" }
              * Examples: "일정 보여줘", "show my plan", "일정", "plan", "what is my plan"

            - view_plan_day → { "dayIndex": 1, "lang": "ko" } OR { "date": "2025-12-06", "lang": "en" }
              * Examples: "1일차 일정", "첫날 일정", "day 1 schedule", "what do we have on first day", "12월 6일 일정", "December 6th schedule"

            - view_plan_place → { "placeName": "강남", "lang": "ko" } OR { "dayIndex": 1, "placeIndex": 1, "lang": "en" }
              * Examples: "강남 일정", "첫날 첫번째 일정", "Gangnam schedule", "first place on day 1", "명동교자 먹는 일정"

            - view_place_day → { "placeName": "Adela Bailey", "lang": "en" }
              * Examples: "When am I going to Adela Bailey?", "What day do I visit Gangnam?", "아델라 베일리는 몇일차에 가?", "명동교자 언제 방문해?"
              * CRITICAL: This asks WHEN (which day), not WHAT (place details)

            - view_current_activity → { "timeQuery": "current", "lang": "ko" }
              * Examples: "지금 뭐해야해", "what should I do now", "현재 일정", "지금 어디 가야해"

            - view_next_activity → { "timeQuery": "next", "lang": "en" }
              * Examples: "다음 일정", "what's next", "next activity"

            - view_plan_summary → { "lang": "ko" }
              * Examples: "여행 요약", "trip summary", "전체 일정 요약"

            - view_plan_time_range → { "range": "morning", "lang": "ko" }
              * Examples: "아침 일정", "morning schedule", "show me morning activities", "점심 뭐 먹어?", "what's for lunch", "저녁 일정 알려줘", "evening plans", "morning plans", "아침에 뭐해?"
              * range values: "morning", "lunch", "evening"
              * CRITICAL: This is for ALL morning/lunch/evening activities across the trip, not current time

            ## Modification Intents
            - plan_add → { "date": "2025-12-06", "place": "...", "lang": "ko" }
            - plan_modify → { "oldPlace": "...", "newPlace": "...", "day": "...", "lang": "ko" }
            - plan_day_swap → { "dayIndexA": 1, "dayIndexB": 3, "lang": "ko" }
            - plan_place_recommend → { "location": "홍대", "lang": "ko" } OR { "keyword": "케이팝", "lang": "en" }
            - currency_exchange → { "from": "USD", "to": "KRW", "lang": "ko" }

            ---

            # ✔ Confidence Rules
            - 0.0 ~ 1.0 range
            - 0.8+: Very confident
            - 0.5 ~ 0.8: Moderately confident
            - < 0.5: Uncertain → likely "etc"

            ---

            # ✔ Output Format (MUST FOLLOW)

            Output ONLY JSON. No explanations or comments.

            {
              "intents": [
                {
                  "intent": "view_plan_day",
                  "confidence": 0.92,
                  "arguments": {
                    "dayIndex": 1,
                    "lang": "ko"
                  }
                }
              ]
            }

            - intent: One of IntentType values
            - confidence: 0.0~1.0
            - arguments: Only necessary information with standardized values

            ---

            # ✔ Output
            Output ONLY JSON.

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
