package com.example.demo.common.sse.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SseService {
  private final Map<String, SseEmitter> sseEmitterMap = new ConcurrentHashMap<>();
  
  /**
   * 구독
   */
  public SseEmitter subscribe(String userId) {
    long timeout = 1000L * 3600;

    SseEmitter sseEmitter = new SseEmitter(timeout);
    sseEmitterMap.put(userId, sseEmitter);

    sseEmitter.onCompletion(() -> sseEmitterMap.remove(userId));
    sseEmitter.onTimeout(() -> sseEmitterMap.remove(userId));
    sseEmitter.onError((throwable) -> sseEmitter.complete());

    sendToClient(userId, "connect", "sse connect...");
    
    return sseEmitter;
  }

  public void sendToClient(String userId, String eventName, Object data) {
    SseEmitter sseEmitter = sseEmitterMap.get(userId);
    try {
      sseEmitter.send(
        SseEmitter.event().id(userId).name(eventName).data(data)
      );
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
