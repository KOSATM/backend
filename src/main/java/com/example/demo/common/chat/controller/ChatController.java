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

    /**
     * 🧪 SmartPlanAgent 테스트 엔드포인트
     * GET /api/chat/test/smart-plan?msg={message}&userId={userId}
     */
    @GetMapping("/api/chat/test/smart-plan")
    public ResponseEntity<String> testSmartPlan(
            @RequestParam String msg,
            @RequestParam(defaultValue = "1") Long userId) {

        log.info("🧪 === SmartPlanAgent 테스트 시작 ===");
        log.info("메시지: {}", msg);
        log.info("사용자: {}", userId);

        IntentRequest intentRequest = IntentRequest.builder()
                .message(msg)
                .currentUrl("/planner")
                .userId(userId)
                .build();

        PipelineResult result = defaultChatPipeline.execute(intentRequest, userId);

        String response = result.getMainResponse().getMessage();

        log.info("🧪 === 응답: {} ===", response);

        return ResponseEntity.ok(response);
    }

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
public ResponseEntity<PipelineResult> analyzeChat(@RequestBody IntentRequest intentRequest) {
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

        IntentRequest intentRequest = IntentRequest.builder()
                .message(request.getMessage())
                .currentUrl("/planner")
                .userId(userId)
                .build();

        PipelineResult result = defaultChatPipeline.execute(intentRequest, userId);

        String response = result.getMainResponse().getMessage(); // 핵심 수정 부분

        return ResponseEntity.ok(
                TravelChatSendResponse.success(response, null)
        );

    } catch (Exception e) {
        log.error("Error processing chat request", e);
        return ResponseEntity.ok(TravelChatSendResponse.error(e.getMessage()));
    }
}




}
