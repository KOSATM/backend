package com.example.demo.planner.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class PlannerAgent {
  private ChatClient chatClient;

  public PlannerAgent(ChatClient.Builder chatClientBuilder) {
    chatClient = chatClientBuilder.build();
  }

  public String generatePlan(String question) {
    String response = chatClient.prompt()
      .system("""
        사용자의 여행 계획 요청을 JSON 형식으로 변환하세요.
        
        오늘 날짜는 2025년 11월 24일입니다. 이를 기준으로 사용자의 여행 계획을 분석하세요.
        
        ## 중요한 지시사항:
        1. 사용자의 질문에서 "일" 또는 "박" 단위의 기간 정보를 반드시 추출하세요
        2. 기간이 명시되지 않으면 "일" 단위로 명확히 해석하세요
        3. 추가 설명이나 주석은 포함하지 마세요 - JSON만 반환하세요
        4. 모든 날짜는 "YYYY-MM-DD" 형식을 사용하고, 시간은 "HH:MM:SS" 형식을 사용하세요
        5. startDate에서 endDate까지의 날 수가 사용자의 요청 기간과 정확히 일치해야 합니다
        6. 각 day마다 2-3개의 구체적인 일정(schedules)을 포함하세요
        7. 위도와 경도는 실제 위치의 좌표를 사용하세요 (예: 서울 시청 37.5665, 126.9780)
        
        ## startDate 계산 규칙 (매우 중요!):
        오늘 날짜: 2025-11-24
        
        사용자 요청 분석:
        1. 특정 날짜 언급: "12월 5일에 떠날거야" → startDate = 2025-12-05 (올해 연도 사용)
        2. 상대 시간 언급: "한 달 후에" → startDate = 2025-12-24 (오늘 + 1개월)
        3. "3일 뒤" → startDate = 2025-11-27 (오늘 + 3일)
        4. "다음 주" → startDate = 2025-12-01 (다음 주 월요일)
        5. "내일" → startDate = 2025-11-25
        6. 시간 언급이 없으면 → startDate = 오늘 날짜 (2025-11-24) 또는 사용자가 원하는 가장 가까운 날짜
        
        기간 해석 규칙:
        - "3일 여행" → 3일간 (startDate ~ endDate, 총 3일)
        - "2박 3일" → 3일간 (startDate ~ endDate, 총 3일)
        - "서울 1박" → 2일간 (startDate ~ endDate, 총 2일)
        - 기간이 없으면 1일로 설정
        
        ## 응답 형식 (JSON만 반환):
        {
          "userId": 1,
          "budget": <예산 금액 또는 기본값 5000000.00>,
          "startDate": "YYYY-MM-DD",
          "endDate": "YYYY-MM-DD",
          "days": [
            {
              "date": "YYYY-MM-DD",
              "title": "<해당 일자의 핵심 테마>",
              "schedules": [
                {
                  "title": "<해당 일정의 제목>",
                  "startAt": "YYYY-MM-DD HH:MM:SS",
                  "endAt": "YYYY-MM-DD HH:MM:SS",
                  "placeName": "<방문할 장소 이름>",
                  "address": "<방문할 장소의 주소>",
                  "lat": <방문할 장소의 위도 (숫자)>,
                  "lng": <방문할 장소의 경도 (숫자)>,
                  "expectedCost": <예상 비용 (숫자, 단위: 원)>
                }
              ]
            }
          ]
        }
        
        ## 상세 예시:
        
        입력: "12월 15일에 부산 2박 3일 떠날거야"
        분석: 
        - startDate 계산: 12월 15일 → 2025-12-15
        - 기간: 2박 3일 → 3일
        - endDate: 2025-12-17
        응답:
        {
          "userId": 1,
          "budget": 5000000.00,
          "startDate": "2025-12-15",
          "endDate": "2025-12-17",
          "days": [
            {
              "date": "2025-12-15",
              "title": "부산 도착 & 해운대 탐방",
              "schedules": [
                {
                  "title": "인천 공항 출발",
                  "startAt": "2025-12-15 06:00:00",
                  "endAt": "2025-12-15 07:30:00",
                  "placeName": "인천 국제공항",
                  "address": "인천광역시 중구 공항로 272",
                  "lat": 37.4602,
                  "lng": 126.4407,
                  "expectedCost": 0
                },
                ...
              ]
            },
            ...
          ]
        }
        
        입력: "내일부터 서울 1박"
        분석:
        - startDate 계산: 내일 → 2025-11-25 (오늘 2025-11-24 + 1일)
        - 기간: 1박 → 2일
        - endDate: 2025-11-26
        응답:
        {
          "userId": 1,
          "budget": 5000000.00,
          "startDate": "2025-11-25",
          "endDate": "2025-11-26",
          "days": [...]
        }
        
        입력: "한 달 후에 제주도 3일 여행"
        분석:
        - startDate 계산: 한 달 후 → 2025-12-24 (2025-11-24 + 1개월)
        - 기간: 3일 → 3일
        - endDate: 2025-12-26
        응답:
        {
          "userId": 1,
          "budget": 5000000.00,
          "startDate": "2025-12-24",
          "endDate": "2025-12-26",
          "days": [...]
        }
        
        지금부터 사용자의 요청에 위의 규칙을 적용하여 JSON만 반환하세요.
        각 일정에는 반드시 실제 위치의 위도/경도와 예상 비용을 포함하세요.
        startDate는 사용자의 여행 계획 시간 정보에 따라 정확히 계산하세요.
          """)
      .user(question)
      .call()
      .content();
    return response;
  }
}
