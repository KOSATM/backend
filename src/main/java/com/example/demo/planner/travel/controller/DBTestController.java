package com.example.demo.planner.travel.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.planner.travel.agent.DBSearchAgent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@Slf4j
public class DBTestController {
  @Autowired
  DBSearchAgent dbSearchAgent;

  @PostMapping("/dbsearch")
  public String postMethodName(@RequestParam("question") String question) {
    String response = dbSearchAgent.ragChat(question);

    return response;
  }

}
