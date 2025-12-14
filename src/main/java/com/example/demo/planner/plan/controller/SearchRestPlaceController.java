package com.example.demo.planner.plan.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.common.chat.intent.dto.IntentCommand;
import com.example.demo.planner.plan.agent.PlaceSuggestAgentNoChat;
import com.example.demo.planner.plan.dto.entity.PlanPlace;
import com.example.demo.planner.plan.dto.entity.TravelPlaces;
import com.example.demo.planner.plan.service.PlanFacade;
import com.example.demo.planner.plan.service.RestPlaceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@RestController
@RequiredArgsConstructor
@Slf4j
public class SearchRestPlaceController {
  private final PlanFacade planFacade;

  private final PlaceSuggestAgentNoChat placeSuggestAgentNoChat;

  private final RestPlaceService restPlaceService;

  @GetMapping("/snapshot/search")
  public String snapshotSearch(@RequestBody String snapshot) {
    try {
      planFacade.parseSnapshot(snapshot);
      return "콘솔에 로그 있어요";
    } catch (Exception e) {
      return "에러";
    }
  }

  @PostMapping("/{userId}/suggest-places")
  public Object suggestPlaces(@PathVariable("userId") Long userId, @RequestBody PlanPlace planPlace) {
    Map<String, Object> map = new HashMap<>();
    try {
      IntentCommand command = IntentCommand.builder().arguments(Map.of("Original Place", planPlace)).build();
      List<TravelPlaces> response = placeSuggestAgentNoChat.execute(command, userId);
      map.put("result", "success");
      map.put("data", response);
    } catch (Exception e) {
      map.put("result", "fail");
      map.put("message", e.getMessage());
    }
    return map;
  }

  @GetMapping("/search-rest-place")
  public Object searchRestPlace(@RequestParam("lat") Double lat, @RequestParam("lng") Double lng) {
    return restPlaceService.searchRestPlace(lat, lng);
  }


  // @PostMapping("/test")
  // public String test(@RequestParam("question") String question) {
  // IntentRequest intentRequest =
  // IntentRequest.builder().currentUrl("/planner").userMessage(question).build();

  // return
  // defaultChatPipeline.execute(intentRequest).getMainResponse().getMessage().toString();
  // }
  
}
