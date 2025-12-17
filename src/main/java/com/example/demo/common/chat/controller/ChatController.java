package com.example.demo.common.chat.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
import com.example.demo.common.chat.pipeline.UnifiedAgentResponse;
import com.example.demo.planner.plan.agent.SmartPlanAgent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;



@RestController
@Slf4j
@RequiredArgsConstructor
public class ChatController {

    private final IntentAnalysisAgent intentAnalysisAgent;
    private final DefaultChatPipeline defaultChatPipeline;
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
            IntentRequest intentRequest = IntentRequest.builder()
                    .message(msg)
                    .currentUrl("/planner")
                    .userId(userId)
                    .build();

            log.info("IntentRequest 생성 완료: {}", intentRequest);

            PipelineResult result = defaultChatPipeline.execute(intentRequest, userId);

            log.info("Pipeline 실행 완료");

            String response = result.getMainResponse().getMessage();

            log.info("🧪 === 응답: {} ===", response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ 테스트 중 오류 발생!", e);
            return ResponseEntity.status(500).body("오류: " + e.getMessage() + "\n스택트레이스를 서버 로그에서 확인하세요.");
        }
    }

    @GetMapping("/api/chat/intent/analyze")
    public String getMethodName() {

        IntentRequest intentRequest = IntentRequest.builder().currentUrl("/planner").message("강남 위주로 여행지 추천해줘").build();
        // IntentRequest intentRequest = IntentRequest.builder().currentUrl("/planner")
                // .userMessage("오늘 날씨 알려주고 일정 수정하고 싶어?").build();

        return intentAnalysisAgent.analyze(intentRequest).toString();
    }

    /**
     * 🧪 PlanContext JSON 조회 엔드포인트 (디버깅용)
     */
    @GetMapping("/api/chat/test/plan-json")
    public ResponseEntity<String> getPlanJson(@RequestParam(defaultValue = "1") Long userId) {
        try {
            var context = smartPlanAgent.loadPlanContext(userId);
            return ResponseEntity.ok(context.toJson());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("오류: " + e.getMessage());
        }
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

        String response = result.getMainResponse().getMessage();
        Object agentData = result.getMainResponse().getData();

        // 이미지 추출
        List<Map<String, Object>> images = extractImages(agentData);

        return ResponseEntity.ok(
                TravelChatSendResponse.success(response, images)
        );

    } catch (Exception e) {
        log.error("Error processing chat request", e);
        return ResponseEntity.ok(TravelChatSendResponse.error(e.getMessage()));
    }
}

    /**
     * Agent 응답 데이터에서 이미지 추출
     */
    private List<Map<String, Object>> extractImages(Object agentData) {
        List<Map<String, Object>> images = new ArrayList<>();

        log.info("🔍 extractImages() 호출 - agentData 타입: {}, 값: {}",
            agentData == null ? "null" : agentData.getClass().getSimpleName(),
            agentData);

        if (agentData == null) {
            return images;
        }

        // UnifiedAgentResponse인 경우 내부 data 추출
        if (agentData instanceof UnifiedAgentResponse) {
            log.info("✅ UnifiedAgentResponse 감지");
            UnifiedAgentResponse unified = (UnifiedAgentResponse) agentData;
            Object innerData = unified.getData();

            log.info("📦 innerData 타입: {}", innerData == null ? "null" : innerData.getClass().getSimpleName());

            if (innerData instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> dataList = (List<Map<String, Object>>) innerData;
                log.info("📋 List 크기: {}", dataList.size());

                for (Map<String, Object> place : dataList) {
                    String title = (String) place.get("title");
                    String image = (String) place.get("image");
                    String address = (String) place.get("address");

                    log.info("   → 장소: {}, 이미지: {}", title, image);

                    if (image != null && !image.isEmpty()) {
                        Map<String, Object> imageData = new java.util.LinkedHashMap<>();
                        imageData.put("title", title);
                        imageData.put("placeName", place.get("placeName"));
                        imageData.put("address", address);
                        imageData.put("image", image);
                        images.add(imageData);
                    }
                }
            }
        }
        // 혹시 List 형태로 직접 오는 경우도 처리
        else if (agentData instanceof List) {
            log.info("✅ List 직접 감지");
            try {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> dataList = (List<Map<String, Object>>) agentData;
                log.info("📋 List 크기: {}", dataList.size());

                for (Map<String, Object> place : dataList) {
                    String title = (String) place.get("title");
                    String image = (String) place.get("image");
                    String address = (String) place.get("address");

                    if (image != null && !image.isEmpty()) {
                        Map<String, Object> imageData = new java.util.LinkedHashMap<>();
                        imageData.put("title", title);
                        imageData.put("placeName", place.get("placeName"));
                        imageData.put("address", address);
                        imageData.put("image", image);
                        images.add(imageData);
                    }
                }
            } catch (Exception e) {
                log.warn("❌ 이미지 추출 중 오류 (List 처리): {}", e.getMessage());
            }
        } else {
            log.warn("⚠️ 예상치 못한 데이터 타입: {}", agentData.getClass().getName());
        }

        log.info("✅ 추출된 이미지 개수: {}", images.size());
        return images;
    }




}
