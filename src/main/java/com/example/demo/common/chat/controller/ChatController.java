package com.example.demo.common.chat.controller;

import java.util.ArrayList;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.common.chat.dto.TravelChatSendRequest;
import com.example.demo.common.chat.dto.TravelChatSendResponse;
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

    /**
     * 간단 채팅 엔드포인트 (legacy / 디버그용)
     */
    @PostMapping("/chat")
    public ResponseEntity<String> analyzeChat(@RequestBody Map<String, Object> request) {
        try {
            String message = (String) request.get("message");
            Long userId = request.get("userId") != null
                    ? ((Number) request.get("userId")).longValue()
                    : 1L;

            var response = smartPlanAgent.execute(message, userId);
            return ResponseEntity.ok(response.getMessage());
        } catch (Exception e) {
            log.error("Error in /chat endpoint", e);
            return ResponseEntity.status(500).body("오류: " + e.getMessage());
        }
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
            
            // 사용자 친화적인 에러 메시지 생성
            String userMessage = "죄송합니다. 요청을 처리하는 중 문제가 발생했습니다.";
            if (e.getMessage() != null && !e.getMessage().contains("Unresolved compilation")) {
                userMessage += " (" + e.getMessage() + ")";
            }
            
            return ResponseEntity.ok(
                TravelChatSendResponse.error(userMessage)
            );
        }
    }
}
