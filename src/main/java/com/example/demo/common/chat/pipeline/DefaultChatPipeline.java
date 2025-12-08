package com.example.demo.common.chat.pipeline;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.common.chat.intent.agent.InputFilteringAgent;
import com.example.demo.common.chat.intent.agent.IntentAnalysisAgent;
import com.example.demo.common.chat.intent.dto.IntentCommand;
import com.example.demo.common.chat.intent.dto.request.FilteredRequest;
import com.example.demo.common.chat.intent.dto.request.IntentRequest;
import com.example.demo.common.chat.intent.dto.response.IntentResponse;
import com.example.demo.common.chat.intent.service.IntentProcessor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultChatPipeline implements ChatPipeline {

    private final InputFilteringAgent inputFilteringAgent;
    private final IntentAnalysisAgent intentAnalysisAgent;
    private final IntentProcessor intentProcessor;
    private final AiAgentRouter agentRouter;

    @Override
    public PipelineResult execute(IntentRequest request, Long userId) {

        // 0) 입력 필터링 및 정규화 (NEW)
        log.info("▶ 0. 입력 필터링 및 정규화");
        FilteredRequest filteredRequest = inputFilteringAgent.filter(request);
        log.info("Filtered: isBlocked={}, normalized={}", 
            filteredRequest.isBlocked(), filteredRequest.getNormalizedText());

        // 블로킹된 요청 처리
        if (filteredRequest.isBlocked()) {
            log.warn("▶ 요청이 블로킹되었습니다: {}", filteredRequest.getBlockReasons());
            return PipelineResult.builder()
                .mainResponse(AiAgentResponse.of(
                    "죄송합니다. 부적절한 내용이 포함되어 요청을 처리할 수 없습니다: " 
                    + String.join(", ", filteredRequest.getBlockReasons())
                ))
                .additionalIntents(List.of())
                .build();
        }

        // 1) 의도 분석 (LLM) - 정규화된 텍스트 사용
        log.info("▶ 1. 의도 분석 (정규화된 텍스트 사용)");
        IntentResponse intentResponse = intentAnalysisAgent.analyze(filteredRequest.toIntentRequest());

        // 2) IntentItem → IntentCommand 변환
        log.info("▶ 2. IntentItem → IntentCommand 변환");
        List<IntentCommand> commands = intentResponse.getIntents().stream()
                .map(intentProcessor::toCommand)
                .toList();
        
        // userId를 모든 IntentCommand의 arguments에 추가
        if (request.getUserId() != null) {
            commands.forEach(cmd -> cmd.getArguments().put("userId", request.getUserId()));
        }
        
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


        // 6) 메인 응답 + 추가 Intent 묶어서 반환
        log.info("▶ 6. Main 응답 + 추가 Intent 묶어서 반환");
        return PipelineResult.builder()
                .mainResponse(mainIntentResponse)
                .additionalIntents(additional)
                .build();
    }
}
