package com.example.demo.common.chat.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

        IntentRequest intentRequest = IntentRequest.builder().currentUrl("/planner").message("강남 위주로 여행지 추천해줘").build();
        // IntentRequest intentRequest = IntentRequest.builder().currentUrl("/planner")
                // .userMessage("오늘 날씨 알려주고 일정 수정하고 싶어?").build();

        return intentAnalysisAgent.analyze(intentRequest).toString();
    }

    // @GetMapping("/test")
    // public ResponseEntity<PipelineResult> test(@RequestParam("msg") String msg, @RequestParam("userId") Long userId) {
    //     IntentRequest intentRequest = IntentRequest.builder().currentUrl("/planner").userMessage(msg).build();

    //     return ResponseEntity.ok(defaultChatPipeline.execute(intentRequest, userId));
    // }

    @PostMapping("/chat")
    public ResponseEntity<PipelineResult> analyzeChat(@RequestBody IntentRequest
    intentRequest) {
    log.info(intentRequest.toString()+";;;;;;;;");
    return ResponseEntity.ok(defaultChatPipeline.execute(intentRequest, intentRequest.getUserId()));
    }

    /**
     * Plan Agent 기반 채팅 엔드포인트
     * /api/chat 경로
     */
    @PostMapping("/api/chat")
    public ResponseEntity<TravelChatSendResponse> chat(@RequestBody TravelChatSendRequest request) {
        try {
            Long userId = request.getUserId() != null ? request.getUserId() : 1L;

            log.info("Chat request from user {}: {}", userId, request.getMessage());

            // Agent에게 처리 위임 - LLM이 자동으로 적절한 Tool 선택
            String response = planAgent.chat(request.getMessage(), userId);

            return ResponseEntity.ok(TravelChatSendResponse.success(response, null));

        } catch (Exception e) {
            log.error("Error processing chat request", e);
            return ResponseEntity.ok(TravelChatSendResponse.error(e.getMessage()));
        }
    }
}
