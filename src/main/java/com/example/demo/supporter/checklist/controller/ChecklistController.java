package com.example.demo.supporter.checklist.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.supporter.checklist.agent.ChecklistAgent;
import com.example.demo.supporter.checklist.dto.entity.Checklist;
import com.example.demo.supporter.checklist.dto.response.TravelDayResponse;
import com.example.demo.supporter.checklist.dto.response.ChecklistItemResponse;
import com.example.demo.supporter.checklist.service.ChecklistService;
import com.example.demo.supporter.checklist.service.TravelDayService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/supporter/checklists")
@RequiredArgsConstructor
public class ChecklistController {

    private final ChecklistService service;
    private final TravelDayService travelDayService;
    private final ChecklistAgent checklistAgent;

    @GetMapping("/{id}")
    public ResponseEntity<Checklist> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Checklist>> list(@PathVariable Long userId) {
        return ResponseEntity.ok(service.getAll(userId));
    }

    @PostMapping
    public ResponseEntity<Long> create(@RequestBody Checklist c) {
        return ResponseEntity.ok(service.create(c));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Integer> update(@PathVariable Long id, @RequestBody Checklist c) {
        c.setId(id);
        return ResponseEntity.ok(service.update(c));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Integer> delete(@PathVariable Long id) {
        return ResponseEntity.ok(service.delete(id));
    }

    @GetMapping("/day/{planId}/{dayIndex}")
    public ResponseEntity<TravelDayResponse> getTravelDay(
            @PathVariable("planId") Long planId,
            @PathVariable("dayIndex") Integer dayIndex) {
        return ResponseEntity.ok(travelDayService.getTravelDay(planId, dayIndex));
    }

    @GetMapping("/checklist/{planId}/{dayIndex}")
    public ResponseEntity<ChecklistItemResponse> generateChecklist(
            @PathVariable("planId") Long planId,
            @PathVariable("dayIndex") Integer dayIndex,
            @RequestParam(value = "userId", required = false) Long userId) {
        return ResponseEntity.ok(checklistAgent.generateChecklist(planId, dayIndex, userId));
    }
}
