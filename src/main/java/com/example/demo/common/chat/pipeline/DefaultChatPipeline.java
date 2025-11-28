package com.example.demo.common.chat.pipeline;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.common.chat.intent.IntentAnalysisAgent;
import com.example.demo.common.chat.intent.dto.IntentCommand;
import com.example.demo.common.chat.intent.dto.request.IntentRequest;
import com.example.demo.common.chat.intent.dto.response.IntentResponse;
import com.example.demo.common.chat.intent.service.IntentProcessor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultChatPipeline implements ChatPipeline {

    private final IntentAnalysisAgent intentAnalysisAgent;
    private final IntentProcessor intentProcessor;
    private final AiAgentRouter agentRouter;

    @Override
    public PipelineResult execute(IntentRequest request) {

        // 1) 의도 분석 (LLM)
        log.info("▶ 1. 의도 분석");
        IntentResponse intentResponse = intentAnalysisAgent.analyze(request);

        // 2) IntentItem → IntentCommand 변환
        log.info("▶ 2. IntentItem → IntentCommand 변환");
        List<IntentCommand> commands = intentResponse.getIntents().stream()
                .map(intentProcessor::toCommand)
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
        AiAgentResponse mainIntentResponse = agentRouter.route(main);


        // 6) 메인 응답 + 추가 Intent 묶어서 반환
        log.info("▶ 6. Main 응답 + 추가 Intent 묶어서 반환");
        return PipelineResult.builder()
                .mainResponse(mainIntentResponse)
                .additionalIntents(additional)
                .build();
    }
}
