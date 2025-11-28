package com.example.demo.common.chat.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.common.chat.intent.IntentAnalysisAgent;


@RestController
@RequestMapping("/chat")
public class ChatController {

    @Autowired
    private IntentAnalysisAgent intentAnalysisAgent;

    @GetMapping("/intent/analyze")
    public String getMethodName() {
        
        return intentAnalysisAgent.analyze();
    }
}
