package com.example.demo.common.chat.controller;

import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.common.chat.intent.IntentAnalysisAgent;
import com.example.demo.common.chat.intent.dto.request.IntentRequest;
import com.example.demo.common.chat.intent.dto.response.IntentResponse;
import com.example.demo.common.chat.pipeline.DefaultChatPipeline;

import lombok.extern.slf4j.Slf4j;



@RestController
@RequestMapping("/chat")
@Slf4j
public class ChatController {

    @Autowired
    private IntentAnalysisAgent intentAnalysisAgent;

    @Autowired
    private DefaultChatPipeline defaultChatPipeline;

    @GetMapping("/intent/analyze")
    public String getMethodName() {
        
        IntentRequest intentRequest = IntentRequest.builder().currentUrl("/planner").userMessage("강남 위주로 여행지 추천해줘").build();
        // IntentRequest intentRequest = IntentRequest.builder().currentUrl("/planner")
                // .userMessage("오늘 날씨 알려주고 일정 수정하고 싶어?").build();
        
        return intentAnalysisAgent.analyze(intentRequest).toString();
    }

    @GetMapping("/test")
    public String getMethodNam1e() {
        IntentRequest intentRequest = IntentRequest.builder().currentUrl("/planner").userMessage("강남 위주로 여행지 추천해줘").build();
        
        return defaultChatPipeline.execute(intentRequest).toString();
    }
    
    
}
