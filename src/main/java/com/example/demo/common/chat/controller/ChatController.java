package com.example.demo.common.chat.controller;

import java.util.ArrayList;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.common.chat.dto.TravelChatSendRequest;
import com.example.demo.common.chat.dto.TravelChatSendResponse;
import com.example.demo.common.chat.intent.dto.request.IntentRequest;
import com.example.demo.common.chat.pipeline.AiAgentResponse;
import com.example.demo.planner.plan.agent.SmartPlanAgent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@RequiredArgsConstructor
public class ChatController {

    private final SmartPlanAgent smartPlanAgent;

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

        try {
            var response = smartPlanAgent.execute(msg, userId);
            log.info("🧪 === 응답: {} ===", response.getMessage());
            return ResponseEntity.ok(response.getMessage());
        } catch (Exception e) {
            log.error("❌ 테스트 중 오류 발생!", e);
            return ResponseEntity.status(500).body("오류: " + e.getMessage());
        }
    }

    /**
     * 🧪 PlanContext JSON 조회 엔드포인트 (디버깅용)
     */
    @GetMapping("/api/chat/test/plan-json")
    public ResponseEntity<String> getPlanJson(
            @RequestParam(defaultValue = "1") Long userId) {

        try {
            var context = smartPlanAgent.loadPlanContext(userId);
            return ResponseEntity.ok(context.toJson());
        } catch (Exception e) {
            log.error("❌ PlanContext 조회 실패", e);
            return ResponseEntity.status(500).body("오류: " + e.getMessage());
        }
    }

    
    @PostMapping("/chat")
    public ResponseEntity<TravelChatSendResponse> analyzeChat(@RequestBody IntentRequest intentRequest) {

        AiAgentResponse response = smartPlanAgent.execute(intentRequest.getMessage(), intentRequest.getUserId());
        log.info(response.getMessage()+">>>>>>>");
        
        TravelChatSendResponse result = TravelChatSendResponse.success(response.getMessage(), response.getData());
        return ResponseEntity.ok(result);
    }

    /**
     * ✅ 메인 채팅 엔드포인트
     * POST /api/chat
     */
    @PostMapping("/api/chat")
    public ResponseEntity<TravelChatSendResponse> chat(
            @RequestBody TravelChatSendRequest request) {

        try {
            Long userId = request.getUserId() != null ? request.getUserId() : 1L;
            var response = smartPlanAgent.execute(request.getMessage(), userId);

            return ResponseEntity.ok(
                TravelChatSendResponse.success(
                    response.getMessage(),
                    new ArrayList<>()
                )
            );
        } catch (Exception e) {
            log.error("Error processing chat request", e);
            return ResponseEntity.ok(
                TravelChatSendResponse.error(e.getMessage())
            );
        }
    }
}
