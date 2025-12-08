package com.example.demo.common.chat.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.common.chat.dto.TravelChatSendRequest;
import com.example.demo.common.chat.dto.TravelChatSendResponse;
import com.example.demo.common.chat.intent.agent.IntentAnalysisAgent;
import com.example.demo.common.chat.intent.dto.request.IntentRequest;
import com.example.demo.common.chat.pipeline.DefaultChatPipeline;
import com.example.demo.common.chat.pipeline.PipelineResult;
import com.example.demo.planner.plan.agent.PlanAgent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;



@RestController
@Slf4j
@RequiredArgsConstructor
public class ChatController {

    private final IntentAnalysisAgent intentAnalysisAgent;
    private final DefaultChatPipeline defaultChatPipeline;
    private final PlanAgent planAgent;

    @GetMapping("/api/chat/intent/analyze")
    public String getMethodName() {

        IntentRequest intentRequest = IntentRequest.builder().currentUrl("/planner").userMessage("강남 위주로 여행지 추천해줘").build();
        // IntentRequest intentRequest = IntentRequest.builder().currentUrl("/planner")
                // .userMessage("오늘 날씨 알려주고 일정 수정하고 싶어?").build();

        return intentAnalysisAgent.analyze(intentRequest).toString();
    }

    @GetMapping("/test")
    public ResponseEntity<PipelineResult> test(@RequestParam("msg") String msg, @RequestParam("userId") Long userId) {
        IntentRequest intentRequest = IntentRequest.builder().currentUrl("/planner").userMessage(msg).build();

        return ResponseEntity.ok(defaultChatPipeline.execute(intentRequest, userId));
    }

    /**
     * PlanAgent의 Tool 기반 채팅 (생성/수정용)
     */
    @PostMapping("/api/chat/agent")
    public ResponseEntity<TravelChatSendResponse> chatWithAgent(@RequestBody TravelChatSendRequest request) {
        try {
            Long userId = request.getUserId() != null ? request.getUserId() : 1L;
            log.info("Agent chat request from user {}: {}", userId, request.getMessage());

            String response = planAgent.chat(request.getMessage(), userId);
            return ResponseEntity.ok(TravelChatSendResponse.success(response, null));

        } catch (Exception e) {
            log.error("Error in agent chat", e);
            return ResponseEntity.ok(TravelChatSendResponse.error(e.getMessage()));
        }
    }

    /**
     * 의도 분석 파이프라인 기반 채팅 엔드포인트
     * /api/chat 경로
     *
     * 흐름: 사용자 입력 → IntentAnalysisAgent (의도 분석) → AiAgentRouter (에이전트 라우팅) → 적절한 에이전트 실행
     */
    @PostMapping("/api/chat")
    public ResponseEntity<TravelChatSendResponse> chat(@RequestBody TravelChatSendRequest request) {
        try {
            Long userId = request.getUserId() != null ? request.getUserId() : 1L;

            log.info("Chat request from user {}: {}", userId, request.getMessage());

            // 1. IntentRequest 생성
            IntentRequest intentRequest = IntentRequest.builder()
                .currentUrl("/planner")
                .userMessage(request.getMessage())
                .build();

            // 2. DefaultChatPipeline 실행 (의도 분석 → 에이전트 라우팅 → 실행)
            PipelineResult result = defaultChatPipeline.execute(intentRequest, userId);

            // 3. 메인 응답 반환
            String response = result.getMainResponse().getMessage();

            log.info("Pipeline returned response (length={}): {}",
                response != null ? response.length() : 0,
                response != null && response.length() > 100 ? response.substring(0, 100) + "..." : response);

            return ResponseEntity.ok(TravelChatSendResponse.success(response, null));

        } catch (Exception e) {
            log.error("Error processing chat request", e);
            return ResponseEntity.ok(TravelChatSendResponse.error(e.getMessage()));
        }
    }


}
