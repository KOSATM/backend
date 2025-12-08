package com.example.demo.planner.plan.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.common.chat.intent.dto.request.IntentRequest;
import com.example.demo.common.chat.pipeline.DefaultChatPipeline;
import com.example.demo.planner.plan.agent.PlaceSuggestAgent;
import com.example.demo.planner.plan.service.create.PlanService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@Slf4j
public class TestSuggestController {
  private final PlanService planService;

  private final PlaceSuggestAgent placeSuggestAgent;

  private final DefaultChatPipeline defaultChatPipeline;

  @GetMapping("/snapshot/search")
  public String snapshotSearch(@RequestBody String snapshot) {
    try {
      planService.parseSnapshot(snapshot);
      return "콘솔에 로그 있어요";
    } catch (Exception e) {
      return "에러";
    }
  }

  // @PostMapping("/suggest-spot")
  // public String suggestSpot(@RequestParam("question") String question) {
  // try {
  // IntentCommand intentCommand
  // String response = placeSuggestAgent.execute(question);
  // return response;
  // } catch (Exception e) {
  // return "에러";
  // }
  // }

  // @PostMapping("/test")
  // public String test(@RequestParam("question") String question) {
  //   IntentRequest intentRequest = IntentRequest.builder().currentUrl("/planner").userMessage(question).build();

  //   return defaultChatPipeline.execute(intentRequest).getMainResponse().getMessage().toString();
  // }

}
