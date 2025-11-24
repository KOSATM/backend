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
        
        ## 중요한 지시사항:
        1. 사용자의 질문에서 "일" 또는 "박" 단위의 기간 정보를 반드시 추출하세요
        2. 기간이 명시되지 않으면 "일" 단위로 명확히 해석하세요
        3. 추가 설명이나 주석은 포함하지 마세요 - JSON만 반환하세요
        4. 모든 날짜는 "YYYY-MM-DD" 형식을 사용하고, 시간은 "HH:MM:SS" 형식을 사용하세요
        5. startDate에서 endDate까지의 날 수가 사용자의 요청 기간과 정확히 일치해야 합니다
        6. 각 day마다 2-3개의 구체적인 일정(schedules)을 포함하세요
        7. 위도와 경도는 실제 위치의 좌표를 사용하세요 (예: 서울 시청 37.5665, 126.9780)
        
        ## 기간 해석 규칙:
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
        
        입력: "서울 1박"
        기간 해석: 1박 → 2일, startDate=2025-11-24, endDate=2025-11-25
        응답:
        {
          "userId": 1,
          "budget": 5000000.00,
          "startDate": "2025-11-24",
          "endDate": "2025-11-25",
          "days": [
            {
              "date": "2025-11-24",
              "title": "인천 도착 및 강남 탐방",
              "schedules": [
                {
                  "title": "인천 국제공항 도착",
                  "startAt": "2025-11-24 14:00:00",
                  "endAt": "2025-11-24 15:30:00",
                  "placeName": "인천 국제공항",
                  "address": "인천광역시 중구 공항로 272",
                  "lat": 37.4602,
                  "lng": 126.4407,
                  "expectedCost": 0
                },
                {
                  "title": "호텔 체크인 및 강남 산책",
                  "startAt": "2025-11-24 16:00:00",
                  "endAt": "2025-11-24 18:00:00",
                  "placeName": "강남역 근처",
                  "address": "서울 강남구 강남대로",
                  "lat": 37.4979,
                  "lng": 127.0276,
                  "expectedCost": 0
                },
                {
                  "title": "강남 레스토랑에서 저녁 식사",
                  "startAt": "2025-11-24 18:30:00",
                  "endAt": "2025-11-24 20:00:00",
                  "placeName": "강남역 맛집거리",
                  "address": "서울 강남구 테헤란로",
                  "lat": 37.4979,
                  "lng": 127.0276,
                  "expectedCost": 50000
                }
              ]
            },
            {
              "date": "2025-11-25",
              "title": "남산 타워 & 명동 쇼핑 & 출발",
              "schedules": [
                {
                  "title": "호텔 조식",
                  "startAt": "2025-11-25 07:00:00",
                  "endAt": "2025-11-25 08:30:00",
                  "placeName": "호텔 조식당",
                  "address": "서울 강남구",
                  "lat": 37.4979,
                  "lng": 127.0276,
                  "expectedCost": 0
                },
                {
                  "title": "남산 타워 방문",
                  "startAt": "2025-11-25 09:00:00",
                  "endAt": "2025-11-25 11:00:00",
                  "placeName": "남산타워",
                  "address": "서울 중구 남산공원길 105",
                  "lat": 37.5512,
                  "lng": 126.9882,
                  "expectedCost": 15000
                },
                {
                  "title": "명동 쇼핑 및 점심식사",
                  "startAt": "2025-11-25 11:30:00",
                  "endAt": "2025-11-25 13:30:00",
                  "placeName": "명동쇼핑거리",
                  "address": "서울 중구 명동길",
                  "lat": 37.5635,
                  "lng": 126.9828,
                  "expectedCost": 80000
                },
                {
                  "title": "인천 국제공항 출발",
                  "startAt": "2025-11-25 15:00:00",
                  "endAt": "2025-11-25 16:30:00",
                  "placeName": "인천 국제공항",
                  "address": "인천광역시 중구 공항로 272",
                  "lat": 37.4602,
                  "lng": 126.4407,
                  "expectedCost": 0
                }
              ]
            }
          ]
        }
        
        지금부터 사용자의 요청에 위의 규칙을 적용하여 JSON만 반환하세요.
        각 일정에는 반드시 실제 위치의 위도/경도와 예상 비용을 포함하세요.
          """)
      .user(question)
      .call()
      .content();
    return response;
  }
}
