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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;



@RestController
@Slf4j
@RequiredArgsConstructor
public class ChatController {

    private final IntentAnalysisAgent intentAnalysisAgent;
    private final DefaultChatPipeline defaultChatPipeline;

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
     * Pipeline 기반 채팅 엔드포인트
     * InputFilteringAgent → IntentAnalysisAgent → AiAgentRouter → Domain Agents
     * /api/chat 경로
     */
    @PostMapping("/api/chat")
    public ResponseEntity<TravelChatSendResponse> chat(@RequestBody TravelChatSendRequest request) {
        try {
            Long userId = request.getUserId();
            String currentUrl = request.getCurrentUrl() != null ? request.getCurrentUrl() : "/planner";

            log.info("Chat request from user {} at {}: {}", userId, currentUrl, request.getMessage());

            // IntentRequest 생성
            IntentRequest intentRequest = IntentRequest.builder()
                .userMessage(request.getMessage())
                .currentUrl(currentUrl)
                .userId(userId)
                .build();

            // 파이프라인 실행: Filtering → Intent Analysis → Agent Routing
            PipelineResult result = defaultChatPipeline.execute(intentRequest);

            // 메인 응답 추출 (AiAgentResponse.message 사용)
            String responseText = result.getMainResponse().getMessage();

            // 추가 Intent가 있으면 알림 추가
            if (result.hasAdditional()) {
                responseText += "\n\n💡 추가로 처리할 작업이 " + result.getAdditionalIntents().size() + "개 있습니다.";
            }

            return ResponseEntity.ok(TravelChatSendResponse.success(responseText, result));

        } catch (Exception e) {
            log.error("Error processing chat request", e);
            return ResponseEntity.ok(TravelChatSendResponse.error("처리 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }


}
