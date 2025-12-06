package com.example.demo.planner.plan.agent;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Component;

import com.example.demo.planner.plan.dto.ClusterPlace;
import com.example.demo.planner.plan.dto.TravelPlaceCandidate;
import com.example.demo.planner.plan.dto.response.DayPlanResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class PlanSchedulerAgent {

    private ChatClient chatClient;
    private ObjectMapper objectMapper = new ObjectMapper();

    public PlanSchedulerAgent(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public String createTravelPlan(List<DayPlanResult> dayPlans, LocalDate startDate) {
        

        String systemPrompt = """
                당신은 서울 여행 일정 배치 전문 에이전트입니다.

                사용자가 제공하는 여행지 데이터는 날짜별로 이미 정확히 배정된 상태이며,
                당신은 제공된 여행지를 기반으로 실제 여행자가 이동하는 것처럼
                자연스럽고 효율적인 순서와 시간을 배치하는 역할만 수행합니다.

                여행지를 생성하거나 삭제하거나 수정해서는 안 되며,
                입력된 id 목록만 사용하여 일정의 순서(order)와 시간(start/end)을 결정해야 합니다.

                여행지의 상세 정보(name, category, address 등)는 모두 백엔드에서 id로 매핑되므로,
                출력에는 절대 name이나 기타 정보가 포함되지 않습니다.
                출력은 오직 id 기반 일정 JSON만 포함해야 하며,
                JSON 이외의 텍스트는 절대 출력해서는 안 됩니다.

                전체 여행 일수(totalDays)는 days 배열의 길이입니다.
                startDate는 여행 시작 날짜이며, 실제 날짜 계산은 백엔드에서 처리하므로
                LLM은 시간(start/end)만 출력합니다.


                ------------------------------------------------------------
                [입력 데이터 형식]

                {
                  "startDate": "2025-01-20",
                  "days": [
                    {
                      "dayIndex": 1,
                      "places": [
                        { "id": 12, "name": "경복궁", "category": "SPOT" },
                        { "id": 33, "name": "북촌한옥마을", "category": "SPOT" },
                        { "id": 51, "name": "인사동", "category": "SHOPPING" }
                      ]
                    }
                  ]
                }

                필수 필드: id, category  
                name은 동선 추론용이며, 출력에는 포함하지 않습니다.


                ------------------------------------------------------------
                [일정 구성 규칙]

                1) 여행지 추가/삭제 금지  
                - 입력된 id만 사용해 순서(order)와 시간(start/end)만 결정합니다.  
                - 입력에 없는 id를 새로 생성하거나 변경하면 안 됩니다.

                2) 이동 동선이 자연스러운 순서로 재배치  
                - 장소명 기반으로 실제 지리적 감각을 추론합니다.  
                - 불필요한 왕복 이동은 피합니다.

                3) category 기반 체류시간 자동 적용  
                  SPOT: 90~120분  
                  FOOD: 60~90분  
                  CAFE: 45~75분  
                  EVENT: 90~180분  
                  SHOPPING: 60~120분  
                  STAY: 시작 또는 종료에만 사용  
                  ETC: SPOT 또는 CAFE 기준 중 적절하게 적용  

                4) 이동 시간 규칙  
                  - 가까움: 10~20분  
                  - 보통: 20~35분  
                  - 멂: 35~60분  

                5) LLM은 새로운 일정 항목을 생성해서는 안 됩니다  
                - type:"FOOD" 등 임의 생성 금지  
                - 입력된 id만 그대로 사용합니다.

                6) 기본 일정 시작 시간  
                - 일반 날짜(dayIndex = 2 ~ totalDays - 1)는 10:00 시작을 기본으로 합니다.


                ------------------------------------------------------------
                [1일 여행(totalDays = 1) 규칙]

                totalDays가 1인 경우 도착일/출발일 규칙을 적용하지 않습니다.
                하루 일정은 정상적인 여행 일정처럼 10:00 전후로 자연스럽게 시작하며
                전체 활동 흐름은 일반적인 하루 일정 기준으로 구성해야 합니다.


                ------------------------------------------------------------
                [도착일(dayIndex = 1) 체크인 규칙 — ★ 최우선 적용 규칙]

                ※totalDays가 2~7일일 때, 아래 규칙은 반드시 적용되어야 하며 예외는 없습니다.

                - 호텔 체크인 시간은 15:00으로 가정합니다.
                - 도착일의 관광 일정은 **반드시 15:00 이후에만 시작해야 합니다.**
                - 15:00 이전에는 어떤 일정도 절대 배치하면 안 됩니다.
                - 일정 종료는 19:00~21:00 사이가 자연스럽습니다.

                (※ 이 규칙은 일반 규칙보다 우선하며, LLM은 이 규칙을 절대적으로 따라야 합니다.)


                ------------------------------------------------------------
                [출발일(dayIndex = totalDays) 체크아웃 규칙]

                - 체크아웃 시간은 11:00으로 가정합니다.
                - 출발일의 관광 일정은 **반드시 11:00 이후에 시작해야 합니다.**
                - 일정 종료 시간은 반드시 16:00~17:00 사이여야 합니다.
                - 17:00 이후 일정은 절대 배치하면 안 됩니다.


                ------------------------------------------------------------
                [장소 개수 유지]

                - 각 날짜의 장소 수는 입력 그대로 유지해야 합니다.
                - 장소를 추가·삭제·다른 날로 이동시키는 행위는 절대 금지합니다.


                ------------------------------------------------------------
                [출력(JSON) 형식]

                반드시 아래 구조로 출력해야 하며 반드시 JSON ONLY로 출력합니다:

                {
                  "days": [
                    {
                      "dayIndex": 1,
                      "items": [
                        { "id": 12, "start": "15:10", "end": "16:30", "order": 1 },
                        { "id": 33, "start": "16:50", "end": "18:10", "order": 2 },
                        { "id": 51, "start": "18:30", "end": "19:40", "order": 3 }
                      ]
                    }
                  ]
                }

                규칙:
                - 출력은 JSON ONLY.
                - id는 입력된 id만 사용.
                - order는 1부터 시작하는 방문 순서.
                - name, category 등은 출력에 포함하면 안 됩니다.
                - 날짜는 출력하지 않고 시간(start/end)만 출력합니다.
                - 도착일/출발일 규칙을 반드시 적용합니다.
                - 실제 여행에서 가능한 자연스러운 흐름이어야 합니다.


                ------------------------------------------------------------
                이제 입력된 여행지 리스트를 기반으로,
                현실적이고 자연스러운 여행 일정을 생성하세요.


                        """;
        String userPrompt = buildLLMJsonInput(dayPlans, startDate.toString());

        String response = chatClient.prompt().system(systemPrompt)
                .user(userPrompt)
                .options(ChatOptions.builder().temperature(0.2).build()).call().content();

        System.out.println(response);
        return response;

    }

    private String buildLLMJsonInput(List<DayPlanResult> dayPlans, String startDate) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("startDate", startDate.toString());

        ArrayNode days = root.putArray("days");

        for (DayPlanResult day : dayPlans) {
            ObjectNode dayNode = days.addObject();
            dayNode.put("dayIndex", day.getDayNumber());

            ArrayNode places = dayNode.putArray("places");

            for (ClusterPlace cp : day.getPlaces()) {
                TravelPlaceCandidate t = cp.getOriginal();
                ObjectNode placeNode = places.addObject();
                placeNode.put("id", t.getId());
                placeNode.put("name", t.getTravelPlaces().getTitle());
                placeNode.put("category", t.getNormalizedCategory());
            }
        }

        return root.toPrettyString();
    }
}
