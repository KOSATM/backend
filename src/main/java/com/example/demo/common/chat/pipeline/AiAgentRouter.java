package com.example.demo.common.chat.pipeline;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.example.demo.common.chat.intent.dto.IntentCommand;
import com.example.demo.common.global.agent.AiAgent;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiAgentRouter {

    private final Map<String, AiAgent> aiAgentMap;
    private Map<Class<? extends AiAgent>, AiAgent> agentByClass;

    @PostConstruct
    public void init() {
        agentByClass = aiAgentMap.values().stream()
            .collect(Collectors.toMap(AiAgent::getClass, Function.identity()));
    }

    public AiAgentResponse route(IntentCommand command) {

        Class<? extends AiAgent> clazz = command.getIntent().getAgentClass();
        AiAgent agent = agentByClass.get(clazz);

        log.info(aiAgentMap.toString());

        if (agent == null) {
            return AiAgentResponse.of("지원하지 않는 기능이에요.");
        }

        return agent.execute(command);

        // return switch (command.getCategory()) {
        // // case PLANNER -> planner.execute(command);
        // // case SUPPORTER -> supporter.execute(command);
        // // case TRAVELGRAM -> travelgram.execute(command);
        // default -> AiAgentResponse.of("지원하지 않는 기능이에요.");
        // };
    }
}