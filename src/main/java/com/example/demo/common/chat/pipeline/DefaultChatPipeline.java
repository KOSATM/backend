package com.example.demo.common.chat.pipeline;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.common.chat.intent.agent.IntentAnalysisAgent;
import com.example.demo.common.chat.intent.agent.ResponseAgent;
import com.example.demo.common.chat.intent.dto.IntentCommand;
import com.example.demo.common.chat.intent.dto.request.IntentRequest;
import com.example.demo.common.chat.intent.dto.response.IntentResponse;
import com.example.demo.common.chat.intent.service.IntentProcessor;
import com.example.demo.planner.plan.agent.test.TravelPlanAgent;
import com.example.demo.planner.plan.service.create.TravelPlannerService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultChatPipeline implements ChatPipeline {

    private final IntentAnalysisAgent intentAnalysisAgent;
    private final IntentProcessor intentProcessor;
    private final AiAgentRouter agentRouter;
    private final ResponseAgent responseAgent;

    @Override
    public PipelineResult execute(IntentRequest request, Long userId) {

        // 1) 의도 분석 (LLM)
        log.info("▶ 1. 의도 분석");
        IntentResponse intentResponse = intentAnalysisAgent.analyze(request);

        // 2) IntentItem → IntentCommand 변환
        log.info("▶ 2. IntentItem → IntentCommand 변환");
        List<IntentCommand> commands = intentResponse.getIntents().stream()
                .map(item -> intentProcessor.toCommand(item, request.getMessage()))
                .toList();
        log.info(commands.toString());


        // 3) 메인 Intent
        log.info("▶ 3. 가장 우선되는 Main Intent 선택");
        IntentCommand main = commands.get(0);

        // 4) 추가 Intent
        log.info("▶ 4. 추가 Intent 처리");
        List<IntentCommand> additional = commands.size() > 1
                ? commands.subList(1, commands.size())
                : List.of();

        // 5) 메인 Intent 실행
        log.info("▶ 5. 가장 우선되는 Main Intent 기능 실행");
        AiAgentResponse mainIntentResponse = agentRouter.route(main, userId);

        // 응답 에이전트 사용
        String message = responseAgent.generateMessage(request.getMessage(), mainIntentResponse.getData());
        AiAgentResponse processedResponse = AiAgentResponse.builder()
                .message(message)
                .targetUrl(mainIntentResponse.getTargetUrl())
                .data(mainIntentResponse.getData())
                .build();

        // 6) 메인 응답 + 추가 Intent 묶어서 반환
        log.info("▶ 6. Main 응답 + 추가 Intent 묶어서 반환");
        return PipelineResult.builder()
                .mainResponse(processedResponse)
                .additionalIntents(additional)
                .build();
    }
}
