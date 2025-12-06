package com.example.demo.common.sse.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.demo.common.sse.dto.SseSendRequest;
import com.example.demo.common.sse.service.SseService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;



@RestController
@RequiredArgsConstructor
@Slf4j
public class SseController {
  private final SseService sseService;

  @GetMapping("/subscribe/{userId}")
  public SseEmitter subscribe(@PathVariable String userId) {
    return sseService.subscribe(userId);
  }

  @PostMapping("/send/{userId}")
  public void postMethodName(@PathVariable String userId, @RequestBody SseSendRequest sseSendRequest) {
    sseService.sendToClient(userId, sseSendRequest.getEventName(), sseSendRequest.getData());
  }
  
  
}
